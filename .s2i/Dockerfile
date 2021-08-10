FROM registry.access.redhat.com/ubi8/openjdk-11

USER root
RUN     microdnf update && \
        microdnf install git

USER 185

RUN     /tmp/src/.s2i/bin/assemble
