# Patches for rizin on SerenityOS

## `0001-Add-serenity-to-lrt-whitelist.patch`

Add serenity to librt whitelist

This patch adds serenity to the list of operating systems that dont require librt to be explicity linked

## `0002-Disable-PCRE2-JIT-compilation.patch`

Disable PCRE2 JIT compilation


## `0003-Remove-resolve_heap_tcache_implementation-for-serenity.patch`

Remove resolve_heap_tcache implementation

Serenity doesn't implement thread caching in its memory allocator, so tcache resolution is not applicable.

## `0004-Define-rz_sys_pipe-as-pipe2-for-serenity.patch`

Define rz_sys_pipe as pipe2 for serenity


## `0005-Add-serenity-identifier-to-rz_types.h.patch`

Add serenity identifier to rz_types.h

Define serenity as __UNIX__ like, and set RZ_SYS_OS to 'serenity' if __serenity__ identifier is defined

## `0006-Add-zlib-dependency-for-libzip.patch`

Add zlib dependency for libzip


