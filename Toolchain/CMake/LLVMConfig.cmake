# This file specifies the options used for building the Clang compiler, LLD linker and the compiler builtins library

if (CMAKE_VERSION VERSION_EQUAL "3.29.0" OR CMAKE_VERSION VERSION_EQUAL "3.29.1")
    message(FATAL_ERROR "CMake versions 3.29.0 and 3.29.1 are known to not install LLVM correctly. "
                        "Please either downgrade CMake or update it to 3.29.2+.")
endif()

set(CMAKE_BUILD_TYPE Release CACHE STRING "")

set(LLVM_TARGETS_TO_BUILD "X86;AArch64;RISCV" CACHE STRING "")

set(LLVM_ENABLE_PROJECTS "llvm;clang;lld;clang-tools-extra" CACHE STRING "")
set(LLVM_ENABLE_RUNTIMES "compiler-rt;libunwind;libcxxabi;libcxx" CACHE STRING "")

set(LLVM_ENABLE_PER_TARGET_RUNTIME_DIR ON CACHE BOOL "")
set(LLVM_ENABLE_BINDINGS OFF CACHE BOOL "")
set(LLVM_INCLUDE_BENCHMARKS OFF CACHE BOOL "")
set(LLVM_BUILD_UTILS ON CACHE BOOL "")
set(LLVM_INCLUDE_TESTS OFF CACHE BOOL "")
set(LLVM_BUILD_LLVM_DYLIB ON CACHE BOOL "")
set(LLVM_LINK_LLVM_DYLIB ON CACHE BOOL "")
set(LLVM_INSTALL_UTILS ON CACHE BOOL "")
set(LLVM_INSTALL_TOOLCHAIN_ONLY OFF CACHE BOOL "Don't install headers, utils, and tools")
set(LLVM_INSTALL_BINUTILS_SYMLINKS OFF CACHE BOOL "")

if(DEFINED ENV{CLANG_ENABLE_CLANGD} AND "$ENV{CLANG_ENABLE_CLANGD}" STREQUAL "ON")
    message(STATUS "Enabling clangd as a part of toolchain build")
    set(CLANG_ENABLE_CLANGD ON CACHE BOOL "" FORCE)
else()
    message(STATUS "Disabling clangd as a part of toolchain build")
    set(CLANG_ENABLE_CLANGD OFF CACHE BOOL "" FORCE)
endif()

