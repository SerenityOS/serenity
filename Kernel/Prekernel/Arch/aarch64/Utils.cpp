/*
 * Copyright (c) 2021, Marcin Undak <mcinek@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Prekernel/Arch/aarch64/UART.h>
#include <Kernel/Prekernel/Arch/aarch64/Utils.h>

void Prekernel::dbgln(const char* text)
{
    auto& uart = Prekernel::UART::the();
    uart.print_str(text);
    uart.print_str("\r\n");
}

void Prekernel::warnln(const char* text)
{
    dbgln(text);
}
