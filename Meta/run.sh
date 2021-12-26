#!/bin/sh
# shellcheck disable=SC2086 # FIXME: fix these globing warnings

set -e

die() {
    echo "die: $*"
    exit 1
}

SCRIPT_DIR="$(dirname "${0}")"

# https://www.shellcheck.net/wiki/SC1090 No need to shellcheck private config.
# shellcheck source=/dev/null
[ -x "$SCRIPT_DIR/../run-local.sh" ] && . "$SCRIPT_DIR/../run-local.sh"

#SERENITY_PACKET_LOGGING_ARG="-object filter-dump,id=hue,netdev=breh,file=e1000.pcap"

KVM_SUPPORT="0"
[ -e /dev/kvm ] && [ -r /dev/kvm ] && [ -w /dev/kvm ] && KVM_SUPPORT="1"

[ -z "$SERENITY_BOCHS_BIN" ] && SERENITY_BOCHS_BIN="bochs"

# To support virtualization acceleration on mac
# we need to use 64-bit qemu
if [ "$(uname)" = "Darwin" ] && [ "$(uname -m)" = "x86_64" ]; then

    [ -z "$SERENITY_QEMU_BIN" ] && SERENITY_QEMU_BIN="qemu-system-x86_64"

    if $SERENITY_QEMU_BIN --accel help | grep -q hvf; then
        SERENITY_VIRT_TECH_ARG="--accel hvf"
    fi
fi

# Prepend the toolchain qemu directory so we pick up QEMU from there
PATH="$SCRIPT_DIR/../Toolchain/Local/qemu/bin:$PATH"

# Also prepend the i686 toolchain directory because that's where most
# people will have their QEMU binaries if they built them before the
# directory was changed to Toolchain/Local/qemu.
PATH="$SCRIPT_DIR/../Toolchain/Local/i686/bin:$PATH"

SERENITY_RUN="${SERENITY_RUN:-$1}"

if [ -z "$SERENITY_QEMU_BIN" ]; then
    if command -v wslpath >/dev/null; then
        QEMU_INSTALL_DIR=$(reg.exe query 'HKLM\Software\QEMU' /v Install_Dir /t REG_SZ | grep '^    Install_Dir' | sed 's/    / /g' | cut -f4- -d' ')
        if [ -z "$QEMU_INSTALL_DIR" ]; then
            if [ "$KVM_SUPPORT" -eq "0" ]; then
                die "Could not determine where QEMU for Windows is installed. Please make sure QEMU is installed or set SERENITY_QEMU_BIN if it is already installed."
            fi
        else
            KVM_SUPPORT="0"
            QEMU_BINARY_PREFIX="$(wslpath -- "${QEMU_INSTALL_DIR}" | tr -d '\r\n')/"
            QEMU_BINARY_SUFFIX=".exe"
        fi
    fi
    if command -v "${QEMU_BINARY_PREFIX}qemu-system-x86_64${QEMU_BINARY_SUFFIX}" >/dev/null; then
        SERENITY_QEMU_BIN="${QEMU_BINARY_PREFIX}qemu-system-x86_64${QEMU_BINARY_SUFFIX}"
    else
        if [ "$SERENITY_ARCH" = "x86_64" ]; then
            die "Please install the 64-bit QEMU system emulator (qemu-system-x86_64)."
        fi
        SERENITY_QEMU_BIN="${QEMU_BINARY_PREFIX}qemu-system-i386${QEMU_BINARY_SUFFIX}"
    fi
fi

[ "$KVM_SUPPORT" -eq "1" ] && SERENITY_VIRT_TECH_ARG="-enable-kvm"

[ -z "$SERENITY_KERNEL_CMDLINE" ] && SERENITY_KERNEL_CMDLINE="hello"

[ -z "$SERENITY_RAM_SIZE" ] && SERENITY_RAM_SIZE=512M

