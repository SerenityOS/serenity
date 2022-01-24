# Patches for mold on SerenityOS

## `0001-Disable-mold-wrapper.so-for-Serenity.patch`

Disable mold-wrapper.so for Serenity

This feature depends on RTLD_NEXT capabilities which are not yet
implemented in the Serenity DynamicLoader.

## `0002-Disable-mimalloc-for-serenity.patch`

Disable mimalloc for serenity

mimalloc needs some help to compile and run on serenity.
That's one yak too far for right now.

## `0003-Tell-TBB-that-SerenityOS-does-not-support-weak-symbo.patch`

Tell TBB that SerenityOS does not support weak symbols

Something about the Clang toolchain configuration causes undefined weak
references to scalable_malloc to remain in the mold executable even
though there's no chance we'll be loading the tbbmalloc library at
runtime. So, just lie to TBB that we don't support weak symbols.

## `0004-Tell-TBB-that-SerenityOS-libraries-are-named-like-BS.patch`

Tell TBB that SerenityOS libraries are named like BSD ones

We won't be loading these libraries when building TBB as a static
library for mold, but the OS detection logic still needs updated.

## `0005-Stub-out-a-definition-of-RTLD_NOLOAD.patch`

Stub out a definition of RTLD_NOLOAD

SerenityOS's DynamicLoader doesn't support this flag. However, we won't
be dynamically loading any tbb extensions for the static library build
mold uses, so we can just define it as a no-op as the code paths that
use it will never be used.

## `0006-Disable-__TBB_RESUMABLE_TASKS-for-serenity.patch`

Disable __TBB_RESUMABLE_TASKS for serenity

This feature requires ``<ucontext.h>``, which is not currently
implemented for any supported SerenityOS targets

