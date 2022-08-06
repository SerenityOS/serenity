/*
 * Copyright (c) 2022, Timon Kruiper <timonkruiper@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Types.h>
#include <Kernel/Arch/aarch64/RPi/UART.h>
#include <Kernel/Graphics/Console/BootFramebufferConsole.h>
#include <Kernel/kstdio.h>

// FIXME: Merge the code in this file with Kernel/kprintf.cpp once the proper abstractions are in place.

namespace Kernel {
extern Atomic<Graphics::Console*> g_boot_console;
}

static void console_out(char ch)
{
    if (auto* boot_console = g_boot_console.load()) {
        boot_console->write(ch, true);
    }
}

void kernelputstr(char const* characters, size_t length)
{
    if (!characters)
        return;

    auto& uart = Kernel::RPi::UART::the();
    uart.print_str(characters, length);

    for (size_t i = 0; i < length; ++i)
        console_out(characters[i]);
}

void kernelcriticalputstr(char const* characters, size_t length)
{
    if (!characters)
        return;

    auto& uart = Kernel::RPi::UART::the();
    uart.print_str(characters, length);
}

void kernelearlyputstr(char const* characters, size_t length)
{
    if (!characters)
        return;

    auto& uart = Kernel::RPi::UART::the();
    uart.print_str(characters, length);
}