[ -z "$SERENITY_DISK_IMAGE" ] && {
    if [ "$SERENITY_RUN" = qgrub ]; then
        SERENITY_DISK_IMAGE="grub_disk_image"
    elif [ "$SERENITY_RUN" = qextlinux ]; then
        SERENITY_DISK_IMAGE="extlinux_disk_image"
    else
        SERENITY_DISK_IMAGE="_disk_image"
    fi
    if command -v wslpath >/dev/null; then
        case "$SERENITY_QEMU_BIN" in
            /mnt/?/*)
                SERENITY_DISK_IMAGE=$(wslpath -w "$SERENITY_DISK_IMAGE")
                ;;
        esac
    fi
}

if ! command -v "$SERENITY_QEMU_BIN" >/dev/null 2>&1 ; then
    die "Please install QEMU version 5.0 or newer or use the Toolchain/BuildQemu.sh script."
fi

SERENITY_QEMU_MIN_REQ_VERSION=5
installed_major_version=$("$SERENITY_QEMU_BIN" -version | head -n 1 | sed -E 's/QEMU emulator version ([1-9][0-9]*|0).*/\1/')
installed_minor_version=$("$SERENITY_QEMU_BIN" -version | head -n 1 | sed -E 's/QEMU emulator version [0-9]+\.([1-9][0-9]*|0).*/\1/')
if [ "$installed_major_version" -lt "$SERENITY_QEMU_MIN_REQ_VERSION" ]; then
    echo "Required QEMU >= 5.0! Found $($SERENITY_QEMU_BIN -version | head -n 1)"
    echo "Please install a newer version of QEMU or use the Toolchain/BuildQemu.sh script."
    die
fi

NATIVE_WINDOWS_QEMU="0"

if command -v wslpath >/dev/null; then
    case "$SERENITY_QEMU_BIN" in
        /mnt/?/*)
            if [ -z "$SERENITY_VIRT_TECH_ARG" ]; then
                if [ "$installed_major_version" -gt 5 ]; then
                    SERENITY_VIRT_TECH_ARG="-accel whpx,kernel-irqchip=off -accel tcg"
                else
                    SERENITY_VIRT_TECH_ARG="-accel whpx -accel tcg"
                fi
            fi
            [ -z "$SERENITY_QEMU_CPU" ] && SERENITY_QEMU_CPU="max,vmx=off"
            SERENITY_KERNEL_CMDLINE="$SERENITY_KERNEL_CMDLINE disable_virtio"
            NATIVE_WINDOWS_QEMU="1"
            ;;
    esac
fi

[ -z "$SERENITY_QEMU_CPU" ] && SERENITY_QEMU_CPU="max"

if [ -z "$SERENITY_SPICE" ] && "${SERENITY_QEMU_BIN}" -chardev help | grep -iq qemu-vdagent; then
    SERENITY_SPICE_SERVER_CHARDEV="-chardev qemu-vdagent,clipboard=on,mouse=off,id=vdagent,name=vdagent"
elif "${SERENITY_QEMU_BIN}" -chardev help | grep -iq spicevmc; then
    SERENITY_SPICE_SERVER_CHARDEV="-chardev spicevmc,id=vdagent,name=vdagent"
fi

if [ "$(uname)" = "Darwin" ]; then
    SERENITY_AUDIO_BACKEND="-audiodev coreaudio,id=snd0"
elif [ "$NATIVE_WINDOWS_QEMU" -eq "1" ]; then
    SERENITY_AUDIO_BACKEND="-audiodev dsound,id=snd0"
elif "$SERENITY_QEMU_BIN" -audio-help 2>&1 | grep -- "-audiodev id=sdl" >/dev/null; then
    SERENITY_AUDIO_BACKEND="-audiodev sdl,id=snd0"
else
    SERENITY_AUDIO_BACKEND="-audiodev pa,id=snd0"
fi

if [ "$installed_major_version" -eq 5 ] && [ "$installed_minor_version" -eq 0 ]; then
    SERENITY_AUDIO_HW="-soundhw pcspk"
else
    SERENITY_AUDIO_HW="-machine pcspk-audiodev=snd0"
fi

SERENITY_SCREENS="${SERENITY_SCREENS:-1}"
if [ "$SERENITY_SPICE" ]; then
    SERENITY_QEMU_DISPLAY_BACKEND="${SERENITY_QEMU_DISPLAY_BACKEND:-spice-app}"
elif [ "$NATIVE_WINDOWS_QEMU" -eq "1" ]; then
    # QEMU for windows does not like gl=on, so detect if we are building in wsl, and if so, disable it
    # Also, when using the GTK backend we run into this problem: https://github.com/SerenityOS/serenity/issues/7657
    SERENITY_QEMU_DISPLAY_BACKEND="${SERENITY_QEMU_DISPLAY_BACKEND:-sdl,gl=off}"
elif [ $SERENITY_SCREENS -gt 1 ] && "${SERENITY_QEMU_BIN}" --display help | grep -iq sdl; then
    SERENITY_QEMU_DISPLAY_BACKEND="${SERENITY_QEMU_DISPLAY_BACKEND:-sdl,gl=off}"
elif ! command -v wslpath >/dev/null && ("${SERENITY_QEMU_BIN}" --display help | grep -iq sdl) && (ldconfig -p | grep -iq virglrenderer); then
    SERENITY_QEMU_DISPLAY_BACKEND="${SERENITY_QEMU_DISPLAY_BACKEND:-sdl,gl=on}"
elif "${SERENITY_QEMU_BIN}" --display help | grep -iq cocoa; then
    # QEMU for OSX seems to only support cocoa
    SERENITY_QEMU_DISPLAY_BACKEND="${SERENITY_QEMU_DISPLAY_BACKEND:-cocoa,gl=off}"
else
    SERENITY_QEMU_DISPLAY_BACKEND="${SERENITY_QEMU_DISPLAY_BACKEND:-gtk,gl=off}"
fi

if [ "$SERENITY_SCREENS" -gt 1 ]; then
    SERENITY_QEMU_DISPLAY_DEVICE="virtio-vga,max_outputs=$SERENITY_SCREENS "
    # QEMU appears to always relay absolute mouse coordinates relative to the screen that the mouse is
    # pointed to, without any way for us to know what screen it was. So, when dealing with multiple
    # displays force using relative coordinates only
    SERENITY_KERNEL_CMDLINE="$SERENITY_KERNEL_CMDLINE vmmouse=off"
else
    SERENITY_QEMU_DISPLAY_DEVICE="VGA,vgamem_mb=64 "
fi

if [ -z "$SERENITY_DISABLE_GDB_SOCKET" ]; then
  SERENITY_EXTRA_QEMU_ARGS="$SERENITY_EXTRA_QEMU_ARGS -s"
fi

if [ -z "$SERENITY_ETHERNET_DEVICE_TYPE" ]; then
  SERENITY_ETHERNET_DEVICE_TYPE="e1000"
fi

[ -z "$SERENITY_COMMON_QEMU_ARGS" ] && SERENITY_COMMON_QEMU_ARGS="
$SERENITY_EXTRA_QEMU_ARGS
-m $SERENITY_RAM_SIZE
-cpu $SERENITY_QEMU_CPU
-d guest_errors
-smp 2
-display $SERENITY_QEMU_DISPLAY_BACKEND
-device $SERENITY_QEMU_DISPLAY_DEVICE
-drive file=${SERENITY_DISK_IMAGE},format=raw,index=0,media=disk
-usb
$SERENITY_SPICE_SERVER_CHARDEV
-device virtio-serial,max_ports=2
-chardev stdio,id=stdout,mux=on
-device virtconsole,chardev=stdout
-device isa-debugcon,chardev=stdout
-device virtio-rng-pci
$SERENITY_AUDIO_BACKEND
$SERENITY_AUDIO_HW
-device sb16,audiodev=snd0
-device pci-bridge,chassis_nr=1,id=bridge1 -device $SERENITY_ETHERNET_DEVICE_TYPE,bus=bridge1
-device i82801b11-bridge,bus=bridge1,id=bridge2 -device sdhci-pci,bus=bridge2
-device i82801b11-bridge,id=bridge3 -device sdhci-pci,bus=bridge3
-device ich9-ahci,bus=bridge3
"

if "${SERENITY_QEMU_BIN}" -chardev help | grep -iq spice; then
    SERENITY_COMMON_QEMU_ARGS="$SERENITY_COMMON_QEMU_ARGS
    -spice port=5930,agent-mouse=off,disable-ticketing=on
    -device virtserialport,chardev=vdagent,nr=1
    "
fi

[ -z "$SERENITY_COMMON_QEMU_Q35_ARGS" ] && SERENITY_COMMON_QEMU_Q35_ARGS="
$SERENITY_EXTRA_QEMU_ARGS
-m $SERENITY_RAM_SIZE
-cpu $SERENITY_QEMU_CPU
-machine q35
-d guest_errors
-smp 2
-vga none
-device bochs-display
-device ich9-usb-ehci1,bus=pcie.0,multifunction=on,addr=0x5.0x0
-device ich9-usb-ehci2,bus=pcie.0,addr=0x5.0x2
-device ich9-usb-uhci1,bus=pcie.0,multifunction=on,addr=0x7.0x0
-device ich9-usb-uhci2,bus=pcie.0,addr=0x7.0x1
-device ich9-usb-uhci3,bus=pcie.0,addr=0x7.0x2
-device ich9-usb-uhci4,bus=pcie.0,addr=0x7.0x3
-device ich9-usb-uhci5,bus=pcie.0,addr=0x7.0x4
-device ich9-usb-uhci6,bus=pcie.0,addr=0x7.0x5
-device pcie-root-port,port=0x10,chassis=1,id=pcie.1,bus=pcie.0,multifunction=on,addr=0x6
-device pcie-root-port,port=0x11,chassis=2,id=pcie.2,bus=pcie.0,addr=0x6.0x1
-device pcie-root-port,port=0x12,chassis=3,id=pcie.3,bus=pcie.0,addr=0x6.0x2
-device pcie-root-port,port=0x13,chassis=4,id=pcie.4,bus=pcie.0,addr=0x6.0x3
-device pcie-root-port,port=0x14,chassis=5,id=pcie.5,bus=pcie.0,addr=0x6.0x4
-device pcie-root-port,port=0x15,chassis=6,id=pcie.6,bus=pcie.0,addr=0x6.0x5
-device pcie-root-port,port=0x16,chassis=7,id=pcie.7,bus=pcie.0,addr=0x6.0x6
-device pcie-root-port,port=0x17,chassis=8,id=pcie.8,bus=pcie.0,addr=0x6.0x7
-device bochs-display,bus=pcie.6,addr=0x10.0x0
-device ich9-intel-hda,bus=pcie.2,addr=0x03.0x0
-device nec-usb-xhci,bus=pcie.2,addr=0x11.0x0
-device pci-bridge,chassis_nr=1,id=bridge1,bus=pcie.4,addr=0x3.0x0
-device sdhci-pci,bus=bridge1,addr=0x1.0x0
-display $SERENITY_QEMU_DISPLAY_BACKEND
-drive file=${SERENITY_DISK_IMAGE},format=raw,id=disk,if=none
-device ahci,id=ahci
-device ide-hd,bus=ahci.0,drive=disk,unit=0
-device virtio-serial
-chardev stdio,id=stdout,mux=on
-device virtconsole,chardev=stdout
-device isa-debugcon,chardev=stdout
-device virtio-rng-pci
$SERENITY_AUDIO_BACKEND
$SERENITY_AUDIO_HW
"

export SDL_VIDEO_X11_DGAMOUSE=0

: "${SERENITY_BUILD:=.}"
cd -P -- "$SERENITY_BUILD" || die "Could not cd to \"$SERENITY_BUILD\""

if [ "$SERENITY_RUN" = "b" ]; then
    # Meta/run.sh b: bochs
    [ -z "$SERENITY_BOCHSRC" ] && {
        # Make sure that SERENITY_SOURCE_DIR is set and not empty
        [ -z "$SERENITY_SOURCE_DIR" ] && die 'SERENITY_SOURCE_DIR not set or empty'
        SERENITY_BOCHSRC="$SERENITY_SOURCE_DIR/Meta/bochsrc"
    }
    "$SERENITY_BOCHS_BIN" -q -f "$SERENITY_BOCHSRC"
elif [ "$SERENITY_RUN" = "qn" ]; then
    # Meta/run.sh qn: qemu without network
    "$SERENITY_QEMU_BIN" \
        $SERENITY_COMMON_QEMU_ARGS \
        -device $SERENITY_ETHERNET_DEVICE_TYPE \
        -kernel Kernel/Prekernel/Prekernel \
        -initrd Kernel/Kernel \
        -append "${SERENITY_KERNEL_CMDLINE}"
elif [ "$SERENITY_RUN" = "qtap" ]; then
    # Meta/run.sh qtap: qemu with tap
    sudo ip tuntap del dev tap0 mode tap || true
    sudo ip tuntap add dev tap0 mode tap user "$(id -u)"
    "$SERENITY_QEMU_BIN" \
        $SERENITY_COMMON_QEMU_ARGS \
        $SERENITY_VIRT_TECH_ARG \
        $SERENITY_PACKET_LOGGING_ARG \
        -netdev tap,ifname=tap0,id=br0 \
        -device $SERENITY_ETHERNET_DEVICE_TYPE,netdev=br0 \
        -kernel Kernel/Prekernel/Prekernel \
        -initrd Kernel/Kernel \
        -append "${SERENITY_KERNEL_CMDLINE}"
    sudo ip tuntap del dev tap0 mode tap
elif [ "$SERENITY_RUN" = "qgrub" ] || [ "$SERENITY_RUN" = "qextlinux" ]; then
    # Meta/run.sh qgrub: qemu with grub/extlinux
    "$SERENITY_QEMU_BIN" \
        $SERENITY_COMMON_QEMU_ARGS \
        $SERENITY_VIRT_TECH_ARG \
        $SERENITY_PACKET_LOGGING_ARG \
        -netdev user,id=breh,hostfwd=tcp:127.0.0.1:8888-10.0.2.15:8888,hostfwd=tcp:127.0.0.1:8823-10.0.2.15:23 \
        -device $SERENITY_ETHERNET_DEVICE_TYPE,netdev=breh
elif [ "$SERENITY_RUN" = "q35" ]; then
    # Meta/run.sh q35: qemu (q35 chipset) with SerenityOS
    echo "Starting SerenityOS with QEMU Q35 machine, Commandline: ${SERENITY_KERNEL_CMDLINE}"
    "$SERENITY_QEMU_BIN" \
        $SERENITY_COMMON_QEMU_Q35_ARGS \
        $SERENITY_VIRT_TECH_ARG \
        -netdev user,id=breh,hostfwd=tcp:127.0.0.1:8888-10.0.2.15:8888,hostfwd=tcp:127.0.0.1:8823-10.0.2.15:23 \
        -device $SERENITY_ETHERNET_DEVICE_TYPE,netdev=breh \
        -kernel Kernel/Prekernel/Prekernel \
        -initrd Kernel/Kernel \
        -append "${SERENITY_KERNEL_CMDLINE}"
elif [ "$SERENITY_RUN" = "ci" ]; then
    # Meta/run.sh ci: qemu in text mode
    echo "Running QEMU in CI"
    "$SERENITY_QEMU_BIN" \
        $SERENITY_EXTRA_QEMU_ARGS \
        $SERENITY_VIRT_TECH_ARG \
        -m $SERENITY_RAM_SIZE \
        -cpu $SERENITY_QEMU_CPU \
        -d guest_errors \
        -no-reboot \
        -smp 2 \
        -drive file=${SERENITY_DISK_IMAGE},format=raw,index=0,media=disk \
        -device ich9-ahci \
        -nographic \
        -display none \
        -debugcon file:debug.log \
        -kernel Kernel/Prekernel/Prekernel \
        -initrd Kernel/Kernel \
        -append "${SERENITY_KERNEL_CMDLINE}"
else
    # Meta/run.sh: qemu with user networking
    "$SERENITY_QEMU_BIN" \
        $SERENITY_COMMON_QEMU_ARGS \
        $SERENITY_VIRT_TECH_ARG \
        $SERENITY_PACKET_LOGGING_ARG \
        -netdev user,id=breh,hostfwd=tcp:127.0.0.1:8888-10.0.2.15:8888,hostfwd=tcp:127.0.0.1:8823-10.0.2.15:23,hostfwd=tcp:127.0.0.1:8000-10.0.2.15:8000,hostfwd=tcp:127.0.0.1:2222-10.0.2.15:22 \
        -device $SERENITY_ETHERNET_DEVICE_TYPE,netdev=breh \
        -kernel Kernel/Prekernel/Prekernel \
        -initrd Kernel/Kernel \
        -append "${SERENITY_KERNEL_CMDLINE}"
fi
