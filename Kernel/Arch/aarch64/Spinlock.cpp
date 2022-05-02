/*
 * Copyright (c) 2022, Timon Kruiper <timonkruiper@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/Spinlock.h>

// FIXME: Actually implement the correct logic once the aarch64 build can
//        do interrupts and/or has support for multiple processors.

namespace Kernel {

u32 Spinlock::lock()
{
    return 0;
}

void Spinlock::unlock(u32)
{
}

u32 RecursiveSpinlock::lock()
{
    return 0;
}

void RecursiveSpinlock::unlock(u32)
{
}

}
