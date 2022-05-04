/*
 * Copyright (c) 2021, James Mintram <me@jamesrm.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/aarch64/Prekernel/Prekernel.h>

#include <Kernel/Arch/aarch64/ASM_wrapper.h>
#include <Kernel/Arch/aarch64/RPi/UART.h>

namespace Prekernel {

[[noreturn]] void halt()
{
    for (;;) {
        asm volatile("wfi");
    }
}

}
