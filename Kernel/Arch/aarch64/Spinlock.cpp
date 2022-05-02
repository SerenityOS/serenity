/*
 * Copyright (c) 2022, Timon Kruiper <timonkruiper@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/Spinlock.h>

namespace Kernel {

u32 Spinlock::lock()
{
    VERIFY_NOT_REACHED();
    return 0;
}

void Spinlock::unlock(u32)
{
    VERIFY_NOT_REACHED();
}

u32 RecursiveSpinlock::lock()
{
    VERIFY_NOT_REACHED();
    return 0;
}

void RecursiveSpinlock::unlock(u32)
{
    VERIFY_NOT_REACHED();
}

}
