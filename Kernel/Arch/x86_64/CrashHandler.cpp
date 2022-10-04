/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/CPU.h>
#include <Kernel/Arch/RegisterState.h>

#include <Kernel/Panic.h>
#include <Kernel/Process.h>
#include <Kernel/Thread.h>

namespace Kernel {

void handle_crash(Kernel::RegisterState const& regs, char const* description, int signal, bool out_of_memory)
{
    auto* current_thread = Thread::current();
    if (!current_thread)
        PANIC("{} with !Thread::current()", description);

    auto crashed_in_kernel = (regs.cs & 3) == 0;
    if (!crashed_in_kernel && current_thread->has_signal_handler(signal) && !current_thread->should_ignore_signal(signal) && !current_thread->is_signal_masked(signal)) {
        current_thread->send_urgent_signal_to_self(signal);
        return;
    }

    auto& process = current_thread->process();

    // If a process crashed while inspecting another process,
    // make sure we switch back to the right page tables.
    Memory::MemoryManager::enter_process_address_space(process);

    dmesgln("CRASH: CPU #{} {} in ring {}", Processor::current_id(), description, (regs.cs & 3));
    dump_registers(regs);

    if (crashed_in_kernel) {
        process.address_space().with([&](auto& space) { space->dump_regions(); });
        PANIC("Crash in ring 0");
    }

    process.crash(signal, regs.ip(), out_of_memory);
}

}
