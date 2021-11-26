/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <Kernel/Arch/x86/CPU.h>
#include <Kernel/Panic.h>
#include <Kernel/Process.h>

using namespace Kernel;

void __assertion_failed(const char* msg, const char* file, unsigned line, const char* func)
{
    asm volatile("cli");
    critical_dmesgln("ASSERTION FAILED: {}", msg);
    critical_dmesgln("{}:{} in {}", file, line, func);

    abort();
}

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

[[noreturn]] void _abort()
{
    asm volatile("ud2");
    __builtin_unreachable();
}
