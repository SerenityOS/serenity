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

#if defined(__APPLE__) && defined(__MACH__)
#    define AK_OS_MACOS
#    define AK_OS_BSD_GENERIC
#endif

#if defined(__FreeBSD__) || defined(__NetBSD__) || defined(__OpenBSD__) || defined(__DragonFly__)
#    define AK_OS_BSD_GENERIC
#endif

#define ARCH(arch) (defined(AK_ARCH_##arch) && AK_ARCH_##arch)

#ifdef ALWAYS_INLINE
#    undef ALWAYS_INLINE
#endif
#define ALWAYS_INLINE [[gnu::always_inline]] inline

#ifdef NEVER_INLINE
#    undef NEVER_INLINE
#endif
#define NEVER_INLINE [[gnu::noinline]]

#ifdef FLATTEN
#    undef FLATTEN
#endif
#define FLATTEN [[gnu::flatten]]

#ifdef NO_SANITIZE_ADDRESS
#    undef NO_SANITIZE_ADDRESS
#endif
#define NO_SANITIZE_ADDRESS [[gnu::no_sanitize_address]]

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
#    include <unistd.h>
#    undef PAGE_SIZE
#    define PAGE_SIZE sysconf(_SC_PAGESIZE)
#endif

ALWAYS_INLINE int count_trailing_zeroes_32(unsigned int val)
{
#if defined(__GNUC__) || defined(__clang__)
    return __builtin_ctz(val);
#else
    for (u8 i = 0; i < 32; ++i) {
        if ((val >> i) & 1) {
            return i;
        }
    }
    return 0;
#endif
}

ALWAYS_INLINE int count_trailing_zeroes_32_safe(unsigned int val)
{
    if (val == 0)
        return 32;
    return count_trailing_zeroes_32(val);
}

#ifdef AK_OS_BSD_GENERIC
#    define CLOCK_MONOTONIC_COARSE CLOCK_MONOTONIC
#    define CLOCK_REALTIME_COARSE CLOCK_REALTIME
#endif
