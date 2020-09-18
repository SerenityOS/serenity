#!/bin/sh
# shellcheck disable=SC2086 # FIXME: fix these globing warnings

set -e

die() {
    echo "die: $*"
    exit 1
}

#SERENITY_PACKET_LOGGING_ARG="-object filter-dump,id=hue,netdev=breh,file=e1000.pcap"

[ -e /dev/kvm ] && [ -r /dev/kvm ] && [ -w /dev/kvm ] && SERENITY_KVM_ARG="-enable-kvm"

[ -z "$SERENITY_BOCHS_BIN" ] && SERENITY_BOCHS_BIN="bochs"

[ -z "$SERENITY_QEMU_BIN" ] && SERENITY_QEMU_BIN="qemu-system-i386"

[ -z "$SERENITY_KERNEL_CMDLINE" ] && SERENITY_KERNEL_CMDLINE="hello"

[ -z "$SERENITY_RAM_SIZE" ] && SERENITY_RAM_SIZE=256M

[ -z "$SERENITY_QEMU_CPU" ] && SERENITY_QEMU_CPU="max"

[ -z "$SERENITY_DISK_IMAGE" ] && {
    if [ "$1" = qgrub ]; then
        SERENITY_DISK_IMAGE="grub_disk_image"
    else
        SERENITY_DISK_IMAGE="_disk_image"
    fi
}

[ -z "$SERENITY_COMMON_QEMU_ARGS" ] && SERENITY_COMMON_QEMU_ARGS="
$SERENITY_EXTRA_QEMU_ARGS
-s -m $SERENITY_RAM_SIZE
-cpu $SERENITY_QEMU_CPU
-d guest_errors
-smp 2
-device VGA,vgamem_mb=64
-drive file=${SERENITY_DISK_IMAGE},format=raw,index=0,media=disk
-device ich9-ahci
-usb
-debugcon stdio
-soundhw pcspk
-device sb16
"

[ -z "$SERENITY_COMMON_QEMU_Q35_ARGS" ] && SERENITY_COMMON_QEMU_Q35_ARGS="
$SERENITY_EXTRA_QEMU_ARGS
-s -m $SERENITY_RAM_SIZE
-cpu $SERENITY_QEMU_CPU
-machine q35
-d guest_errors
-smp 2
-device VGA,vgamem_mb=64
-device piix3-ide
-drive file=${SERENITY_DISK_IMAGE},id=disk,if=none
-device ide-hd,bus=ide.6,drive=disk,unit=0
-usb
-debugcon stdio
-soundhw pcspk
-device sb16
"

export SDL_VIDEO_X11_DGAMOUSE=0

: "${SERENITY_BUILD:=.}"
cd -P -- "$SERENITY_BUILD" || die "Could not cd to \"$SERENITY_BUILD\""

SERENITY_RUN="${SERENITY_RUN:-$1}"

if [ "$SERENITY_RUN" = "b" ]; then
    # Meta/run.sh b: bochs
    [ -z "$SERENITY_BOCHSRC" ] && {
        # Make sure that SERENITY_ROOT is set and not empty
        [ -z "$SERENITY_ROOT" ] && die 'SERENITY_ROOT not set or empty'
        SERENITY_BOCHSRC="$SERENITY_ROOT/Meta/bochsrc"
    }
    "$SERENITY_BOCHS_BIN" -q -f "$SERENITY_BOCHSRC"
elif [ "$SERENITY_RUN" = "qn" ]; then
    # Meta/run.sh qn: qemu without network
    "$SERENITY_QEMU_BIN" \
        $SERENITY_COMMON_QEMU_ARGS \
        -device e1000 \
        -kernel Kernel/Kernel \
        -append "${SERENITY_KERNEL_CMDLINE}"
elif [ "$SERENITY_RUN" = "qtap" ]; then
    # Meta/run.sh qtap: qemu with tap
    sudo "$SERENITY_QEMU_BIN" \
        $SERENITY_COMMON_QEMU_ARGS \
        $SERENITY_KVM_ARG \
        $SERENITY_PACKET_LOGGING_ARG \
        -netdev tap,ifname=tap0,id=br0 \
        -device e1000,netdev=br0 \
        -kernel Kernel/Kernel \
        -append "${SERENITY_KERNEL_CMDLINE}"
elif [ "$SERENITY_RUN" = "qgrub" ]; then
    # Meta/run.sh qgrub: qemu with grub
    "$SERENITY_QEMU_BIN" \
        $SERENITY_COMMON_QEMU_ARGS \
        $SERENITY_KVM_ARG \
        $SERENITY_PACKET_LOGGING_ARG \
        -netdev user,id=breh,hostfwd=tcp:127.0.0.1:8888-10.0.2.15:8888,hostfwd=tcp:127.0.0.1:8823-10.0.2.15:23 \
        -device e1000,netdev=breh
elif [ "$SERENITY_RUN" = "q35_cmd" ]; then
    # Meta/run.sh q35_cmd: qemu (q35 chipset) with SerenityOS with custom commandline
    shift
    SERENITY_KERNEL_CMDLINE="$*"
    echo "Starting SerenityOS, Commandline: ${SERENITY_KERNEL_CMDLINE}"
    "$SERENITY_QEMU_BIN" \
        $SERENITY_COMMON_QEMU_Q35_ARGS \
        $SERENITY_KVM_ARG \
        -netdev user,id=breh,hostfwd=tcp:127.0.0.1:8888-10.0.2.15:8888,hostfwd=tcp:127.0.0.1:8823-10.0.2.15:23 \
        -device e1000,netdev=breh \
        -kernel Kernel/Kernel \
        -append "${SERENITY_KERNEL_CMDLINE}"
elif [ "$SERENITY_RUN" = "qcmd" ]; then
    # Meta/run.sh qcmd: qemu with SerenityOS with custom commandline
    shift
    SERENITY_KERNEL_CMDLINE="$*"
    echo "Starting SerenityOS, Commandline: ${SERENITY_KERNEL_CMDLINE}"
    "$SERENITY_QEMU_BIN" \
        $SERENITY_COMMON_QEMU_ARGS \
        $SERENITY_KVM_ARG \
        -netdev user,id=breh,hostfwd=tcp:127.0.0.1:8888-10.0.2.15:8888,hostfwd=tcp:127.0.0.1:8823-10.0.2.15:23 \
        -device e1000,netdev=breh \
        -kernel Kernel/Kernel \
        -append "${SERENITY_KERNEL_CMDLINE}"
else
    # Meta/run.sh: qemu with user networking
    "$SERENITY_QEMU_BIN" \
        $SERENITY_COMMON_QEMU_ARGS \
        $SERENITY_KVM_ARG \
        $SERENITY_PACKET_LOGGING_ARG \
        -netdev user,id=breh,hostfwd=tcp:127.0.0.1:8888-10.0.2.15:8888,hostfwd=tcp:127.0.0.1:8823-10.0.2.15:23,hostfwd=tcp:127.0.0.1:8000-10.0.2.15:8000,hostfwd=tcp:127.0.0.1:2222-10.0.2.15:22 \
        -device e1000,netdev=breh \
        -kernel Kernel/Kernel \
        -append "${SERENITY_KERNEL_CMDLINE}"
fi
