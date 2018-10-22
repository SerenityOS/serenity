#pragma once

#include "types.h"

void vga_init();
BYTE vga_get_attr();
void vga_set_attr(BYTE);
void vga_set_cursor(WORD);
void vga_set_cursor(BYTE row, BYTE column);
WORD vga_get_cursor();
void vga_putch_at(byte row, byte column, byte ch);
void vga_scroll_up();

int kprintf(const char *fmt, ...);
int ksprintf(char* buf, const char *fmt, ...);
