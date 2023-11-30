# Patches for qemu on SerenityOS

## `0001-Add-build-system-support-for-SerenityOS.patch`

Add build system support for SerenityOS


## `0002-Extend-short-scan-sets-into-the-full-list.patch`

Extend short scan sets into the full list

We don't support the (apparently nonstandard) short variant of scan
sets, so extend them into a full list manually.

## `0003-Use-the-coarse-monotonic-clock-for-timing-CPU-ticks.patch`

Use the coarse monotonic clock for timing CPU ticks

While this loses quite a bit of accuracy (although to no apparent
decrease in emulation quality), it helps avoiding the additional
overhead of the `clock_gettime` syscall (as `CLOCK_MONOTONIC_COARSE`
is forwarded using the mapped time page) and we don't have to do a
HPET timer read for each tick.

## `0004-file-posix-Include-the-correct-file-for-ioctl-on-Ser.patch`

file-posix: Include the correct file for ioctl on SerenityOS


