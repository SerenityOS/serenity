/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <Kernel/Arch/x86/Processor.h>
#include <Kernel/CommandLine.h>
#include <Kernel/KSyms.h>
#include <Kernel/Panic.h>

namespace Kernel {

[[noreturn]] static void __reset()
{
    // FIXME: This works for i686/x86_64, but needs to be ported to any other arch when needed.
    asm(
        "lidt 0\n"
        "movl $0, 0\n");

    __builtin_unreachable();
}

void __panic(const char* file, unsigned int line, const char* function)
{
    critical_dmesgln("at {}:{} in {}", file, line, function);
    dump_backtrace();
    if (kernel_command_line().boot_mode() == BootMode::SelfTest)
        __reset();
    else
        Processor::halt();
}
}
