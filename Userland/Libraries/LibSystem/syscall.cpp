/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/API/Syscall.h>
#include <LibSystem/syscall.h>

extern "C" {

uintptr_t syscall0(uintptr_t function)
{
    return Syscall::invoke((Syscall::Function)function);
}

uintptr_t syscall1(uintptr_t function, uintptr_t arg0)
{
    return Syscall::invoke((Syscall::Function)function, arg0);
}

uintptr_t syscall2(uintptr_t function, uintptr_t arg0, uintptr_t arg1)
{
    return Syscall::invoke((Syscall::Function)function, arg0, arg1);
}

uintptr_t syscall3(uintptr_t function, uintptr_t arg0, uintptr_t arg1, uintptr_t arg2)
{
    return Syscall::invoke((Syscall::Function)function, arg0, arg1, arg2);
}

uintptr_t syscall4(uintptr_t function, uintptr_t arg0, uintptr_t arg1, uintptr_t arg2, uintptr_t arg3)
{
    return Syscall::invoke((Syscall::Function)function, arg0, arg1, arg2, arg3);
}
}
