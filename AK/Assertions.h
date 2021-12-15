/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

extern "C" {
[[noreturn]] void __assertion_failed(const char* msg, const char* file, unsigned line, const char* func);
}

#define _VERIFY(expr, msg)                                                      \
    do {                                                                        \
        if (!static_cast<bool>(expr)) [[unlikely]]                              \
            __assertion_failed((msg), __FILE__, __LINE__, __PRETTY_FUNCTION__); \
    } while (0)

#define VERIFY(expr) _VERIFY((expr), #expr)
#define VERIFY_NOT_REACHED() _VERIFY(false, "Should not be reachable")
#define TODO() _VERIFY(false, "TODO")
