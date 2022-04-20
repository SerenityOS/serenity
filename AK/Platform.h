/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#ifdef __i386__
#    define AK_ARCH_I386 1
#endif

#ifdef __x86_64__
#    define AK_ARCH_X86_64 1
#endif

#ifdef __aarch64__
#    define AK_ARCH_AARCH64 1
#endif

#if defined(__APPLE__) && defined(__MACH__)
#    define AK_OS_MACOS
#    define AK_OS_BSD_GENERIC
#endif

#if defined(__FreeBSD__) || defined(__NetBSD__) || defined(__OpenBSD__) || defined(__DragonFly__)
#    define AK_OS_BSD_GENERIC
#endif

#define ARCH(arch) (defined(AK_ARCH_##arch) && AK_ARCH_##arch)

#if ARCH(I386) || ARCH(X86_64)
#    define VALIDATE_IS_X86()
#else
#    define VALIDATE_IS_X86() static_assert(false, "Trying to include x86 only header on non x86 platform");
#endif

#if !defined(__clang__) && !defined(__CLION_IDE_)
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
#define NAKED __attribute__((naked))

#ifdef DISALLOW
#    undef DISALLOW
#endif
#ifdef __clang__
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

#ifndef __serenity__
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

#ifdef AK_OS_BSD_GENERIC
#    define CLOCK_MONOTONIC_COARSE CLOCK_MONOTONIC
#    define CLOCK_REALTIME_COARSE CLOCK_REALTIME
#endif

#ifndef SYSTEM_CACHE_ALIGNMENT_SIZE
#    if ARCH(AARCH64) || ARCH(x86_64)
#        define SYSTEM_CACHE_ALIGNMENT_SIZE 64
#    else
#        define SYSTEM_CACHE_ALIGNMENT_SIZE 128
#    endif
#endif /* SYSTEM_CACHE_ALIGNMENT_SIZE */

#ifdef CACHE_ALIGNED
#    undef CACHE_ALIGNED
#endif
#define CACHE_ALIGNED alignas(SYSTEM_CACHE_ALIGNMENT_SIZE)
