#include <AK/PrintfImplementation.h>
#include <AK/Types.h>
#include <Kernel/Console.h>
#include <Kernel/IO.h>
#include <Kernel/Process.h>
#include <Kernel/kstdio.h>
#include <LibC/stdarg.h>

static bool serial_debug;

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

static void console_putch(char*&, char ch)
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

int sprintf(char* buffer, const char* fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    int ret = printf_internal(buffer_putch, buffer, fmt, ap);
    buffer[ret] = '\0';
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
    for (int i = 0; i < length; ++i)
        debugger_out(characters[i]);
    return 0;
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
