# Patches for mold on SerenityOS

## `0001-Disable-mold_preload.so-for-Serenity.patch`
This feature depends on RTLD_NEXT capabilities which are not yet
implemented in the Serenity DynamicLoader.

## `0002-Disable-mimalloc-for-serenity.patch`
mimalloc needs some help to compile on serenity. That's one yak too
far for right now.

## `0004-Add-POSIX-headers-for-timeval-and-select.patch`
mold was relying on other libc implementations leaking these definitions
from other headers.

## `0005-Tell-TBB-that-SerenityOS-does-not-support-weak-symbo.patch`
Something about the Clang toolchain configuration causes undefined weak
references to scalable_malloc to remain in the mold executable even
though there's no chance we'll be loading the tbbmalloc library at
runtime. So, just lie to TBB that we don't support weak symbols.

## `0006-Tell-TBB-that-SerenityOS-libraries-are-named-like-BS.patch`
We won't be loading these libraries when building TBB as a static
library for mold, but the OS detection logic still needs updated.

## `0007-Stub-out-a-definition-of-RTLD_NOLOAD.patch`
SerenityOS's DynamicLoader doesn't support this flag. However, we won't
be dynamically loading any tbb extensions for the static library build
mold uses, so we can just define it as a no-op as the code paths that
use it will never be used.
