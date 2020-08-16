/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/PrintfImplementation.h>
#include <AK/Types.h>
#include <Kernel/Console.h>
#include <Kernel/IO.h>
#include <Kernel/Process.h>
#include <Kernel/SpinLock.h>
#include <Kernel/kstdio.h>

#include <LibC/stdarg.h>

static bool serial_debug;
// A recursive spinlock allows us to keep writing in the case where a
// page fault happens in the middle of a dbg(), klog(), etc
static RecursiveSpinLock s_log_lock;

void set_serial_debug(bool on_or_off)
{
    serial_debug = on_or_off;
}

int get_serial_debug()
{
    return serial_debug;
}

static void color_on()
{
    IO::out8(0xe9, 0x1b);
    IO::out8(0xe9, '[');
    IO::out8(0xe9, '3');
    IO::out8(0xe9, '6');
    IO::out8(0xe9, 'm');
}

static void color_off()
{
    IO::out8(0xe9, 0x1b);
    IO::out8(0xe9, '[');
    IO::out8(0xe9, '0');
    IO::out8(0xe9, 'm');
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

    // It would be bad to reach the assert in Console()::the() and do a stack overflow

    if (Console::is_initialized()) {
        Console::the().put_char(ch);
    } else {
        IO::out8(0xe9, ch);
    }
}

static void console_putch(char*&, char ch)
{
    console_out(ch);
}

int kprintf(const char* fmt, ...)
{
    ScopedSpinLock lock(s_log_lock);
    color_on();
    va_list ap;
    va_start(ap, fmt);
    int ret = printf_internal(console_putch, nullptr, fmt, ap);
    va_end(ap);
    color_off();
    return ret;
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
    ScopedSpinLock lock(s_log_lock);
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
    IO::out8(0xe9, ch);
}

static void debugger_putch(char*&, char ch)
{
    debugger_out(ch);
}

extern "C" int dbgputstr(const char* characters, int length)
{
    if (!characters)
        return 0;
    ScopedSpinLock lock(s_log_lock);
    for (int i = 0; i < length; ++i)
        debugger_out(characters[i]);
    return 0;
}

extern "C" int kernelputstr(const char* characters, int length)
{
    if (!characters)
        return 0;
    ScopedSpinLock lock(s_log_lock);
    for (int i = 0; i < length; ++i)
        console_out(characters[i]);
    return 0;
}

static int vdbgprintf(const char* fmt, va_list ap)
{
    ScopedSpinLock lock(s_log_lock);
    color_on();
    int ret = printf_internal(debugger_putch, nullptr, fmt, ap);
    color_off();
    return ret;
}

extern "C" int dbgprintf(const char* fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    int ret = vdbgprintf(fmt, ap);
    va_end(ap);
    return ret;
}
