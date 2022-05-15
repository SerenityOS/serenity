/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/BitCast.h>
#include <Kernel/API/Syscall.h>
#include <sys/types.h>

extern "C" {

uintptr_t syscall0(uintptr_t function);
uintptr_t syscall1(uintptr_t function, uintptr_t arg0);
uintptr_t syscall2(uintptr_t function, uintptr_t arg0, uintptr_t arg1);
uintptr_t syscall3(uintptr_t function, uintptr_t arg0, uintptr_t arg1, uintptr_t arg2);
uintptr_t syscall4(uintptr_t function, uintptr_t arg0, uintptr_t arg1, uintptr_t arg2, uintptr_t arg3);
}

#ifdef __cplusplus

// To get rid of C-style casts, we need to conditionally static_cast or reinterpret_cast the syscall arguments.
#    define CAST_ARGUMENT(argument)                                                  \
        uintptr_t argument##_ptr;                                                    \
        if constexpr (requires(decltype(argument) a) { static_cast<uintptr_t>(a); }) \
            argument##_ptr = static_cast<uintptr_t>(argument);                       \
        else                                                                         \
            argument##_ptr = reinterpret_cast<uintptr_t>(argument)

inline uintptr_t syscall(auto function)
{
    return syscall0(function);
}

inline uintptr_t syscall(auto function, auto arg0)
{
    CAST_ARGUMENT(arg0);

    return syscall1(static_cast<uintptr_t>(function), arg0_ptr);
}

inline uintptr_t syscall(auto function, auto arg0, auto arg1)
{
    CAST_ARGUMENT(arg0);
    CAST_ARGUMENT(arg1);

    return syscall2(static_cast<uintptr_t>(function), arg0_ptr, arg1_ptr);
}

inline uintptr_t syscall(auto function, auto arg0, auto arg1, auto arg2)
{
    CAST_ARGUMENT(arg0);
    CAST_ARGUMENT(arg1);
    CAST_ARGUMENT(arg2);

    return syscall3(static_cast<uintptr_t>(function), arg0_ptr, arg1_ptr, arg2_ptr);
}

inline uintptr_t syscall(auto function, auto arg0, auto arg1, auto arg2, auto arg3)
{
    CAST_ARGUMENT(arg0);
    CAST_ARGUMENT(arg1);
    CAST_ARGUMENT(arg2);
    CAST_ARGUMENT(arg3);

    return syscall4(static_cast<uintptr_t>(function), arg0_ptr, arg1_ptr, arg2_ptr, arg3_ptr);
}

#    undef CAST_ARGUMENT

#endif
