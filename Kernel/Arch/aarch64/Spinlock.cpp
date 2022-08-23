/*
 * Copyright (c) 2022, Timon Kruiper <timonkruiper@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/Spinlock.h>

// FIXME: Actually implement the correct logic once the aarch64 build can
//        do interrupts and/or has support for multiple processors.

namespace Kernel {

InterruptsState Spinlock::lock()
{
    return InterruptsState::Disabled;
}

void Spinlock::unlock(InterruptsState)
{
}

InterruptsState RecursiveSpinlock::lock()
{
    return InterruptsState::Disabled;
}

void RecursiveSpinlock::unlock(InterruptsState)
{
}

}
