/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/PrintfImplementation.h>
#include <AK/Types.h>
#include <Kernel/IO.h>
#include <Kernel/Prekernel/kstdio.h>

static bool serial_debug;
// A recursive spinlock allows us to keep writing in the case where a
// page fault happens in the middle of a dbgln(), etc

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

static void console_out(char ch)
{
    if (serial_debug)
        serial_putch(ch);
    IO::out8(IO::BOCHS_DEBUG_PORT, ch);
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

extern "C" void dbgputch(char ch)
{
    if (serial_debug)
        serial_putch(ch);
    IO::out8(IO::BOCHS_DEBUG_PORT, ch);
}

extern "C" void dbgputstr(const char* characters, size_t length)
{
    if (!characters)
        return;
    for (size_t i = 0; i < length; ++i)
        dbgputch(characters[i]);
}

extern "C" void kernelputstr(const char* characters, size_t length)
{
    if (!characters)
        return;
    for (size_t i = 0; i < length; ++i)
        console_out(characters[i]);
}

