/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <YAK/Assertions.h>
#include <Kernel/Arch/x86/CPU.h>
#include <Kernel/Panic.h>
#include <Kernel/Process.h>

void __assertion_failed(const char* msg, const char* file, unsigned line, const char* func)
{
    asm volatile("cli");
    critical_dmesgln("ASSERTION FAILED: {}", msg);
    critical_dmesgln("{}:{} in {}", file, line, func);

    abort();
}

[[noreturn]] void abort()
{
    // Switch back to the current process's page tables if there are any.
    // Otherwise stack walking will be a disaster.
    if (Process::has_current())
        MM.enter_process_paging_scope(Process::current());

    PANIC("Aborted");
}

[[noreturn]] void _abort()
{
    asm volatile("ud2");
    __builtin_unreachable();
}
