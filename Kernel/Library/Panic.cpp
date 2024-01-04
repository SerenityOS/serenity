/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <Kernel/Arch/Processor.h>
#if ARCH(X86_64)
#    include <Kernel/Arch/x86_64/Shutdown.h>
#elif ARCH(AARCH64)
#    include <Kernel/Arch/aarch64/RPi/Watchdog.h>
#elif ARCH(RISCV64)
#    include <Kernel/Arch/riscv64/SBI.h>
#endif
#include <Kernel/Boot/CommandLine.h>
#include <Kernel/KSyms.h>
#include <Kernel/Library/Panic.h>
#include <Kernel/Tasks/Thread.h>

namespace Kernel {

[[noreturn]] static void __shutdown()
{
#if ARCH(X86_64)
    qemu_shutdown();
    virtualbox_shutdown();
#elif ARCH(AARCH64)
    RPi::Watchdog::the().system_shutdown();
#elif ARCH(RISCV64)
    auto ret = SBI::SystemReset::system_reset(SBI::SystemReset::ResetType::Shutdown, SBI::SystemReset::ResetReason::SystemFailure);
    dbgln("SBI: Failed to shut down: {}", ret);
    dbgln("SBI: Attempting to shut down using the legacy extension...");
    SBI::Legacy::shutdown();
#endif
    // Note: If we failed to invoke platform shutdown, we need to halt afterwards
    // to ensure no further execution on any CPU still happens.
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
