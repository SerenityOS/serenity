/*
 * Copyright (c) 2021, Marcin Undak <mcinek@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/aarch64/RPi/UART.h>
#include <Kernel/Arch/aarch64/Utils.h>

void Prekernel::dbgln(char const* text)
{
    auto& uart = Prekernel::UART::the();
    uart.print_str(text);
    uart.print_str("\r\n");
}

void Prekernel::warnln(char const* text)
{
    dbgln(text);
}
