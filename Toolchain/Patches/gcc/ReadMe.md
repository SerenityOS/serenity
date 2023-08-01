# Patches for gcc on SerenityOS

## `0001-Add-a-gcc-driver-for-SerenityOS.patch`

Add a gcc driver for SerenityOS

This patch adds support for the `*-*-serenity` target to gcc.

It specifies which flags need to be passed to the linker, defines the
__serenity__ macro, sets the correct underlying type of `size_t` and
`ptrdiff_t`, and enables IFUNCs.


## `0002-fixincludes-Skip-for-SerenityOS-targets.patch`

fixincludes: Skip for SerenityOS targets

`fixincludes` is responsible for fixing mistakes in system headers that
rely in compiler extensions that GCC doesn't support or cause errors in
C++ mode.

Our headers don't have such problems, so this hack is of no use for us.

## `0003-libgcc-Build-for-SerenityOS.patch`

libgcc: Build for SerenityOS

This patch enables building gcc's own C runtime files, and sets up
exception handling support.


## `0004-libgcc-Do-not-link-libgcc_s-to-LibC.patch`

libgcc: Do not link libgcc_s to LibC

The toolchain is built before LibC, so linking to the C runtime library
would fail.


## `0005-i386-Disable-math-errno-for-SerenityOS.patch`

i386: Disable math errno for SerenityOS

SerenityOS uses exceptions for math error handling, which allows the
compiler to do more optimizations on calls to math functions. This patch
has the effect of setting -fno-math-errno by default.

## `0006-libstdc-Support-SerenityOS.patch`

libstdc++: Support SerenityOS

During the toolchain build, SerenityOS libraries are not available, so
we have to manually tell libstdc++ about what our LibC supports.

In most places, we take the Newlib code paths.


## `0007-libstdc-Build-static-library-with-fPIC.patch`

libstdc++: Build static library with -fPIC

We want the libstdc++.a library to contain -fPIC code in order to link
it statically into LibC/our shared objects. However, the build system
forces no-pic/pie instead.

This hack is from https://gcc.gnu.org/bugzilla/show_bug.cgi?id=58638

