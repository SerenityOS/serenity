FROM ubuntu:21.04

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update -y \
    && apt-get install -y \
        build-essential \
        ccache \
        cmake \
        curl \
        genext2fs \
        gettext \
        git \
        imagemagick \
        libmpfr-dev \
        libmpc-dev \
        libgmp-dev \
        e2fsprogs \
        ninja-build \
        qemu-utils \
        rsync \
        sudo \
        tzdata \
        unzip \
        wget

WORKDIR /serenity

RUN /bin/bash
