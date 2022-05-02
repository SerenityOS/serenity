/*
 * Copyright (c) 2022, Timon Kruiper <timonkruiper@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/aarch64/RPi/UART.h>
#include <Kernel/kstdio.h>

// FIXME: Merge the code in this file with Kernel/kprintf.cpp once the proper abstractions are in place.

void kernelputstr(char const* characters, size_t)
{
    if (!characters)
        return;

    auto& uart = Prekernel::UART::the();
    uart.print_str(characters);
}

void kernelcriticalputstr(char const* characters, size_t)
{
    if (!characters)
        return;

    auto& uart = Prekernel::UART::the();
    uart.print_str(characters);
}

void kernelearlyputstr(char const* characters, size_t)
{
    if (!characters)
        return;

    auto& uart = Prekernel::UART::the();
    uart.print_str(characters);
}
