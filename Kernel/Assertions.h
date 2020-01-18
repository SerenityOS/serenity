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

#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/kstdio.h>

#ifdef DEBUG
[[noreturn]] void __assertion_failed(const char* msg, const char* file, unsigned line, const char* func);
#    define ASSERT(expr) (static_cast<bool>(expr) ? (void)0 : __assertion_failed(#    expr, __FILE__, __LINE__, __PRETTY_FUNCTION__))
#    define ASSERT_NOT_REACHED() ASSERT(false)
#else
#    define ASSERT(expr)
#    define ASSERT_NOT_REACHED() CRASH()
#endif
#define CRASH()              \
    do {                     \
        asm volatile("ud2"); \
    } while (0)
#define RELEASE_ASSERT(x) \
    do {                  \
        if (!(x))         \
            CRASH();      \
    } while (0)
#define ASSERT_INTERRUPTS_DISABLED() ASSERT(!(cpu_flags() & 0x200))
#define ASSERT_INTERRUPTS_ENABLED() ASSERT(cpu_flags() & 0x200)
