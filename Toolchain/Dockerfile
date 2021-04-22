FROM ubuntu:21.04

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update -y \
    && apt-get install -y \
        build-essential \
        cmake \
        curl \
        genext2fs \
        git \
        libmpfr-dev \
        libmpc-dev \
        libgmp-dev \
        e2fsprogs \
        ninja-build \
        qemu-utils \
        sudo \
        tzdata \
        wget

WORKDIR /serenity

RUN /bin/bash
