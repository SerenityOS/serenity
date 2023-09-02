#!/usr/bin/env -S USE_CCACHE=false bash ../.port_include.sh

port='OpenJDK'
version='17.0.6'
workdir="jdk17u-dev-jdk-${version}-ga"
useconfigure='true'
use_fresh_config_guess='true'
config_guess_paths=("make/autoconf/build-aux/autoconf-config.guess")
use_fresh_config_sub='true'
config_sub_paths=("make/autoconf/build-aux/autoconf-config.sub")
files=(
    "https://github.com/openjdk/jdk17u-dev/archive/refs/tags/jdk-${version}-ga.tar.gz#4bd3d2534d7b584c01711e64b9e5b7e79052a1759d3fded8d64107ebc9d37dc2"
)
depends=("fontconfig" "libffi")

configure() {
    TOOLCHAIN_ARGS=()
    WARNING_IGNORE_FLAGS='-Wno-error=switch -Wno-maybe-uninitialized'
    if [ $SERENITY_TOOLCHAIN = 'Clang' ]; then
        # We need the build CC and CXX to actually be clang when using clang to cross-compile
        #    ... for some reason.
        TOOLCHAIN_ARGS=("--with-toolchain-type=clang"
                        "BUILD_CC=clang"
                        "BUILD_CXX=clang++")
        WARNING_IGNORE_FLAGS="${WARNING_IGNORE_FLAGS} -Wno-error=bitwise-instead-of-logical"
    fi

    # Note: To use ccache with OpenJDK, pass --enable-ccache
    #     It rejects the ccache symlinks.

    run bash configure \
        AR=${AR} \
        READELF=${READELF} \
        STRIP=${STRIP} \
        CXXFILT=${CXXFILT} \
        BUILD_AR=${HOST_AR} \
        BUILD_OBJCOPY=${HOST_OBJCOPY} \
        BUILD_STRIP=${HOST_STRIP} \
        --openjdk-target=${SERENITY_ARCH}-pc-serenity \
        --with-sysroot=${SERENITY_INSTALL_ROOT} \
        --with-jvm-variants=zero \
        --enable-headless-only \
        --with-debug-level=fastdebug \
        --with-native-debug-symbols=internal \
        --with-tools-dir=${SERENITY_TOOLCHAIN_BINDIR} \
        --with-extra-cflags="${WARNING_IGNORE_FLAGS}" \
        --with-extra-cxxflags="${WARNING_IGNORE_FLAGS}" \
        "${TOOLCHAIN_ARGS[@]}"
}

build() {
    run make java.base jdk.compiler java.logging
}

install() {
    run mkdir -p ${SERENITY_INSTALL_ROOT}/usr/local/lib/jvm/
    run sh -c "cp ./build/serenity-* ${SERENITY_INSTALL_ROOT}/usr/local/lib/jvm/ -rf"
}
