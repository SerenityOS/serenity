# Patches for LibUV on SerenityOS

## `0001-unix-Stub-out-get-set-priority-for-serenity.patch`

Serenity does not have `{get,set}priority()`, this stubs them out.

### Status
- [X] Local?
- [ ] Should be merged to upstream?
- [X] Resolves issues(s) with our side of things
- [X] Hack

## `0002-fs-Stub-out-unsupported-syscalls.patch`

Makes libuv use `statvfs` instead of `statfs`.

### Status
- [ ] Local?
- [X] Should be merged to upstream?
- [ ] Resolves issues(s) with our side of things
- [ ] Hack

## `0003-stream-Don-t-use-AF_INET6.patch`

Serenity does not support IPv6, this removes them.

### Status
- [ ] Local?
- [ ] Should be merged to upstream?
- [X] Resolves issues(s) with our side of things
- [ ] Hack

## `0004-tcp-Don-t-use-SO_LINGER.patch`

Serenity does not support `SO_LINGER`, this removes them.

### Status
- [ ] Local?
- [ ] Should be merged to upstream?
- [X] Resolves issues(s) with our side of things
- [X] Hack

## `0005-build-Add-SerenityOS-platform-definitions.patch`

Adds SerenityOS platform definitions to the build.

### Status
- [ ] Local?
- [X] Should be merged to upstream?
- [ ] Resolves issues(s) with our side of things
- [ ] Hack

## `0006-include-Teach-the-header-about-serenity.patch`

Make the header include guards understand that SerenityOS is a thing.

### Status
- [ ] Local?
- [X] Should be merged to upstream?
- [ ] Resolves issues(s) with our side of things
- [ ] Hack

## `0007-build-Add-platform-specific-stubs-and-implementation.patch`

Adds implementations for libuv's primitives specific to SerenityOS.

### Status
- [ ] Local?
- [X] Should be merged to upstream?
- [ ] Resolves issues(s) with our side of things
- [ ] Hack

