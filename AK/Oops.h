/*
 * Copyright (c) 2026, bogdan-glitchm <github.34373732+copyright@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#if defined(KERNEL)
#    include <Kernel/Library/Oops.hpp>
#else
extern "C" __attribute__((noreturn)) void ak_verification_failed(char const*);
#    define __stringify_helper(x) #x
#    define __stringify(x) __stringify_helper(x)
#    define BUG_ON(expr)                                                                  \
        (__builtin_expect(expr, 0)                                                     \
                ? ak_verification_failed("!(" #expr ") at " __FILE__ ":" __stringify(__LINE__)) \
                : (void)0)
#endif
