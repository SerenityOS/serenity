#!/usr/bin/env bash

export SERENITY_SOURCE_DIR="$(realpath "${SCRIPT}/../")"

if [ "$SERENITY_TOOLCHAIN" = "Clang" ]; then
    export SERENITY_BUILD_DIR="${SERENITY_SOURCE_DIR}/Build/${SERENITY_ARCH}clang"
    export CC="clang --target=${SERENITY_ARCH}-pc-serenity --sysroot=${SERENITY_BUILD_DIR}/Root"
    export CXX="clang++ --target=${SERENITY_ARCH}-pc-serenity --sysroot=${SERENITY_BUILD_DIR}/Root"
    export AR="llvm-ar"
    export RANLIB="llvm-ranlib"
    export READELF="llvm-readelf"
    export PATH="${SERENITY_SOURCE_DIR}/Toolchain/Local/clang/bin:${HOST_PATH}"
else
    export SERENITY_BUILD_DIR="${SERENITY_SOURCE_DIR}/Build/${SERENITY_ARCH}"
    export CC="${SERENITY_ARCH}-pc-serenity-gcc"
    export CXX="${SERENITY_ARCH}-pc-serenity-g++"
    export AR="${SERENITY_ARCH}-pc-serenity-ar"
    export RANLIB="${SERENITY_ARCH}-pc-serenity-ranlib"
    export READELF="${SERENITY_ARCH}-pc-serenity-readelf"
    export PATH="${SERENITY_SOURCE_DIR}/Toolchain/Local/${SERENITY_ARCH}/bin:${HOST_PATH}"
fi

export PKG_CONFIG_DIR=""
export PKG_CONFIG_SYSROOT_DIR="${SERENITY_BUILD_DIR}/Root"
export PKG_CONFIG_LIBDIR="${PKG_CONFIG_SYSROOT_DIR}/usr/lib/pkgconfig/:${PKG_CONFIG_SYSROOT_DIR}/usr/local/lib/pkgconfig"

enable_ccache

DESTDIR="${SERENITY_BUILD_DIR}/Root"
export SERENITY_INSTALL_ROOT="$DESTDIR"
