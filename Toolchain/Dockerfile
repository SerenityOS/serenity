FROM ubuntu:22.10

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update -y \
    && apt-get install -y \
        build-essential \
        ccache \
        cmake \
        curl \
        g++-12 \
        gcc-12 \
        e2fsprogs \
        genext2fs \
        git \
        imagemagick \
        libgmp-dev \
        libgtk-3-dev \
        libmpc-dev \
        libmpfr-dev \
        libpixman-1-dev \
        libsdl2-dev \
        libspice-server-dev \
        ninja-build \
        qemu-utils \
        rsync \
        sudo \
        texinfo \
        tzdata \
        unzip \
    && rm -rf /var/lib/apt/lists/ \
    && update-alternatives --install /usr/bin/gcc gcc /usr/bin/gcc-12 900 --slave /usr/bin/g++ g++ /usr/bin/g++-12
