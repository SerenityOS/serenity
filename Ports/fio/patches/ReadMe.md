# Patches for fio on SerenityOS

## `0001-Remove-non-existent-header-sys-ipc.h.patch`

Remove non existent header sys/ipc.h

Serenity doesn't currently have this header, and
it doesn't appear to be needed on our platform so
remove it for the port.

## `0002-Add-SerenityOS-platform-support.patch`

Add SerenityOS platform support

`fio` abstracts individual operating system support out into to an
`os/os-<name>.h` header where you can select which platform features
are available and implement missing function stubs for our operating
system.

This patch implements basic OS support for Serenity just to get fio up
and running.

## `0003-Add-SerenityOS-support-to-configure.patch`

Add SerenityOS support to configure

This patch implements targetos detection for serenity, and also
disables shared memory support automatically for serenity, as it's not
currently supported.

## `0004-Disable-rdtsc-support-for-serenity.patch`

Disable rdtsc support for serenity

This patch disables the function which uses `rdtsc` to get the current
clock time, as that instruction isn't allowed to be called from user
space by serenity.

If you did attempt to call it you would trip a segfault.

