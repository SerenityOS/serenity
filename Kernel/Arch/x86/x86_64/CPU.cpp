/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/Types.h>

#include <Kernel/Arch/x86/CPU.h>
#include <Kernel/Arch/x86/Processor.h>
#include <Kernel/Arch/x86/TrapFrame.h>
#include <Kernel/KSyms.h>
#include <Kernel/Process.h>
#include <Kernel/Thread.h>

namespace Kernel {

// The compiler can't see the calls to these functions inside assembly.
// Declare them, to avoid dead code warnings.
extern "C" void enter_thread_context(Thread* from_thread, Thread* to_thread) __attribute__((used));
extern "C" u32 do_init_context(Thread* thread, u32 flags) __attribute__((used));

extern "C" void enter_thread_context(Thread* from_thread, Thread* to_thread)
{
    (void)from_thread;
    (void)to_thread;
    TODO();
}

extern "C" u32 do_init_context(Thread* thread, u32 flags)
{
    (void)thread;
    (void)flags;
    TODO();
}
}
