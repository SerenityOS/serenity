/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#ifndef USING_AK_GLOBALLY
#    define USING_AK_GLOBALLY 1
#endif

#ifdef __i386__
#    define AK_ARCH_I386 1
#endif

#ifdef __x86_64__
#    define AK_ARCH_X86_64 1
#endif

#ifdef __aarch64__
#    define AK_ARCH_AARCH64 1
#endif

#ifdef __wasm32__
#    define AK_ARCH_WASM32 1
#endif

#if (defined(__SIZEOF_POINTER__) && __SIZEOF_POINTER__ == 8) || defined(_WIN64)
#    define AK_ARCH_64_BIT
#else
#    define AK_ARCH_32_BIT
#endif

#if defined(__clang__)
#    define AK_COMPILER_CLANG
#elif defined(__GNUC__)
#    define AK_COMPILER_GCC
#endif

#if defined(__serenity__)
#    define AK_OS_SERENITY
#endif

#if defined(__linux__)
#    define AK_OS_LINUX
#endif

#if defined(__APPLE__) && defined(__MACH__)
#    define AK_OS_MACOS
#    define AK_OS_BSD_GENERIC
#endif

#if defined(__FreeBSD__)
#    define AK_OS_BSD_GENERIC
#    define AK_OS_FREEBSD
#endif

#if defined(__NetBSD__)
#    define AK_OS_BSD_GENERIC
#    define AK_OS_NETBSD
#endif

#if defined(__OpenBSD__)
#    define AK_OS_BSD_GENERIC
#    define AK_OS_OPENBSD
#endif

#if defined(__DragonFly__)
#    define AK_OS_BSD_GENERIC
#    define AK_OS_DRAGONFLY
#endif

#if defined(_WIN32) || defined(_WIN64)
#    define AK_OS_WINDOWS
#endif

#if defined(__ANDROID__)
#    define STR(x) __STR(x)
#    define __STR(x) #x
#    if __ANDROID_API__ < 30
#        pragma message "Invalid android API " STR(__ANDROID_API__)
#        error "Build configuration not tested on configured Android API version"
#    endif
#    undef STR
#    undef __STR
#    define AK_OS_ANDROID
#endif

#if defined(__EMSCRIPTEN__)
#    define AK_OS_EMSCRIPTEN
#endif

#define ARCH(arch) (defined(AK_ARCH_##arch) && AK_ARCH_##arch)

#if ARCH(I386) || ARCH(X86_64)
#    define VALIDATE_IS_X86()
#else
#    define VALIDATE_IS_X86() static_assert(false, "Trying to include x86 only header on non x86 platform");
#endif

#if ARCH(AARCH64)
#    define VALIDATE_IS_AARCH64()
#else
#    define VALIDATE_IS_AARCH64() static_assert(false, "Trying to include aarch64 only header on non aarch64 platform");
#endif

#if !defined(AK_COMPILER_CLANG) && !defined(__CLION_IDE_) && !defined(__CLION_IDE__)
#    define AK_HAS_CONDITIONALLY_TRIVIAL
#endif

#ifdef ALWAYS_INLINE
#    undef ALWAYS_INLINE
#endif
#define ALWAYS_INLINE __attribute__((always_inline)) inline

#ifdef NEVER_INLINE
#    undef NEVER_INLINE
#endif
#define NEVER_INLINE __attribute__((noinline))

#ifdef FLATTEN
#    undef FLATTEN
#endif
#define FLATTEN __attribute__((flatten))

#ifdef RETURNS_NONNULL
#    undef RETURNS_NONNULL
#endif
#define RETURNS_NONNULL __attribute__((returns_nonnull))

#ifdef NO_SANITIZE_ADDRESS
#    undef NO_SANITIZE_ADDRESS
#endif
#define NO_SANITIZE_ADDRESS __attribute__((no_sanitize_address))

#ifdef NAKED
#    undef NAKED
#endif
#ifndef AK_ARCH_AARCH64
#    define NAKED __attribute__((naked))
#else
#    define NAKED
#endif

#ifdef DISALLOW
#    undef DISALLOW
#endif
#if defined(AK_COMPILER_CLANG)
#    define DISALLOW(message) __attribute__((diagnose_if(1, message, "error")))
#else
#    define DISALLOW(message) __attribute__((error(message)))
#endif

// GCC doesn't have __has_feature but clang does
#ifndef __has_feature
#    define __has_feature(...) 0
#endif

#if __has_feature(address_sanitizer) || defined(__SANITIZE_ADDRESS__)
#    define HAS_ADDRESS_SANITIZER
#    define ASAN_POISON_MEMORY_REGION(addr, size) __asan_poison_memory_region(addr, size)
#    define ASAN_UNPOISON_MEMORY_REGION(addr, size) __asan_unpoison_memory_region(addr, size)
#else
#    define ASAN_POISON_MEMORY_REGION(addr, size)
#    define ASAN_UNPOISON_MEMORY_REGION(addr, size)
#endif

#ifndef AK_OS_SERENITY
// On macOS (at least Mojave), Apple's version of this header is not wrapped
// in extern "C".
#    ifdef AK_OS_MACOS
extern "C" {
#    endif
#    include <unistd.h>
#    undef PAGE_SIZE
#    define PAGE_SIZE sysconf(_SC_PAGESIZE)
#    ifdef AK_OS_MACOS
};
#    endif
#endif

#if defined(AK_OS_WINDOWS)
#    define CLOCK_MONOTONIC_COARSE CLOCK_MONOTONIC
#endif

#if defined(AK_OS_BSD_GENERIC) && !defined(AK_OS_FREEBSD)
#    define CLOCK_MONOTONIC_COARSE CLOCK_MONOTONIC
#    define CLOCK_REALTIME_COARSE CLOCK_REALTIME
#endif

#ifndef AK_SYSTEM_CACHE_ALIGNMENT_SIZE
#    if ARCH(AARCH64) || ARCH(x86_64)
#        define AK_SYSTEM_CACHE_ALIGNMENT_SIZE 64
#    else
#        define AK_SYSTEM_CACHE_ALIGNMENT_SIZE 128
#    endif
#endif /* AK_SYSTEM_CACHE_ALIGNMENT_SIZE */

#ifdef AK_CACHE_ALIGNED
#    undef AK_CACHE_ALIGNED
#endif
#define AK_CACHE_ALIGNED alignas(AK_SYSTEM_CACHE_ALIGNMENT_SIZE)
