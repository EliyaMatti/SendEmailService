FROM ubuntu:24.04

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        openjdk-21-jdk-headless \
        maven \
        git \
        curl \
    && rm -rf /var/lib/apt/lists/*

USER ubuntu
