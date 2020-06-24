#!/bin/sh
# shellcheck disable=SC2086 # FIXME: fix these globing warnings

set -e

#SERENITY_PACKET_LOGGING_ARG="-object filter-dump,id=hue,netdev=breh,file=e1000.pcap"

[ -e /dev/kvm ] && [ -r /dev/kvm ] && [ -w /dev/kvm ] && SERENITY_KVM_ARG="-enable-kvm"

[ -z "$SERENITY_BOCHS_BIN" ] && SERENITY_BOCHS_BIN="bochs"

[ -z "$SERENITY_QEMU_BIN" ] && SERENITY_QEMU_BIN="qemu-system-i386"

[ -z "$SERENITY_KERNEL_CMDLINE" ] && SERENITY_KERNEL_CMDLINE="hello"

[ -z "$SERENITY_RAM_SIZE" ] && SERENITY_RAM_SIZE=256M

[ -z "$SERENITY_COMMON_QEMU_ARGS" ] && SERENITY_COMMON_QEMU_ARGS="
$SERENITY_EXTRA_QEMU_ARGS
-s -m $SERENITY_RAM_SIZE
-cpu max
-d cpu_reset,guest_errors
-smp 2
-device VGA,vgamem_mb=64
-drive file=_disk_image,format=raw,index=0,media=disk
-device ich9-ahci
-debugcon stdio
-soundhw pcspk
-soundhw sb16
"

[ -z "$SERENITY_COMMON_QEMU_Q35_ARGS" ] && SERENITY_COMMON_QEMU_Q35_ARGS="
$SERENITY_EXTRA_QEMU_ARGS
-s -m $SERENITY_RAM_SIZE
-cpu max
-machine q35
-d cpu_reset,guest_errors
-smp 2
-device VGA,vgamem_mb=64
-device piix3-ide
-drive file=_disk_image,id=disk,if=none
-device ide-hd,bus=ide.6,drive=disk,unit=0
-debugcon stdio
-soundhw pcspk
-soundhw sb16
"

export SDL_VIDEO_X11_DGAMOUSE=0

if [ "$1" = "b" ]; then
    # ./run b: bochs
    "$SERENITY_BOCHS_BIN" -q -f .bochsrc
elif [ "$1" = "qn" ]; then
    # ./run qn: qemu without network
    "$SERENITY_QEMU_BIN" \
        $SERENITY_COMMON_QEMU_ARGS \
        -device e1000 \
        -kernel Kernel/Kernel \
        -append "${SERENITY_KERNEL_CMDLINE}"
elif [ "$1" = "qtap" ]; then
    # ./run qtap: qemu with tap
    sudo "$SERENITY_QEMU_BIN" \
        $SERENITY_COMMON_QEMU_ARGS \
        $SERENITY_KVM_ARG \
        $SERENITY_PACKET_LOGGING_ARG \
        -netdev tap,ifname=tap0,id=br0 \
        -device e1000,netdev=br0 \
        -kernel Kernel/Kernel \
        -append "${SERENITY_KERNEL_CMDLINE}"
elif [ "$1" = "qgrub" ]; then
    # ./run qgrub: qemu with grub
    "$SERENITY_QEMU_BIN" \
        $SERENITY_COMMON_QEMU_ARGS \
        $SERENITY_KVM_ARG \
        $SERENITY_PACKET_LOGGING_ARG \
        -netdev user,id=breh,hostfwd=tcp:127.0.0.1:8888-10.0.2.15:8888,hostfwd=tcp:127.0.0.1:8823-10.0.2.15:23 \
        -device e1000,netdev=breh
elif [ "$1" = "q35_cmd" ]; then
    shift
    SERENITY_KERNEL_CMDLINE="$*"
    echo "Starting SerenityOS, Commandline: ${SERENITY_KERNEL_CMDLINE}"
    # ./run: qemu with SerenityOS with custom commandline
    "$SERENITY_QEMU_BIN" \
        $SERENITY_COMMON_QEMU_Q35_ARGS \
        $SERENITY_KVM_ARG \
        -netdev user,id=breh,hostfwd=tcp:127.0.0.1:8888-10.0.2.15:8888,hostfwd=tcp:127.0.0.1:8823-10.0.2.15:23 \
        -device e1000,netdev=breh \
        -kernel Kernel/Kernel \
        -append "${SERENITY_KERNEL_CMDLINE}"
elif [ "$1" = "qcmd" ]; then
    shift
    SERENITY_KERNEL_CMDLINE="$*"
    echo "Starting SerenityOS, Commandline: ${SERENITY_KERNEL_CMDLINE}"
    # ./run: qemu with SerenityOS with custom commandline
    "$SERENITY_QEMU_BIN" \
        $SERENITY_COMMON_QEMU_ARGS \
        $SERENITY_KVM_ARG \
        -netdev user,id=breh,hostfwd=tcp:127.0.0.1:8888-10.0.2.15:8888,hostfwd=tcp:127.0.0.1:8823-10.0.2.15:23 \
        -device e1000,netdev=breh \
        -kernel Kernel/Kernel \
        -append "${SERENITY_KERNEL_CMDLINE}"
else
    # ./run: qemu with user networking
    "$SERENITY_QEMU_BIN" \
        $SERENITY_COMMON_QEMU_ARGS \
        $SERENITY_KVM_ARG \
        $SERENITY_PACKET_LOGGING_ARG \
        -netdev user,id=breh,hostfwd=tcp:127.0.0.1:8888-10.0.2.15:8888,hostfwd=tcp:127.0.0.1:8823-10.0.2.15:23,hostfwd=tcp:127.0.0.1:8000-10.0.2.15:8000,hostfwd=tcp:127.0.0.1:2222-10.0.2.15:22 \
        -device e1000,netdev=breh \
        -kernel Kernel/Kernel \
        -append "${SERENITY_KERNEL_CMDLINE}"
fi
