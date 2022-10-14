/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

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

inline uintptr_t syscall(auto function)
{
    return syscall0(function);
}

inline uintptr_t syscall(auto function, auto arg0)
{
    return syscall1((uintptr_t)function, (uintptr_t)arg0);
}

inline uintptr_t syscall(auto function, auto arg0, auto arg1)
{
    return syscall2((uintptr_t)function, (uintptr_t)arg0, (uintptr_t)arg1);
}

inline uintptr_t syscall(auto function, auto arg0, auto arg1, auto arg2)
{
    return syscall3((uintptr_t)function, (uintptr_t)arg0, (uintptr_t)arg1, (uintptr_t)arg2);
}

inline uintptr_t syscall(auto function, auto arg0, auto arg1, auto arg2, auto arg3)
{
    return syscall4((uintptr_t)function, (uintptr_t)arg0, (uintptr_t)arg1, (uintptr_t)arg2, (uintptr_t)arg3);
}

#endif
