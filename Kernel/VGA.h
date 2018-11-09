#pragma once

#include "types.h"

void vga_init();
void vga_set_cursor(WORD);
void vga_set_cursor(byte row, byte column, word start_address);
void vga_set_cursor(BYTE row, BYTE column);
WORD vga_get_cursor();
void vga_putch_at(byte row, byte column, byte ch, byte attr);
void vga_scroll_up();
void vga_clear();
void vga_clear_row(word);
word vga_get_start_address();
void vga_set_start_address(word);
