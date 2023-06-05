/*
 * Copyright (c) 2022, Timon Kruiper <timonkruiper@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Types.h>
#include <Kernel/Arch/aarch64/RPi/UART.h>
#include <Kernel/Devices/GPU/Console/BootFramebufferConsole.h>
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

static void critical_console_out(char ch)
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

    for (size_t i = 0; i < length; ++i)
        critical_console_out(characters[i]);
}

void kernelearlyputstr(char const* characters, size_t length)
{
    kernelputstr(characters, length);
}

void dbgputstr(char const* characters, size_t length)
{
    kernelputstr(characters, length);
}

void dbgputstr(StringView view)
{
    dbgputstr(view.characters_without_null_termination(), view.length());
}

void dbgputchar(char ch)
{
    kernelputstr(&ch, 1);
}
