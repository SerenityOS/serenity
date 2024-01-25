#!/usr/bin/env bash
set -e

# This file will need to be run in bash, for now.

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# shellcheck source=/dev/null
. "${DIR}/../Meta/shell_include.sh"

exit_if_running_as_root 'Do not run BuildUBoot.sh as root, parts of your Toolchain directory will become root-owned'

# Since we don't have a Serenity port of U-Boot for now, the version information lives here.
UBOOT_VERSION='2023.10'
UBOOT_ARCHIVE="v${UBOOT_VERSION}.tar.gz"
UBOOT_DIR="u-boot-${UBOOT_VERSION}"
UBOOT_ARCHIVE_URL="https://github.com/u-boot/u-boot/archive/refs/tags/${UBOOT_ARCHIVE}"
UBOOT_ARCHIVE_SHA256SUM="b22664ee56640bba87068a7cdcd7cb50f956973a348e844788d2fb882fe0dc55"

mkdir -p "${DIR}/Tarballs"

# Download and extract U-Boot source.
pushd "${DIR}/Tarballs"
    if [ ! -e "${UBOOT_ARCHIVE}" ]; then
        curl -LC - -O "${UBOOT_ARCHIVE_URL}"
    else
        echo "Skipped downloading ${UBOOT_ARCHIVE}"
    fi

    if ! sha256sum --status -c <(echo "${UBOOT_ARCHIVE_SHA256SUM}" "${UBOOT_ARCHIVE}"); then
        echo "U-Boot sha256 sum mismatching, please run script again."
        rm -f "${UBOOT_ARCHIVE}"
        exit 1
    fi

    if [ -d "$UBOOT_DIR" ]; then
        rm -rf "$UBOOT_DIR"
    fi

    echo "Extracting U-Boot..."
    tar -xf "${UBOOT_ARCHIVE}"
popd

NPROC=$(get_number_of_processing_units)
[ -z "$MAKEJOBS" ] && MAKEJOBS=${NPROC}

# RISC-V QEMU U-Boot build.
# This can be generalized later to build different U-Boot bootloaders for other (emulated) machines.

PREFIX="${DIR}/Local/u-boot-riscv64"
echo PREFIX is "$PREFIX"
rm -rf "$PREFIX"

# Check that we have a cross compiler available for building U-Boot.
# Clang cannot be used; it requires some patches to the U-Boot build system to compile and then crashes on boot.
LINUX_CROSS_GCC=$(find_executable "riscv64-linux-gnu-gcc")
if [ -x "${DIR}/Local/riscv64/bin/riscv64-pc-serenity-gcc" ]; then
	CROSS_COMPILE="${DIR}/Local/riscv64/bin/riscv64-pc-serenity-"
elif [ -x "$LINUX_CROSS_GCC" ]; then
    CROSS_COMPILE="${LINUX_CROSS_GCC%gcc}"
fi

if [ "${CROSS_COMPILE:x}" == 'x' ]; then
    die Required cross-compiler not found. Please build a RISC-V toolchain or install the riscv64-linux-gnu cross-compiler.
fi

export CROSS_COMPILE
echo Found cross compiler prefix "$CROSS_COMPILE"

MAKEARGS="${MAKEARGS} O=${PREFIX}"

# Build.
pushd "${DIR}/Tarballs/${UBOOT_DIR}"
    # shellcheck disable=SC2086 # Word splitting is intentional here
    make $MAKEARGS mrproper

    # shellcheck disable=SC2086
    make $MAKEARGS qemu-riscv64_smode_defconfig
    # Merge in our own configuration fragments.
    scripts/kconfig/merge_config.sh -m -O "$PREFIX" "${PREFIX}/.config" "${DIR}/u-boot-riscv64.config"

    # shellcheck disable=SC2086
    make $MAKEARGS -j "$MAKEJOBS" all || exit 1
popd
