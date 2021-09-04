/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#define __STRINGIFY_HELPER(x) #x
#define __STRINGIFY(x) __STRINGIFY_HELPER(x)

[[noreturn]] void __assertion_failed(char const* msg, char const* file, unsigned line, char const* func);
#define VERIFY(expr)                                                            \
    do {                                                                        \
        if (!static_cast<bool>(expr)) [[unlikely]]                              \
            __assertion_failed(#expr, __FILE__, __LINE__, __PRETTY_FUNCTION__); \
    } while (0)

#define VERIFY_NOT_REACHED() VERIFY(false)

extern "C" {
[[noreturn]] void _abort();
[[noreturn]] void abort();
}

#define VERIFY_INTERRUPTS_DISABLED() VERIFY(!(cpu_flags() & 0x200))
#define VERIFY_INTERRUPTS_ENABLED() VERIFY(cpu_flags() & 0x200)

static constexpr bool TODO = false;
#define TODO() VERIFY(TODO)