foreach(target x86_64-pc-serenity;aarch64-pc-serenity;riscv64-pc-serenity)
    list(APPEND targets "${target}")

    set(RUNTIMES_${target}_CMAKE_BUILD_TYPE Release CACHE STRING "")
    set(RUNTIMES_${target}_CMAKE_SYSROOT ${SERENITY_${target}_SYSROOT} CACHE PATH "")
    # Prevent configure checks from trying to link to the not-yet-built startup files & libunwind.
    set(compiler_flags "-Wno-unused-command-line-argument -nostartfiles -L${SERENITY_${target}_STUBS}")
    set(RUNTIMES_${target}_CMAKE_C_FLAGS ${compiler_flags} CACHE STRING "")
    set(RUNTIMES_${target}_CMAKE_CXX_FLAGS ${compiler_flags} CACHE STRING "")
    set(RUNTIMES_${target}_COMPILER_RT_BUILD_CRT ON CACHE BOOL "")
    set(RUNTIMES_${target}_COMPILER_RT_BUILD_SANITIZERS OFF CACHE BOOL "")
    set(RUNTIMES_${target}_COMPILER_RT_BUILD_LIBFUZZER OFF CACHE BOOL "")
    set(RUNTIMES_${target}_COMPILER_RT_BUILD_MEMPROF OFF CACHE BOOL "")
    set(RUNTIMES_${target}_COMPILER_RT_BUILD_PROFILE ON CACHE BOOL "")
    set(RUNTIMES_${target}_COMPILER_RT_BUILD_XRAY OFF CACHE BOOL "")
    set(RUNTIMES_${target}_COMPILER_RT_BUILD_ORC OFF CACHE BOOL "")
    set(RUNTIMES_${target}_CMAKE_SYSTEM_NAME SerenityOS CACHE STRING "")
    set(RUNTIMES_${target}_CMAKE_MODULE_PATH ${CMAKE_MODULE_PATH} CACHE STRING "")

    set(BUILTINS_${target}_CMAKE_BUILD_TYPE Release CACHE STRING "")
    set(BUILTINS_${target}_CMAKE_SYSROOT ${SERENITY_${target}_SYSROOT} CACHE PATH "")
    # Explicitly set these so that host CFLAGS/CXXFLAGS don't get passed to the cross compiler.
    set(BUILTINS_${target}_CMAKE_C_FLAGS "" CACHE STRING "")
    set(BUILTINS_${target}_CMAKE_CXX_FLAGS "" CACHE STRING "")
    set(BUILTINS_${target}_COMPILER_RT_EXCLUDE_ATOMIC_BUILTIN OFF CACHE BOOL "")
    set(BUILTINS_${target}_CMAKE_SYSTEM_NAME SerenityOS CACHE STRING "")
    set(BUILTINS_${target}_CMAKE_MODULE_PATH ${CMAKE_MODULE_PATH} CACHE STRING "")

    set(RUNTIMES_${target}_LIBUNWIND_USE_COMPILER_RT ON CACHE BOOL "")
    set(RUNTIMES_${target}_LIBCXXABI_USE_COMPILER_RT ON CACHE BOOL "")
    set(RUNTIMES_${target}_LIBCXX_USE_COMPILER_RT ON CACHE BOOL "")

    set(RUNTIMES_${target}_LIBCXX_ENABLE_STATIC_ABI_LIBRARY ON CACHE BOOL "")
    set(RUNTIMES_${target}_LIBCXX_INCLUDE_BENCHMARKS OFF CACHE BOOL "")

    # Hardcode autodetection results for libm, libdl, and libpthread.
    # This keeps us from accidentially detecting those libraries as being present
    # if we build the toolchain with a populated sysroot (which features the
    # compability linker scripts).
    # TODO: Figure out if we can always build against the Stubs directory instead.
    set(RUNTIMES_${target}_LIBCXXABI_HAS_DL_LIB OFF CACHE BOOL "")
    set(RUNTIMES_${target}_LIBCXXABI_HAS_PTHREAD_LIB OFF CACHE BOOL "")
    set(RUNTIMES_${target}_LIBCXX_HAS_M_LIB OFF CACHE BOOL "")
    set(RUNTIMES_${target}_LIBCXX_HAS_PTHREAD_LIB OFF CACHE BOOL "")
    set(RUNTIMES_${target}_LIBUNWIND_HAS_DL_LIB OFF CACHE BOOL "")
    set(RUNTIMES_${target}_LIBUNWIND_HAS_PTHREAD_LIB OFF CACHE BOOL "")
endforeach()

set(LLVM_TOOLCHAIN_TOOLS
        llvm-addr2line
        llvm-ar
        llvm-config
        llvm-cov
        llvm-cxxfilt
        llvm-dwarfdump
        llvm-ifs
        llvm-lib
        llvm-nm
        llvm-objcopy
        llvm-objdump
        llvm-profdata
        llvm-rc
        llvm-ranlib
        llvm-readelf
        llvm-readobj
        llvm-size
        llvm-strings
        llvm-strip
        llvm-symbolizer
        CACHE STRING "")

set(LLVM_TOOLCHAIN_UTILITIES
        FileCheck
        CACHE STRING ""
)

set(LLVM_RUNTIME_TARGETS ${targets} CACHE STRING "")
set(LLVM_BUILTIN_TARGETS ${targets} CACHE STRING "")
