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

#include <sys/cdefs.h>

__BEGIN_DECLS

#ifdef DEBUG
__attribute__((noreturn)) void __assertion_failed(const char* msg);
#    define __stringify_helper(x) #    x
#    define __stringify(x) __stringify_helper(x)
#    define assert(expr)                                                           \
        do {                                                                       \
            if (__builtin_expect(!(expr), 0))                                      \
                __assertion_failed(#expr "\n" __FILE__ ":" __stringify(__LINE__)); \
        } while (0)
#    define ASSERT_NOT_REACHED() assert(false)
#else
#    define assert(expr) ((void)0)
#    define ASSERT_NOT_REACHED() CRASH()
#endif

#define CRASH()              \
    do {                     \
        asm volatile("ud2"); \
    } while (0)
#define ASSERT assert
#define RELEASE_ASSERT assert
#define TODO ASSERT_NOT_REACHED

__END_DECLS
