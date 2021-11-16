FROM ubuntu:21.10

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update -y \
    && apt-get install -y \
        build-essential \
        ccache \
        cmake \
        curl \
        g++-10 \
        gcc-10 \
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
        wget \
    && rm -rf /var/lib/apt/lists/ \
    && update-alternatives --install /usr/bin/gcc gcc /usr/bin/gcc-10 900 --slave /usr/bin/g++ g++ /usr/bin/g++-10

RUN git clone --depth 1 https://github.com/SerenityOS/serenity.git /serenity/serenity-git
RUN cd /serenity/serenity-git/Toolchain; \
        ./BuildIt.sh

WORKDIR /serenity
VOLUME /serenity/out
