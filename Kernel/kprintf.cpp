/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/PrintfImplementation.h>
#include <AK/Types.h>
#include <Kernel/ConsoleDevice.h>
#include <Kernel/Devices/PCISerialDevice.h>
#include <Kernel/Graphics/Console/Console.h>
#include <Kernel/Graphics/GraphicsManagement.h>
#include <Kernel/IO.h>
#include <Kernel/Process.h>
#include <Kernel/SpinLock.h>
#include <Kernel/TTY/ConsoleManagement.h>
#include <Kernel/kstdio.h>

#include <LibC/stdarg.h>

static bool serial_debug;
// A recursive spinlock allows us to keep writing in the case where a
// page fault happens in the middle of a dbgln(), etc
static RecursiveSpinLock s_log_lock;

void set_serial_debug(bool on_or_off)
{
    serial_debug = on_or_off;
}

int get_serial_debug()
{
    return serial_debug;
}

static void serial_putch(char ch)
{
    if (PCISerialDevice::is_available())
        return PCISerialDevice::the().put_char(ch);

    static bool serial_ready = false;
    static bool was_cr = false;

    if (!serial_ready) {
        IO::out8(0x3F8 + 1, 0x00);
        IO::out8(0x3F8 + 3, 0x80);
        IO::out8(0x3F8 + 0, 0x02);
        IO::out8(0x3F8 + 1, 0x00);
        IO::out8(0x3F8 + 3, 0x03);
        IO::out8(0x3F8 + 2, 0xC7);
        IO::out8(0x3F8 + 4, 0x0B);

        serial_ready = true;
    }

    while ((IO::in8(0x3F8 + 5) & 0x20) == 0)
        ;

    if (ch == '\n' && !was_cr)
        IO::out8(0x3F8, '\r');

    IO::out8(0x3F8, ch);

    if (ch == '\r')
        was_cr = true;
    else
        was_cr = false;
}

static void critical_console_out(char ch)
{
    if (serial_debug)
        serial_putch(ch);
    // No need to output things to the real ConsoleDevice as no one is likely
    // to read it (because we are in a fatal situation, so only print things and halt)
    IO::out8(IO::BOCHS_DEBUG_PORT, ch);
    // We emit chars directly to the string. this is necessary in few cases,
    // especially when we want to avoid any memory allocations...
    if (GraphicsManagement::is_initialized() && GraphicsManagement::the().console()) {
        GraphicsManagement::the().console()->write(ch, true);
    }
}

static void console_out(char ch)
{
    if (serial_debug)
        serial_putch(ch);

    // It would be bad to reach the assert in ConsoleDevice()::the() and do a stack overflow

    if (ConsoleDevice::is_initialized()) {
        ConsoleDevice::the().put_char(ch);
    } else {
        IO::out8(IO::BOCHS_DEBUG_PORT, ch);
    }
    if (ConsoleManagement::is_initialized()) {
        ConsoleManagement::the().debug_tty()->emit_char(ch);
    }
}

static void buffer_putch(char*& bufptr, char ch)
{
    *bufptr++ = ch;
}

// Declare it, so that the symbol is exported, because libstdc++ uses it.
// However, *only* libstdc++ uses it, and none of the rest of the Kernel.
extern "C" int sprintf(char* buffer, const char* fmt, ...);

int sprintf(char* buffer, const char* fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    int ret = printf_internal(buffer_putch, buffer, fmt, ap);
    buffer[ret] = '\0';
    va_end(ap);
    return ret;
}

static size_t __vsnprintf_space_remaining;
ALWAYS_INLINE void sized_buffer_putch(char*& bufptr, char ch)
{
    if (__vsnprintf_space_remaining) {
        *bufptr++ = ch;
        --__vsnprintf_space_remaining;
    }
}

int snprintf(char* buffer, size_t size, const char* fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    if (size) {
        __vsnprintf_space_remaining = size - 1;
    } else {
        __vsnprintf_space_remaining = 0;
    }
    int ret = printf_internal(sized_buffer_putch, buffer, fmt, ap);
    if (__vsnprintf_space_remaining) {
        buffer[ret] = '\0';
    } else if (size > 0) {
        buffer[size - 1] = '\0';
    }
    va_end(ap);
    return ret;
}

static void debugger_out(char ch)
{
    if (serial_debug)
        serial_putch(ch);
    IO::out8(IO::BOCHS_DEBUG_PORT, ch);
}

extern "C" void dbgputstr(const char* characters, size_t length)
{
    if (!characters)
        return;
    ScopedSpinLock lock(s_log_lock);
    for (size_t i = 0; i < length; ++i)
        debugger_out(characters[i]);
}

extern "C" void kernelputstr(const char* characters, size_t length)
{
    if (!characters)
        return;
    ScopedSpinLock lock(s_log_lock);
    for (size_t i = 0; i < length; ++i)
        console_out(characters[i]);
}

extern "C" void kernelcriticalputstr(const char* characters, size_t length)
{
    if (!characters)
        return;
    ScopedSpinLock lock(s_log_lock);
    for (size_t i = 0; i < length; ++i)
        critical_console_out(characters[i]);
}
