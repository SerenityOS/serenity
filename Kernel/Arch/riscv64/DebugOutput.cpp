/*
 * Copyright (c) 2023, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/DebugOutput.h>
#include <Kernel/Arch/riscv64/SBI.h>
#include <Kernel/Library/Assertions.h>

namespace Kernel {

void debug_output(char c)
{
    // FIXME: add extension probing support to SBI.cpp to check which debug console extensions are available
    (void)SBI::Legacy::console_putchar(c);
}

}
