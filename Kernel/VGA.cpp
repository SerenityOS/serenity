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

PRIVATE void print_num( DWORD );
PRIVATE void print_hex( DWORD, BYTE fields );
static void printSignedNumber(int);

PRIVATE void
putch( char ch )
{
    WORD row;

    switch( ch )
    {
        case '\n':
            row = soft_cursor / 80;
            if( row == 23 )
            {
                memcpy( vga_mem, vga_mem + 160, 160 * 23 );
                memset( vga_mem + (160 * 23), 0, 160 );
                soft_cursor = row * 80;
            }
            else
                soft_cursor = (row + 1) * 80;
            return;
        default:
            vga_mem[soft_cursor * 2] = ch;
            vga_mem[soft_cursor * 2 + 1] = current_attr;
            soft_cursor++;
    }
    row = soft_cursor / 80;
    if ((row >= 24 && current->handle() != IPC::Handle::PanelTask)) {
        memcpy( vga_mem, vga_mem + 160, 160 * 23 );
        memset( vga_mem + (160 * 23), 0, 160 );
        soft_cursor = 23 * 80;
    }
}

PUBLIC void
kprintf( const char *fmt, ... )
{
    const char *p;
    va_list ap;

    soft_cursor = vga_get_cursor();

    va_start( ap, fmt );

    for( p = fmt; *p; ++p )
    {
        if( *p == '%' && *(p + 1) )
        {
            ++p;
            switch( *p )
            {
                case 's':
                    {
                        const char* sp = va_arg(ap, const char*);
                        //ASSERT(sp != nullptr);
                        if (!sp) {
                            putch('<');
                            putch('N');
                            putch('u');
                            putch('L');
                            putch('>');
                        } else {
                            for (; *sp; ++sp)
                                putch(*sp);
                        }
                    }
                    break;

                case 'd':
                    printSignedNumber(va_arg(ap, int));
                    break;

                case 'u':
                    print_num( va_arg( ap, DWORD ));
                    break;

                case 'x':
                    print_hex( va_arg( ap, DWORD ), 8 );
                    break;

                case 'b':
                    print_hex( va_arg( ap, int ), 2 );
                    break;

                case 'c':
                    putch( (char)va_arg( ap, int ));
                    break;

                case 'p':
                    putch( '0' );
                    putch( 'x' );
                    print_hex( va_arg( ap, DWORD ), 8 );
                    break;
            }
        }
        else
        {
            putch( *p );
        }
    }

    /* va_arg( ap, type ); */
    va_end( ap );

    vga_set_cursor( soft_cursor );
}

PRIVATE void
print_hex( DWORD number, BYTE fields )
{
    static const char h[] = {
        '0','1','2','3','4','5','6','7',
        '8','9','a','b','c','d','e','f'
    };

    BYTE shr_count = fields * 4;
    while( shr_count )
    {
        shr_count -= 4;
        putch( h[(number >> shr_count) & 0x0F] );
    }
}

PRIVATE void
print_num( DWORD number )
{
    DWORD divisor = 1000000000;
    char ch;
    char padding = 1;

    for( ;; )
    {
        ch = '0' + (number / divisor);
        number %= divisor;

        if( ch != '0' )
            padding = 0;

        if( !padding || divisor == 1 )
            putch( ch );

        if( divisor == 1 )
            break;
        divisor /= 10;
    }
}

static void printSignedNumber(int number)
{
    if (number < 0) {
        putch('-');
        print_num(0 - number);
    } else {
        print_num(number);
    }
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
