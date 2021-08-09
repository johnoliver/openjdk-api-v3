#!/bin/bash

# Temporary hack until patch is release on main quarkus project

curl -sLo 2.1.1.Final.zip "https://github.com/quarkusio/quarkus/archive/refs/tags/2.1.1.Final.zip"
unzip -q 2.1.1.Final.zip
cd quarkus-2.1.1.Final/
patch -p 1 < /tmp/patch
cd independent-projects/resteasy-reactive/server/vertx
../../../../mvnw clean install
