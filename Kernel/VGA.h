#pragma once

#include "types.h"

void vga_init();
BYTE vga_get_attr();
void vga_set_attr(BYTE);
void vga_set_cursor(WORD);
void vga_set_cursor(BYTE row, BYTE column);
WORD vga_get_cursor();
void kprintf(const char *fmt, ...);
