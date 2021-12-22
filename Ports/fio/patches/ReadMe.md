# Patches for fio 3.29 on SerenityOS

## `0001-fio-remove-non-existent-header-sys-ipc.patch`

Serenity currently doesn't have a <sys/ipc.h> header, so we have to patch the include out.

## `0002-fio-add-serenityos-platform-support.patch`

`fio` abstracts individual operating system support out into to an `os/os-<name>.h` header
where you can select which platform features are available and implement missing function
stubs for our operating system. 

This patch implements basic OS support for Serenity just to get fio up and running.

## `0003-fio-add-serenityos-support-to-configure.patch`

This patch implements targetos detection for serenity, and also disables shared memory
support automatically for serenity, as it's not currently supported.

## `0004-fio-disable-rdtsc-support-for-serenityos.patch`
 
This patch disables the function which uses `rdtsc` to get the current clock time,
as that instruction isn't allowed to be called from user space by serenity.

If you did attempt to call it you would trip a segfault.
