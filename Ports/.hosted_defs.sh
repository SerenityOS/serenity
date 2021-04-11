#!/usr/bin/env bash

export SERENITY_ROOT="$(realpath "${SCRIPT}/../")"
export SERENITY_BUILD_DIR="${SERENITY_ROOT}/Build/${SERENITY_ARCH}"
export CC="${SERENITY_ARCH}-pc-serenity-gcc"
export CXX="${SERENITY_ARCH}-pc-serenity-g++"
export AR="${SERENITY_ARCH}-pc-serenity-ar"
export RANLIB="${SERENITY_ARCH}-pc-serenity-ranlib"
export PATH="${SERENITY_ROOT}/Toolchain/Local/${SERENITY_ARCH}/bin:${PATH}"
export DESTDIR="${SERENITY_BUILD_DIR}/Root"
export PKG_CONFIG_DIR=""
export PKG_CONFIG_SYSROOT_DIR="${SERENITY_BUILD_DIR}/Root"
export PKG_CONFIG_LIBDIR="${PKG_CONFIG_SYSROOT_DIR}/usr/lib/pkgconfig/:${PKG_CONFIG_SYSROOT_DIR}/usr/local/lib/pkgconfig"
