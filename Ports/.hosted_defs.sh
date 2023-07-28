#!/usr/bin/env bash

SCRIPT="$(realpath $(dirname "${BASH_SOURCE[0]}"))"

export SERENITY_ARCH="${SERENITY_ARCH:-x86_64}"
export SERENITY_TOOLCHAIN="${SERENITY_TOOLCHAIN:-GNU}"

if [ -z "${HOST_CC:=}" ]; then
    export HOST_CC="${CC:=cc}"
    export HOST_CXX="${CXX:=c++}"
    export HOST_LD="${LD:=ld}"
    export HOST_AR="${AR:=ar}"
    export HOST_RANLIB="${RANLIB:=ranlib}"
    export HOST_PATH="${PATH:=}"
    export HOST_READELF="${READELF:=readelf}"
    export HOST_OBJCOPY="${OBJCOPY:=objcopy}"
    export HOST_OBJDUMP="${OBJDUMP:=objdump}"
    export HOST_STRIP="${STRIP:=strip}"
    export HOST_CXXFILT="${CXXFILT:=c++filt}"
    export HOST_PKG_CONFIG_DIR="${PKG_CONFIG_DIR:=}"
    export HOST_PKG_CONFIG_SYSROOT_DIR="${PKG_CONFIG_SYSROOT_DIR:=}"
    export HOST_PKG_CONFIG_LIBDIR="${PKG_CONFIG_LIBDIR:=}"
fi

export SERENITY_SOURCE_DIR="$(realpath "${SCRIPT}/../")"

if [ "$SERENITY_TOOLCHAIN" = "Clang" ]; then
    export SERENITY_BUILD_DIR="${SERENITY_SOURCE_DIR}/Build/${SERENITY_ARCH}clang"
    export SERENITY_TOOLCHAIN_BINDIR="${SERENITY_SOURCE_DIR}/Toolchain/Local/clang/bin"
    export CC="${SERENITY_ARCH}-pc-serenity-clang"
    export CXX="${SERENITY_ARCH}-pc-serenity-clang++"
    export LD="${SERENITY_TOOLCHAIN_BINDIR}/ld.lld"
    export AR="llvm-ar"
    export RANLIB="llvm-ranlib"
    export READELF="llvm-readelf"
    export OBJCOPY="llvm-objcopy"
    export OBJDUMP="llvm-objdump"
    export STRIP="llvm-strip"
    export CXXFILT="llvm-cxxfilt"
else
    export SERENITY_BUILD_DIR="${SERENITY_SOURCE_DIR}/Build/${SERENITY_ARCH}"
    export SERENITY_TOOLCHAIN_BINDIR="${SERENITY_SOURCE_DIR}/Toolchain/Local/${SERENITY_ARCH}/bin"
    export CC="${SERENITY_ARCH}-pc-serenity-gcc"
    export CXX="${SERENITY_ARCH}-pc-serenity-g++"
    export LD="${SERENITY_TOOLCHAIN_BINDIR}/${SERENITY_ARCH}-pc-serenity-ld"
    export AR="${SERENITY_ARCH}-pc-serenity-ar"
    export RANLIB="${SERENITY_ARCH}-pc-serenity-ranlib"
    export READELF="${SERENITY_ARCH}-pc-serenity-readelf"
    export OBJCOPY="${SERENITY_ARCH}-pc-serenity-objcopy"
    export OBJDUMP="${SERENITY_ARCH}-pc-serenity-objdump"
    export STRIP="${SERENITY_ARCH}-pc-serenity-strip"
    export CXXFILT="${SERENITY_ARCH}-pc-serenity-c++filt"
fi

export PATH="${SERENITY_TOOLCHAIN_BINDIR}:${SERENITY_SOURCE_DIR}/Toolchain/Local/cmake/bin:${HOST_PATH}"

export PKG_CONFIG_DIR=""
export PKG_CONFIG_SYSROOT_DIR="${SERENITY_BUILD_DIR}/Root"
export PKG_CONFIG_LIBDIR="${PKG_CONFIG_SYSROOT_DIR}/usr/local/lib/pkgconfig"

export SERENITY_INSTALL_ROOT="${SERENITY_BUILD_DIR}/Root"
export SERENITY_PORT_DIRS="${SERENITY_PORT_DIRS:+${SERENITY_PORT_DIRS}:}${SERENITY_SOURCE_DIR}/Ports"
