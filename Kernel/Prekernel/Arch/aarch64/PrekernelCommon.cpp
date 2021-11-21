/*
 * Copyright (c) 2021, James Mintram <me@jamesrm.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Prekernel/Arch/aarch64/Prekernel.h>

#include <Kernel/Arch/aarch64/ASM_wrapper.h>
#include <Kernel/Prekernel/Arch/aarch64/UART.h>

namespace Prekernel {

[[noreturn]] void panic(const char* msg)
{
    auto& uart = Prekernel::UART::the();

    if (msg) {
        uart.print_str(msg);
    }

    Prekernel::halt();
}

[[noreturn]] void halt()
{
    for (;;) {
        asm volatile("wfi");
    }
}

}
