/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/Processor.h>
#include <Kernel/Library/Assertions.h>
#include <Kernel/Library/Panic.h>
#include <Kernel/Tasks/Process.h>
#include <Kernel/Tasks/Thread.h>

// Note: This is required here, since __assertion_failed should be out of the Kernel namespace,
//       but the PANIC macro uses functions that require the Kernel namespace.
using namespace Kernel;

NO_SANITIZE_COVERAGE void __assertion_failed(char const* msg, char const* file, unsigned line, char const* func)
{
    Processor::disable_interrupts();
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
