# Patches for fio on SerenityOS

## `0001-fio-remove-non-existent-header-sys-ipc.patch`

Port: fio, remove non existent header sys/ipc.h

Serenity doesn't currently have this header, and
it doesn't appear to be needed on our platform so
remove it for the port.

## `0002-fio-add-serenityos-platform-support.patch`

Port: fio - Add SerenityOS platform support

`fio` abstracts individual operating system support out into to an
`os/os-<name>.h` header where you can select which platform features
are available and implement missing function stubs for our operating
system.

This patch implements basic OS support for Serenity just to get fio up
and running.

## `0003-fio-add-serenityos-support-to-configure.patch`

Port: Add SerenityOS support to configure

This patch implements targetos detection for serenity, and also
disables shared memory support automatically for serenity, as it's not
currently supported.

## `0004-fio-disable-rdtsc-support-for-serenityos.patch`

Port: fio - Disable rdtsc support for serenity

This patch disables the function which uses `rdtsc` to get the current
clock time, as that instruction isn't allowed to be called from user
space by serenity.

If you did attempt to call it you would trip a segfault.

