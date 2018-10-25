#include "kprintf.h"
#include "Console.h"
#include <stdarg.h>
#include <AK/Types.h>

template<typename PutChFunc> static int printNumber(PutChFunc, char*&, dword);
template<typename PutChFunc> static int printHex(PutChFunc, char*&, dword, byte fields);
template<typename PutChFunc> static int printSignedNumber(PutChFunc, char*&, int);

static void console_putch(char*, char ch)
{
    Console::the().write((byte*)&ch, 1);
}

template<typename PutChFunc>
int kprintfInternal(PutChFunc putch, char* buffer, const char*& fmt, char*& ap)
{
    const char *p;

    int ret = 0;
    char* bufptr = buffer;

    for (p = fmt; *p; ++p) {
        if (*p == '%' && *(p + 1)) {
            ++p;
            switch( *p )
            {
                case 's':
                    {
                        const char* sp = va_arg(ap, const char*);
                        //ASSERT(sp != nullptr);
                        if (!sp) {
                            putch(bufptr, '<');
                            putch(bufptr, 'N');
                            putch(bufptr, 'u');
                            putch(bufptr, 'L');
                            putch(bufptr, '>');
                            ret += 5;
                        } else {
                            for (; *sp; ++sp) {
                                putch(bufptr, *sp);
                                ++ret;
                            }
                        }
                    }
                    break;

                case 'd':
                    ret += printSignedNumber(putch, bufptr, va_arg(ap, int));
                    break;

                case 'u':
                    ret += printNumber(putch, bufptr, va_arg(ap, dword));
                    break;

                case 'x':
                    ret += printHex(putch, bufptr, va_arg(ap, dword), 8);
                    break;

                case 'w':
                    ret += printHex(putch, bufptr, va_arg(ap, int), 4);
                    break;

                case 'b':
                    ret += printHex(putch, bufptr, va_arg(ap, int), 2);
                    break;

                case 'c':
                    putch(bufptr, (char)va_arg(ap, int));
                    ++ret;
                    break;

                case 'p':
                    putch(bufptr, '0');
                    putch(bufptr, 'x');
                    ret += 2;
                    ret += printHex(putch, bufptr, va_arg(ap, dword), 8);
                    break;
            }
        }
        else {
            putch(bufptr, *p);
            ++ret;
        }
    }
    return ret;
}

int kprintf(const char* fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    int ret = kprintfInternal(console_putch, nullptr, fmt, ap);
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
    int ret = kprintfInternal(buffer_putch, buffer, fmt, ap);
    buffer[ret] = '\0';
    va_end(ap);
    return ret;
}

template<typename PutChFunc>
int printHex(PutChFunc putch, char*& bufptr, dword number, byte fields)
{
    static const char h[] = {
        '0','1','2','3','4','5','6','7',
        '8','9','a','b','c','d','e','f'
    };

    int ret = 0;
    byte shr_count = fields * 4;
    while (shr_count) {
        shr_count -= 4;
        putch(bufptr, h[(number >> shr_count) & 0x0F]);
        ++ret;
    }
    return ret;
}

template<typename PutChFunc>
int printNumber(PutChFunc putch, char*& bufptr, dword number)
{
    dword divisor = 1000000000;
    char ch;
    char padding = 1;
    int ret = 0;

    for (;;) {
        ch = '0' + (number / divisor);
        number %= divisor;

        if (ch != '0')
            padding = 0;

        if (!padding || divisor == 1) {
            putch(bufptr, ch);
            ++ret;
        }

        if (divisor == 1)
            break;
        divisor /= 10;
    }
    return ret;
}

template<typename PutChFunc>
static int printSignedNumber(PutChFunc putch, char*& bufptr, int number)
{
    if (number < 0) {
        putch(bufptr, '-');
        return printNumber(putch, bufptr, 0 - number) + 1;
    }
    return printNumber(putch, bufptr, number);
}

