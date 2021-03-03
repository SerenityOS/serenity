/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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

#ifndef __serenity__
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
