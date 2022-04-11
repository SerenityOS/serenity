#!/bin/sh

set -e

# sourcing this file will export SERENITY_QEMU_IMG_BIN and SERENITY_QEMU_BIN
# in such a way that they will contain the correct paths also under WSL

if command -v wslpath >/dev/null; then
    # existence of wslpath implies we are running inside WSL on Windows
    # the Windows installation of QEMU adds the install directory of itself
    # to the registry, we use powershell to get the install directory

    # System32 and cohorts must be on the PATH for the build to work anyways so using powershell without full path is fine
    QEMU_INSTALL_DIR=$(PowerShell.exe -Command Get-ItemPropertyValue 'HKLM:\SOFTWARE\QEMU' -Name Install_Dir)

    if [ -z "$QEMU_INSTALL_DIR" ]; then
        if [ "$KVM_SUPPORT" -eq "0" ]; then
            die "Could not determine where QEMU for Windows is installed. Please make sure QEMU is installed or set SERENITY_QEMU_IMG_BIN and SERENITY_QEMU_BIN if it is already installed."
        fi
    else
        KVM_SUPPORT="0"
        QEMU_BINARY_PREFIX="$(wslpath -- "${QEMU_INSTALL_DIR}" | tr -d '\r\n')/"
        QEMU_BINARY_SUFFIX=".exe"
    fi
fi

if [ -z "$SERENITY_QEMU_IMG_BIN" ]; then
    SERENITY_QEMU_IMG_BIN="${QEMU_BINARY_PREFIX}qemu-img${QEMU_BINARY_SUFFIX}"
fi

if [ -z "$SERENITY_QEMU_BIN" ]; then
    if [ "$SERENITY_ARCH" = "aarch64" ]; then
        SERENITY_QEMU_BIN="${QEMU_BINARY_PREFIX}qemu-system-aarch64${QEMU_BINARY_SUFFIX}"
    elif [ "$SERENITY_ARCH" = "x86_64" ]; then
        SERENITY_QEMU_BIN="${QEMU_BINARY_PREFIX}qemu-system-x86_64${QEMU_BINARY_SUFFIX}"
    else
        SERENITY_QEMU_BIN="${QEMU_BINARY_PREFIX}qemu-system-i386${QEMU_BINARY_SUFFIX}"
    fi
fi

export SERENITY_QEMU_IMG_BIN
export SERENITY_QEMU_BIN
