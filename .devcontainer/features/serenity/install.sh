#!/bin/sh
set -e

# Feature options

LLVM_VERSION=${LLVM_VERSION:-18}
ENABLE_LADYBIRD=${ENABLE_LADYBIRD:-true}
ENABLE_SERENITY=${ENABLE_SERENITY:-true}

### Check distro

if [ ! -f /etc/lsb-release ]; then
    echo "Not an Ubuntu container, add logic for your distro to the serenity feature or use Ubuntu"
    exit 1
fi

# shellcheck source=/dev/null
. /etc/lsb-release

### Declare helper functions

install_llvm_key() {
    wget -O /usr/share/keyrings/llvm-snapshot.gpg.key https://apt.llvm.org/llvm-snapshot.gpg.key
    echo "deb [signed-by=/usr/share/keyrings/llvm-snapshot.gpg.key] http://apt.llvm.org/${DISTRIB_CODENAME}/ llvm-toolchain-${DISTRIB_CODENAME} main" | tee -a /etc/apt/sources.list.d/llvm.list
    if [ ! "${LLVM_VERSION}" = "trunk" ]; then
        echo "deb [signed-by=/usr/share/keyrings/llvm-snapshot.gpg.key] http://apt.llvm.org/${DISTRIB_CODENAME}/ llvm-toolchain-${DISTRIB_CODENAME}-${LLVM_VERSION} main" | tee -a /etc/apt/sources.list.d/llvm.list
    fi
    apt update -y
}

### Install packages

apt update -y
apt install -y build-essential cmake ninja-build ccache shellcheck
if [ "${ENABLE_LADYBIRD}" = "true" ]; then
    apt install -y libgl1-mesa-dev qt6-base-dev qt6-tools-dev-tools qt6-wayland qt6-multimedia-dev
fi
if [ "${ENABLE_SERENITY}" = "true" ]; then
    apt install -y curl libmpfr-dev libmpc-dev libgmp-dev e2fsprogs genext2fs qemu-system-gui qemu-system-x86 qemu-utils rsync unzip texinfo libssl-dev
fi

### Ensure new enough host compiler is available

VERSION="0.0.0"
if command -v clang >/dev/null 2>&1; then
    VERSION="$(clang -dumpversion)"
fi
MAJOR_VERSION="${VERSION%%.*}"

if [ "${LLVM_VERSION}" = "trunk" ]; then
    install_llvm_key

    apt install -y llvm clang clangd clang-tools lld lldb clang-tidy clang-format
elif [ "${MAJOR_VERSION}" -lt "${LLVM_VERSION}" ]; then
    FAILED_INSTALL=0
    apt install -y "llvm-${LLVM_VERSION}" "clang-${LLVM_VERSION}" "clangd-${LLVM_VERSION}" "clang-tools-${LLVM_VERSION}" "lld-${LLVM_VERSION}" "lldb-${LLVM_VERSION}" "clang-tidy-${LLVM_VERSION}" "clang-format-${LLVM_VERSION}"  || FAILED_INSTALL=1

    if [ "${FAILED_INSTALL}" -ne 0 ]; then
        install_llvm_key
        apt install -y "llvm-${LLVM_VERSION}" "clang-${LLVM_VERSION}" "clangd-${LLVM_VERSION}" "clang-tools-${LLVM_VERSION}" "lld-${LLVM_VERSION}" "lldb-${LLVM_VERSION}" "clang-tidy-${LLVM_VERSION}" "clang-format-${LLVM_VERSION}"
    fi
fi
