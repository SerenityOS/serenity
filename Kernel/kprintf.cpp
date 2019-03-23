#include "kprintf.h"
#include "Console.h"
#include "IO.h"
#include <LibC/stdarg.h>
#include "Process.h"
#include <AK/Types.h>
#include <AK/printf.cpp>

static void console_putch(char*&, char ch)
{
    if (!current) {
        IO::out8(0xe9, ch);
        return;
    }
    Console::the().write(current->process(), (byte*)&ch, 1);
}

int kprintf(const char* fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    int ret = printf_internal(console_putch, nullptr, fmt, ap);
    va_end(ap);
    return ret;
}

static void buffer_putch(char*& bufptr, char ch)
{
    *bufptr++ = ch;
}

int ksprintf(char* buffer, const char* fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    int ret = printf_internal(buffer_putch, buffer, fmt, ap);
    buffer[ret] = '\0';
    va_end(ap);
    return ret;
}

static void debugger_putch(char*&, char ch)
{
    IO::out8(0xe9, ch);
}

extern "C" int dbgprintf(const char* fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    int ret = printf_internal(debugger_putch, nullptr, fmt, ap);
    va_end(ap);
    return ret;
}
