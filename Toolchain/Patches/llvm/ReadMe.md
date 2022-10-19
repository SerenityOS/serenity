# Patches for llvm on SerenityOS

## `0001-Support-Add-support-for-building-LLVM-on-SerenityOS.patch`

Add support for building LLVM on SerenityOS

Adds SerenityOS `#ifdef`s for platform-specific code.

We stub out wait4, as SerenityOS doesn't support querying a child
process's resource usage information.

## `0002-Triple-Add-triple-for-SerenityOS.patch`

Add triple for SerenityOS


## `0003-Driver-Add-support-for-SerenityOS.patch`

Add support for SerenityOS

Adds support for the `$arch-pc-serenity` target to the Clang front end.
This makes the compiler look for libraries and headers in the right
places, and enables some security mitigations like stack-smashing
protection and position-independent code by default.

## `0004-Driver-Default-to-ftls-model-initial-exec-on-Serenit.patch`

Default to -ftls-model=initial-exec on SerenityOS

This is a hack to make Clang use the initial-exec TLS model instead of
the default local-exec when building code for Serenity.

This patch should be removed when we implement proper TLS support.

## `0005-libc-Add-support-for-SerenityOS.patch`

Add support for SerenityOS

This commit teaches libc++ about what features are available in our
LibC, namely:
* We do not have locale support, so no-op shims should be used in place
  of the C locale API.
* The number of errno constants defined by us is given by the value of
  the `ELAST` macro.
* Multithreading is implemented though the pthread library.
* Use libc++'s builtin character type table instead of the one provided
  by LibC as there's a lot of extra porting work to convince the rest of
  locale.cpp to use our character type table properly.

## `0006-compiler-rt-Build-crtbegin.o-crtend.o-for-SerenityOS.patch`

Build crtbegin.o/crtend.o for SerenityOS


## `0007-cmake-Allow-undefined-symbols-on-SerenityOS.patch`

Allow undefined symbols on SerenityOS

Allow undefined symbols in LLVM libraries, which is needed because only
stubs are available for SerenityOS libraries when libc++ and libunwind
are built.

## `0008-cmake-Support-building-shared-libLLVM-and-libClang-f.patch`

Support building shared libLLVM and libClang for SerenityOS

This patch tells CMake that the --whole-archive linker option should be
used for specifying the archives whose members will constitute these
shared libraries.

Symbol versioning is disabled, as the SerenityOS loader doesn't support
it, and the ELF sections that store version data would just waste space.

## `0009-compiler-rt-llvm-Enable-profile-instrumentation-for-.patch`

Enable profile instrumentation for SerenityOS

Treat SerenityOS the same as other *NIX platforms that behave close
enough to linux to use the pre-canned InstrProfiling implementation.

Curiously, enabling profiling for the SerenityOS target changes the ELF
OS ABI for userspace binaries to 3, or GNU/Linux.

## `0010-Add-SerenityOS-to-config.guess.patch`

Add SerenityOS to config.guess


## `0011-llvm-Prevent-the-use-of-POSIX-shm-on-SerenityOS.patch`

Prevent the use of POSIX shm on SerenityOS

POSIX shm is not supported by SerenityOS yet, so this causes a
compilation error.

