#include <AK/PrintfImplementation.h>
#include <AK/Types.h>
#include <Kernel/Console.h>
#include <Kernel/IO.h>
#include <Kernel/Process.h>
#include <Kernel/kstdio.h>
#include <LibC/stdarg.h>

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
static void console_putch(char*&, char ch)
{
    if (!current) {
        IO::out8(0xe9, ch);
        return;
    }
    Console::the().put_char(ch);
}

int kprintf(const char* fmt, ...)
{
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

int ksprintf(char* buffer, const char* fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    int ret = printf_internal(buffer_putch, buffer, fmt, ap);
    buffer[ret] = '\0';
    va_end(ap);
    return ret;
}

extern "C" int dbgputstr(const char* characters, int length)
{
    for (int i = 0; i < length; ++i)
        IO::out8(0xe9, characters[i]);
    return 0;
}

static void debugger_putch(char*&, char ch)
{
    IO::out8(0xe9, ch);
}

extern "C" int dbgprintf(const char* fmt, ...)
{
    color_on();
    va_list ap;
    va_start(ap, fmt);
    int ret = printf_internal(debugger_putch, nullptr, fmt, ap);
    va_end(ap);
    color_off();
    return ret;
}
