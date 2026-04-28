# Patches for llvm on SerenityOS

## `0001-llvm-Add-support-for-building-LLVM-on-SerenityOS.patch`

Add support for building LLVM on SerenityOS

Adds SerenityOS `#ifdef`s for platform-specific code.

We stub out wait4, as SerenityOS doesn't support querying a child
process's resource usage information.

POSIX shm is not supported by SerenityOS yet, so disable it in Orc.

Serenity gives each thread a default of 1MiB of stack. Increase the
default stack size for llvm applications when running on SerenityOS.


## `0002-tools-Support-building-shared-libLLVM-and-libClang-f.patch`

Support building shared libLLVM and libClang for SerenityOS

This patch tells CMake that the --whole-archive linker option should be
used for specifying the archives whose members will constitute these
shared libraries.

Symbol versioning is disabled, as the SerenityOS loader doesn't support
it, and the ELF sections that store version data would just waste space.

## `0003-compiler-rt-Enable-profile-instrumentation-for-Seren.patch`

Enable profile instrumentation for SerenityOS

Treat SerenityOS the same as other *NIX platforms that behave close
enough to linux to use the pre-canned InstrProfiling implementation.

## `0004-libcxx-Add-support-for-SerenityOS.patch`

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

## `0005-RISCV-Implement-__init_riscv_feature_bits-for-Sereni.patch`

Implement __init_riscv_feature_bits for SerenityOS

The SerenityOS dynamic linker provides a magic function
"__get_riscv_feature_bits" that populates __riscv_feature_bits
and __riscv_cpu_model.

## `0006-libcxxabi-Define-__cxa_thread_atexit-on-serenity.patch`

Define __cxa_thread_atexit on serenity


