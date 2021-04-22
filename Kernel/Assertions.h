/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#define __STRINGIFY_HELPER(x) #x
#define __STRINGIFY(x) __STRINGIFY_HELPER(x)

#ifdef DEBUG
[[noreturn]] void __assertion_failed(const char* msg, const char* file, unsigned line, const char* func);
#    define VERIFY(expr) (static_cast<bool>(expr) ? void(0) : __assertion_failed(#    expr, __FILE__, __LINE__, __PRETTY_FUNCTION__))
#    define VERIFY_NOT_REACHED() VERIFY(false)
#else
#    define VERIFY(expr)
#    define VERIFY_NOT_REACHED() _abort()
#endif

extern "C" {
[[noreturn]] void _abort();
[[noreturn]] void abort();
}

#define VERIFY_INTERRUPTS_DISABLED() VERIFY(!(cpu_flags() & 0x200))
#define VERIFY_INTERRUPTS_ENABLED() VERIFY(cpu_flags() & 0x200)
#define TODO VERIFY_NOT_REACHED
