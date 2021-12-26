/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <sys/cdefs.h>

__BEGIN_DECLS

#ifdef DEBUG
[[noreturn]] void __assertion_failed(const char* msg);
#    define __stringify_helper(x) #    x
#    define __stringify(x) __stringify_helper(x)
#    define assert(expr)                                                           \
        do {                                                                       \
            if (__builtin_expect(!(expr), 0))                                      \
                __assertion_failed(#expr "\n" __FILE__ ":" __stringify(__LINE__)); \
        } while (0)
#else
#    define assert(expr) ((void)(0))
#    define VERIFY_NOT_REACHED() _abort()
#endif

[[noreturn]] void _abort();

#ifndef __cplusplus
#    define static_assert _Static_assert
#endif

__END_DECLS
