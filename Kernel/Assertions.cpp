/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Konrad <konrad@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/Processor.h>
#include <Kernel/Assertions.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Panic.h>
#include <Kernel/Process.h>
#include <Kernel/Thread.h>

using namespace Kernel;

[[noreturn]] void abort()
{
    // Avoid lock ranking checks on crashing paths, just try to get some debugging messages out.
    auto thread = Thread::current();
    if (thread)
        thread->set_crashing();

    // Switch back to the current process's page tables if there are any.
    // Otherwise stack walking will be a disaster.
    if (Process::has_current())
        Memory::MemoryManager::enter_process_address_space(Process::current());

    PANIC("Aborted");
}

void __assertion_failed(char const* msg, char const* file, unsigned line, char const* func)
{
    Processor::disable_interrupts();

    critical_dmesgln("ASSERTION FAILED: {}", msg);
    critical_dmesgln("{}:{} in {}", file, line, func);

    abort();
}

[[noreturn]] void _abort()
{
    Processor::undefined_behaviour();
    __builtin_unreachable();
}
