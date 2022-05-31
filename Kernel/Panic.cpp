/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <Kernel/Arch/Processor.h>
#include <Kernel/Arch/x86/IO.h>
#include <Kernel/CommandLine.h>
#include <Kernel/KSyms.h>
#include <Kernel/Panic.h>
#include <Kernel/Thread.h>

namespace Kernel {

[[noreturn]] static void __shutdown()
{
    // Note: This will invoke QEMU Shutdown, but for other platforms (or emulators),
    // this has no effect on the system, so we still need to halt afterwards.
    // We also try the Bochs/Old QEMU shutdown method, if the first didn't work.
    IO::out16(0x604, 0x2000);
    IO::out16(0xb004, 0x2000);
    Processor::halt();
}

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
        __shutdown();
    case PanicMode::Halt:
        [[fallthrough]];
    default:
        Processor::halt();
    }
}
}
