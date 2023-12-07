/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/CPU.h>
#include <Kernel/Arch/RegisterState.h>

#include <Kernel/Library/Panic.h>
#include <Kernel/Tasks/Process.h>
#include <Kernel/Tasks/Thread.h>

namespace Kernel {

void handle_crash(Kernel::RegisterState const& regs, char const* description, int signal, bool out_of_memory)
{
    auto* current_thread = Thread::current();
    if (!current_thread) {
        VERIFY(regs.previous_mode() == ExecutionMode::Kernel);

        dbgln("CRASH: CPU #{} {} in kernel", Processor::current_id(), description);

        dump_registers(regs);
        if (Memory::MemoryManager::is_initialized())
            MM.dump_kernel_regions();

        PANIC("Crash in kernel with !Thread::current()");
    }

    auto crashed_in_kernel = regs.previous_mode() == ExecutionMode::Kernel;
    if (!crashed_in_kernel && current_thread->has_signal_handler(signal) && !current_thread->should_ignore_signal(signal) && !current_thread->is_signal_masked(signal)) {
        current_thread->send_urgent_signal_to_self(signal);
        return;
    }

    auto& process = current_thread->process();

    // If a process crashed while inspecting another process,
    // make sure we switch back to the right page tables.
    Memory::MemoryManager::enter_process_address_space(process);

    dmesgln("CRASH: CPU #{} {} in {}", Processor::current_id(), description, regs.previous_mode() == ExecutionMode::Kernel ? "kernel"sv : "userspace"sv);
    dump_registers(regs);

    if (crashed_in_kernel) {
        process.address_space().with([&](auto& space) { space->dump_regions(); });
        PANIC("Crash in kernel");
    }

    process.crash(signal, { regs }, out_of_memory);
}

}
