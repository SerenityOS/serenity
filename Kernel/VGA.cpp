#include "types.h"
#include "VGA.h"
#include "i386.h"
#include "IO.h"
#include "StdLib.h"
#include "Task.h"
#include <stdarg.h>

PRIVATE BYTE *vga_mem = 0L;
PRIVATE BYTE current_attr = 0x07;

PRIVATE volatile WORD soft_cursor;

template<typename PutChFunc> static int printNumber(PutChFunc, char*&, DWORD);
template<typename PutChFunc> static int printHex(PutChFunc, char*&, DWORD, BYTE fields);
template<typename PutChFunc> static int printSignedNumber(PutChFunc, char*&, int);

static void vga_putch(char*, char ch)
{
    WORD row;

    switch (ch) {
    case '\n':
        row = soft_cursor / 80;
        if (row == 23) {
            memcpy( vga_mem, vga_mem + 160, 160 * 23 );
            memset( vga_mem + (160 * 23), 0, 160 );
            soft_cursor = row * 80;
        } else {
            soft_cursor = (row + 1) * 80;
        }
        return;
    default:
        vga_mem[soft_cursor * 2] = ch;
        vga_mem[soft_cursor * 2 + 1] = current_attr;
        ++soft_cursor;
    }
    row = soft_cursor / 80;
    if ((row >= 24 && current->handle() != IPC::Handle::PanelTask)) {
        memcpy(vga_mem, vga_mem + 160, 160 * 23);
        memset(vga_mem + (160 * 23), 0, 160);
        soft_cursor = 23 * 80;
    }
}

template<typename PutChFunc>
int kprintfInternal(PutChFunc putch, char* buffer, const char*& fmt, char*& ap)
{
    const char *p;

    soft_cursor = vga_get_cursor();

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
                    ret += printNumber(putch, bufptr, va_arg(ap, DWORD));
                    break;

                case 'x':
                    ret += printHex(putch, bufptr, va_arg(ap, DWORD), 8);
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
                    ret += printHex(putch, bufptr, va_arg(ap, DWORD), 8);
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
    soft_cursor = vga_get_cursor();
    int ret = kprintfInternal(vga_putch, nullptr, fmt, ap);
    vga_set_cursor(soft_cursor);
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
    va_end(ap);
    return ret;
}

template<typename PutChFunc>
int printHex(PutChFunc putch, char*& bufptr, DWORD number, BYTE fields)
{
    static const char h[] = {
        '0','1','2','3','4','5','6','7',
        '8','9','a','b','c','d','e','f'
    };

    int ret = 0;
    BYTE shr_count = fields * 4;
    while (shr_count) {
        shr_count -= 4;
        putch(bufptr, h[(number >> shr_count) & 0x0F]);
        ++ret;
    }
    return ret;
}

template<typename PutChFunc>
int printNumber(PutChFunc putch, char*& bufptr, DWORD number)
{
    DWORD divisor = 1000000000;
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

PUBLIC void
vga_set_attr( BYTE attr )
{
    current_attr = attr;
}

PUBLIC BYTE
vga_get_attr()
{
    return current_attr;
}

PUBLIC void
vga_init()
{
    DWORD i;

    current_attr = 0x07;
    vga_mem = (BYTE *)0xb8000;

    for( i = 0; i < (80 * 24); ++i )
    {
        vga_mem[i*2] = ' ';
        vga_mem[i*2 + 1] = 0x07;
    }

    /* Fill the bottom line with blue. */
    for( i = (80 * 24); i < (80 * 25); ++i )
    {
        vga_mem[i*2] = ' ';
        vga_mem[i*2 + 1] = 0x17;
    }
    vga_set_cursor( 0 );
}

PUBLIC WORD
vga_get_cursor()
{
    WORD value;
    IO::out8(0x3d4, 0x0e);
    value = IO::in8(0x3d5) << 8;
    IO::out8(0x3d4, 0x0f);
    value |= IO::in8(0x3d5);
    return value;
}

PUBLIC void
vga_set_cursor( WORD value )
{
    if( value >= (80 * 25) )
    {
        /* XXX: If you try to move the cursor off the screen, I will go reddish pink! */
        vga_set_cursor( 0 );
        current_attr = 0x0C;
        return;
    }
    IO::out8(0x3d4, 0x0e);
    IO::out8(0x3d5, MSB(value));
    IO::out8(0x3d4, 0x0f);
    IO::out8(0x3d5, LSB(value));
}

void vga_set_cursor(BYTE row, BYTE column)
{
    vga_set_cursor(row * 80 + column);
}
