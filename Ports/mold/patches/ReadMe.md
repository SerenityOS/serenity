# Patches for mold on SerenityOS

## `0001-Tell-TBB-that-SerenityOS-does-not-support-weak-symbo.patch`

Tell TBB that SerenityOS does not support weak symbols

Something about the Clang toolchain configuration causes undefined weak
references to scalable_malloc to remain in the mold executable even
though there's no chance we'll be loading the tbbmalloc library at
runtime. So, just lie to TBB that we don't support weak symbols.

## `0002-Tell-TBB-that-SerenityOS-libraries-are-named-like-BS.patch`

Tell TBB that SerenityOS libraries are named like BSD ones

We won't be loading these libraries when building TBB as a static
library for mold, but the OS detection logic still needs updated.

## `0003-Stub-out-a-definition-of-RTLD_NOLOAD.patch`

Stub out a definition of RTLD_NOLOAD

SerenityOS's DynamicLoader doesn't support this flag. However, we won't
be dynamically loading any tbb extensions for the static library build
mold uses, so we can just define it as a no-op as the code paths that
use it will never be used.

## `0004-Disable-mold-wrapper.so-on-SerenityOS.patch`

Disable mold-wrapper.so on SerenityOS


## `0005-Replace-lockf-fd-F_LOCK-0-with-flock-fd-LOCK_EX.patch`

Replace lockf(fd, F_LOCK, 0) with flock(fd, LOCK_EX)

We don't have the former implemented.

