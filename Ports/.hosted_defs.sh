#!/usr/bin/env bash

SCRIPT="$(dirname "${0}")"

export SERENITY_ARCH="${SERENITY_ARCH:-i686}"
export SERENITY_TOOLCHAIN="${SERENITY_TOOLCHAIN:-GCC}"

if [ -z "${HOST_CC:=}" ]; then
    export HOST_CC="${CC:=cc}"
    export HOST_CXX="${CXX:=c++}"
    export HOST_AR="${AR:=ar}"
    export HOST_RANLIB="${RANLIB:=ranlib}"
    export HOST_PATH="${PATH:=}"
    export HOST_READELF="${READELF:=readelf}"
    export HOST_OBJCOPY="${OBJCOPY:=objcopy}"
    export HOST_PKG_CONFIG_DIR="${PKG_CONFIG_DIR:=}"
    export HOST_PKG_CONFIG_SYSROOT_DIR="${PKG_CONFIG_SYSROOT_DIR:=}"
    export HOST_PKG_CONFIG_LIBDIR="${PKG_CONFIG_LIBDIR:=}"
fi

export SERENITY_SOURCE_DIR="$(realpath "${SCRIPT}/../")"

if [ "$SERENITY_TOOLCHAIN" = "Clang" ]; then
    export SERENITY_BUILD_DIR="${SERENITY_SOURCE_DIR}/Build/${SERENITY_ARCH}clang"
    export CC="clang --target=${SERENITY_ARCH}-pc-serenity --sysroot=${SERENITY_BUILD_DIR}/Root"
    export CXX="clang++ --target=${SERENITY_ARCH}-pc-serenity --sysroot=${SERENITY_BUILD_DIR}/Root"
    export AR="llvm-ar"
    export RANLIB="llvm-ranlib"
    export READELF="llvm-readelf"
    export OBJCOPY="llvm-objcopy"
    export PATH="${SERENITY_SOURCE_DIR}/Toolchain/Local/clang/bin:${HOST_PATH}"
else
    export SERENITY_BUILD_DIR="${SERENITY_SOURCE_DIR}/Build/${SERENITY_ARCH}"
    export CC="${SERENITY_ARCH}-pc-serenity-gcc"
    export CXX="${SERENITY_ARCH}-pc-serenity-g++"
    export AR="${SERENITY_ARCH}-pc-serenity-ar"
    export RANLIB="${SERENITY_ARCH}-pc-serenity-ranlib"
    export READELF="${SERENITY_ARCH}-pc-serenity-readelf"
    export OBJCOPY="${SERENITY_ARCH}-pc-serenity-objcopy"
    export PATH="${SERENITY_SOURCE_DIR}/Toolchain/Local/${SERENITY_ARCH}/bin:${HOST_PATH}"
fi

export PKG_CONFIG_DIR=""
export PKG_CONFIG_SYSROOT_DIR="${SERENITY_BUILD_DIR}/Root"
export PKG_CONFIG_LIBDIR="${PKG_CONFIG_SYSROOT_DIR}/usr/lib/pkgconfig/:${PKG_CONFIG_SYSROOT_DIR}/usr/local/lib/pkgconfig"

export SERENITY_INSTALL_ROOT="${SERENITY_BUILD_DIR}/Root"
