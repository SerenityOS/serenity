/*
 * Copyright (c) 2022, Timon Kruiper <timonkruiper@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/Processor.h>
#include <Kernel/KSyms.h>
#include <Kernel/Panic.h>

// FIXME: Merge the code in this file with Kernel/Panic.cpp once the proper abstractions are in place.

namespace Kernel {

void __panic(char const* file, unsigned int line, char const* function)
{
    critical_dmesgln("at {}:{} in {}", file, line, function);
    dump_backtrace(PrintToScreen::Yes);

    Processor::halt();
}

}
