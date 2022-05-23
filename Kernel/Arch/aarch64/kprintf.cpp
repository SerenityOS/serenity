/*
 * Copyright (c) 2022, Timon Kruiper <timonkruiper@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/aarch64/RPi/UART.h>
#include <Kernel/kstdio.h>

// FIXME: Merge the code in this file with Kernel/kprintf.cpp once the proper abstractions are in place.

void kernelputstr(char const* characters, size_t length)
{
    if (!characters)
        return;

    auto& uart = Kernel::UART::the();
    uart.print_str(characters, length);
}

void kernelcriticalputstr(char const* characters, size_t length)
{
    if (!characters)
        return;

    auto& uart = Kernel::UART::the();
    uart.print_str(characters, length);
}

void kernelearlyputstr(char const* characters, size_t length)
{
    if (!characters)
        return;

    auto& uart = Kernel::UART::the();
    uart.print_str(characters, length);
}
