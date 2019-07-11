package net.adoptopenjdk.api.v3.models

import org.eclipse.microprofile.openapi.annotations.enums.SchemaType
import org.eclipse.microprofile.openapi.annotations.media.Schema

@Schema(type = SchemaType.STRING, enumeration = ["x64", "x32", "ppc64", "ppc64le", "s390x", "aarch64", "arm", "sparcv9"], example = "x64")
enum class Architecture : FileNameMatcher {
    x64(),
    x32("x86-32"),
    ppc64,
    ppc64le,
    s390x,
    aarch64,
    arm("arm32"),
    sparcv9;

    override lateinit var names: List<String>

    constructor(vararg alternativeNames: String) {
        setNames(this.name, alternativeNames.toList())
    }

}