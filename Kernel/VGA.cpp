#include "types.h"
#include "VGA.h"
#include "i386.h"
#include "IO.h"
#include "StdLib.h"
#include "Task.h"

static byte* vga_mem = nullptr;

void vga_scroll_up()
{
    InterruptDisabler disabler;
    memcpy(vga_mem, vga_mem + 160, 160 * 24);
    vga_clear_row(24);
}

void vga_clear_row(word line)
{
    InterruptDisabler disabler;
    word* linemem = (word*)&vga_mem[line * 160];
    for (word i = 0; i < 80; ++i) {
        linemem[i] = 0x0720;
    }
}

void vga_clear()
{
    InterruptDisabler disabler;
    for (word i = 0; i < 25; ++i)
        vga_clear_row(i);
}

void vga_putch_at(byte row, byte column, byte ch, byte attr)
{
    word cur = (row * 160) + (column * 2);
    vga_mem[cur] = ch;
    vga_mem[cur + 1] = attr;
}

void vga_init()
{
    vga_mem = (byte*)0xb8000;

    for (word i = 0; i < (80 * 25); ++i) {
        vga_mem[i*2] = ' ';
        vga_mem[i*2 + 1] = 0x07;
    }

    vga_set_cursor(0);
}

WORD vga_get_cursor()
{
    WORD value;
    IO::out8(0x3d4, 0x0e);
    value = IO::in8(0x3d5) << 8;
    IO::out8(0x3d4, 0x0f);
    value |= IO::in8(0x3d5);
    return value;
}

void vga_set_cursor(WORD value)
{
    if (value >= (80 * 25)) {
        vga_set_cursor(0);
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
