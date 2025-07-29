#!/usr/bin/env bash

set -e

# edksetup.sh defines DIR, so we have to call it _DIR here.
_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

. "${_DIR}/../Meta/shell_include.sh"

exit_if_running_as_root "Do not run BuildEDK2.sh as root, parts of your Toolchain directory will become root-owned"

NPROC=$(get_number_of_processing_units)
[ -z "${MAKEJOBS-}" ] && MAKEJOBS=${NPROC}

if [ $# -ne 1 ]; then
    echo "Usage: $0 PLATFORM"
    echo "       PLATFORM must be one of: rpi3, rpi4, riscv64-virt, aarch64-virt, x86_64-ovmf"
    exit 1
fi

case $1 in
    rpi3)
        TARGET_ARCH=AARCH64
        SERENITY_ARCH=aarch64
        PLATFORM_FILE="$_DIR/Tarballs/edk2-platforms/Platform/RaspberryPi/RPi3/RPi3.dsc"

        # Enable ACPI+Devicetree (default is only ACPI) and add the `tftp` command to the UEFI shell.
        EXTRA_BUILD_OPTIONS="--pcd gRaspberryPiTokenSpaceGuid.PcdSystemTableMode=1 -DINCLUDE_TFTP_COMMAND=TRUE"
        ;;
    rpi4)
        TARGET_ARCH=AARCH64
        SERENITY_ARCH=aarch64
        PLATFORM_FILE="$_DIR/Tarballs/edk2-platforms/Platform/RaspberryPi/RPi4/RPi4.dsc"

        # Enable ACPI+Devicetree (default is only ACPI) and add the `tftp` command to the UEFI shell.
        EXTRA_BUILD_OPTIONS="--pcd gRaspberryPiTokenSpaceGuid.PcdSystemTableMode=1 -DINCLUDE_TFTP_COMMAND=TRUE"
        ;;
    aarch64-virt)
        TARGET_ARCH=AARCH64
        SERENITY_ARCH=aarch64
        PLATFORM_FILE="$_DIR/Tarballs/edk2/ArmVirtPkg/ArmVirtQemu.dsc"
        ;;
    riscv64-virt)
        TARGET_ARCH=RISCV64
        SERENITY_ARCH=riscv64
        PLATFORM_FILE="$_DIR/Tarballs/edk2/OvmfPkg/RiscVVirt/RiscVVirtQemu.dsc"
        ;;
    x86_64-ovmf)
        TARGET_ARCH=X64
        SERENITY_ARCH=x86_64
        PLATFORM_FILE="$_DIR/Tarballs/edk2/OvmfPkg/OvmfPkgX64.dsc"
        ;;
    *)
        echo "Unknown platform: $1"
        exit 1
        ;;
esac

shift # edksetup.sh wants no arguments to be set

EDK2_REPO=https://github.com/tianocore/edk2
EDK2_TAG=edk2-stable202505

EDK2_PLATFORMS_REPO=https://github.com/tianocore/edk2-platforms
EDK2_PLATFORMS_REV=c639ab7149a985c196317acec3dd96383c07abbb

# Needed for the Raspberry Pi platform.
EDK2_NON_OSI_REPO=https://github.com/tianocore/edk2-non-osi
EDK2_NON_OSI_REV=ea2040c2d4e2200557e87b9f9fbd4f8fb7a2b6e8

mkdir -p "$_DIR"/Tarballs
pushd "$_DIR"/Tarballs
    [ ! -d edk2 ] && git clone $EDK2_REPO --depth 1 --branch $EDK2_TAG
    [ ! -d edk2-platforms ] && git clone $EDK2_PLATFORMS_REPO --depth 1 --revision $EDK2_PLATFORMS_REV
    [ ! -d edk2-non-osi ] && git clone $EDK2_NON_OSI_REPO --depth 1 --revision $EDK2_NON_OSI_REV

    pushd edk2
        git fetch origin
        git checkout $EDK2_TAG
        git submodule update --init
    popd

    pushd edk2-platforms
        git fetch origin
        git checkout $EDK2_PLATFORMS_REV
        git submodule update --init

        # Don't use the non-open source Raspberry Pi logo.
        sed -i 's|Platform/RaspberryPi/Drivers/LogoDxe/LogoDxe.inf|MdeModulePkg/Logo/LogoDxe.inf|' \
            Platform/RaspberryPi/{RPi3/RPi3.fdf,RPi3/RPi3.dsc,RPi4/RPi4.fdf,RPi4/RPi4.dsc}
    popd

    pushd edk2-non-osi
        git fetch origin
        git checkout $EDK2_NON_OSI_REV
        git submodule update --init

        rm -rf Platform/RaspberryPi/Drivers/LogoDxe
    popd
popd

export PATH="${_DIR}/Local/${SERENITY_ARCH}/bin:${PATH}"

# These environment variables will be used by the EDK II build scripts.
export WORKSPACE="$_DIR/Build/edk2"
export PACKAGES_PATH="$_DIR/Tarballs/edk2:$_DIR/Tarballs/edk2-platforms:$_DIR/Tarballs/edk2-non-osi"
export GCC5_AARCH64_PREFIX=aarch64-pc-serenity-
export GCC5_RISCV64_PREFIX=riscv64-pc-serenity-
export GCC5_X64_PREFIX=x86_64-pc-serenity-

mkdir -p "$WORKSPACE"

# shellcheck source=/dev/null
source "$_DIR/Tarballs/edk2/edksetup.sh"

make -C "$_DIR/Tarballs/edk2/BaseTools" -j "$MAKEJOBS" # needs to be an in-tree build

# shellcheck source=/dev/null
source "$_DIR/Tarballs/edk2/edksetup.sh" BaseTools

# shellcheck disable=SC2086 # Word splitting is intentional here
build -t GCC5 -b RELEASE -n "$MAKEJOBS" -a "$TARGET_ARCH" -p "$PLATFORM_FILE" $EXTRA_BUILD_OPTIONS
