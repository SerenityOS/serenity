/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <Kernel/Arch/PowerState.h>
#include <Kernel/Arch/Processor.h>
#include <Kernel/Boot/CommandLine.h>
#include <Kernel/KSyms.h>
#include <Kernel/Library/Panic.h>
#include <Kernel/Tasks/Thread.h>

namespace Kernel {

void __panic(char const* file, unsigned int line, char const* function)
{
    // Avoid lock ranking checks on crashing paths, just try to get some debugging messages out.
    auto* thread = Thread::current();
    if (thread)
        thread->set_crashing();

    critical_dmesgln("at {}:{} in {}", file, line, function);
    dump_backtrace(PrintToScreen::Yes);
    if (!CommandLine::was_initialized())
        Processor::halt();
    switch (kernel_command_line().panic_mode()) {
    case PanicMode::Shutdown:
        arch_specific_poweroff(PowerOffOrRebootReason::SystemFailure);

        // Note: If we failed to invoke platform shutdown, we need to halt afterwards
        // to ensure no further execution on any CPU still happens.
        [[fallthrough]];
    case PanicMode::Halt:
    default:
        Processor::halt();
    }
}
}
