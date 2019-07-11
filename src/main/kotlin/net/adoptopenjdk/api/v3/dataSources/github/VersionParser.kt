package net.adoptopenjdk.api.v3.dataSources.github

import net.adoptopenjdk.api.v3.models.VersionData
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

//This is a port of the Groovy VersionParser in the openjdk-build project
//Should probably look at exporting it as a common lib rather than having 2 implementations
class VersionParser {

    private var major: Int? = null
    private var minor: Int? = null
    private var security: Int? = null
    private var build: Int? = null
    private var opt: String? = null
    private var version: String? = null
    private var pre: String? = null
    private var adopt_build_number: Int? = 1
    private var semver: String? = null

    fun parse(PUBLISH_NAME: String?, ADOPT_BUILD_NUMBER: String? = null): VersionData {
        if (PUBLISH_NAME != null) {
            if (!matchPre223(PUBLISH_NAME)) {
                match223(PUBLISH_NAME)
            }
        }

        if (ADOPT_BUILD_NUMBER != null) {
            adopt_build_number = Integer.parseInt(ADOPT_BUILD_NUMBER)
        }

        semver = formSemver()

        return VersionData(major!!, minor!!, security!!, pre, adopt_build_number!!, semver!!, build!!, opt, version!!)
    }

    private fun or0(matched: Matcher, groupName: String): Int {
        if (!matched.pattern().pattern().contains(groupName)) {
            return 0
        }
        val number = matched.group(groupName)
        return if (number != null) {
            try {
                Integer.parseInt(number)
            } catch (e: NumberFormatException) {
                println("failed to match ${number}")
                return 0
            }
        } else {
            0
        }

    }

    private fun matchAltPre223(versionString: String): Boolean {
        //1.8.0_202-internal-201903130451-b08
        val pre223regex = """(?<version>1\.(?<major>[0-8])\.0(_(?<update>[0-9]+))?(-(?<additional>.*))?)"""
        val matched = Pattern.compile(".*?$pre223regex.*?").matcher(versionString)

        if (matched.matches()) {
            major = or0(matched, "major")
            minor = 0
            security = or0(matched, "update")
            if (matched.group("additional") != null) {
                val additional = matched.group("additional")

                for (`val` in additional.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {

                    var matcher = Pattern.compile("""b(?<build>[0-9]+)""").matcher(`val`)
                    if (matcher.matches()) build = Integer.parseInt(matcher.group("build"))

                    matcher = Pattern.compile("""^(?<opt>[0-9]{12})$""").matcher(`val`)
                    if (matcher.matches()) opt = matcher.group("opt")
                }
            }

            version = matched.group("version")
            return true
        }


        return false
    }

    private fun matchPre223(versionString: String): Boolean {
        val pre223regex = "jdk\\-?(?<version>(?<major>[0-8]+)(u(?<update>[0-9]+))?(-b(?<build>[0-9]+))(_(?<opt>[-a-zA-Z0-9\\.]+))?)"
        val matched = Pattern.compile(".*?$pre223regex.*?").matcher(versionString)

        if (matched.matches()) {
            major = or0(matched, "major")
            minor = 0
            security = or0(matched, "update")
            build = or0(matched, "build")
            if (matched.group("opt") != null) opt = matched.group("opt")
            version = matched.group("version")
            return true
        } else {
            return matchAltPre223(versionString)
        }

    }

    private fun match223(versionString: String): Boolean {
        //Regexes based on those in http://openjdk.java.net/jeps/223
        // Technically the standard supports an arbitrary number of numbers, we will support 3 for now
        val vnumRegex = """(?<major>[0-9]+)(\.(?<minor>[0-9]+))?(\.(?<security>[0-9]+))?"""
        val preRegex = "(?<pre>[a-zA-Z0-9]+)"
        val buildRegex = "(?<build>[0-9]+)"
        val optRegex = "(?<opt>[-a-zA-Z0-9\\.]+)"

        val version223Regexs = Arrays.asList(
                "(?:jdk\\-)?(?<version>$vnumRegex(\\-$preRegex)?\\+$buildRegex(\\-$optRegex)?)",
                "(?:jdk\\-)?(?<version>$vnumRegex\\-$preRegex(\\-$optRegex)?)",
                "(?:jdk\\-)?(?<version>$vnumRegex(\\+\\-$optRegex)?)")

        for (regex in version223Regexs) {
            val matched223 = Pattern.compile(".*?$regex.*?").matcher(versionString)
            if (matched223.matches()) {
                major = or0(matched223, "major")
                minor = or0(matched223, "minor")
                security = or0(matched223, "security")
                if (regex.contains("pre") && matched223.group("pre") != null) pre = matched223.group("pre")
                build = or0(matched223, "build")
                if (matched223.group("opt") != null) opt = matched223.group("opt")
                version = matched223.group("version")
                return true
            }
        }

        return false
    }

    fun formSemver(): String? {
        if (major != null) {
            var semver = major.toString() + "." + minor + "." + security

            if (pre != null) {
                semver += "-" + pre!!
            }


            semver += "+"
            val i = build
            semver += if (i == null) i else "0"
            semver += "." + adopt_build_number!!

            if (opt != null) {
                semver += "." + opt!!
            }

            return semver
        } else {
            return null
        }

    }

}
