# Patches for libuv on SerenityOS

## `0001-fs-Stub-out-unsupported-syscalls.patch`

fs: Stub out unsupported syscalls


## `0002-build-Add-SerenityOS-platform-definitions.patch`

build: Add SerenityOS platform definitions


## `0003-include-Teach-the-header-about-serenity.patch`

include: Teach the header about serenity


## `0004-build-Add-platform-specific-stubs-and-implementation.patch`

build: Add platform-specific stubs and implementations


## `0005-include-Errnos-are-positive-in-serenity.patch`

include: Errnos are positive in serenity

Checking this with `#if EDOM > 0` doesn't work in serenity since errno
values are defined to enumerators.

