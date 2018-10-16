#include "IO.h"

namespace IO {

BYTE in8(WORD port)
{
    BYTE value;
    asm("inb %%dx, %%al":"=a"(value):"d"(port));
    return value;
}

WORD in16(WORD port)
{
    WORD value;
    asm("inw %%dx, %%ax":"=a"(value):"d"(port));
    return value;
}

DWORD in32(DWORD port)
{
    DWORD value;
    asm("inl %%dx, %%eax":"=a"(value):"d"(port));
    return value;
}

void out8(WORD port, BYTE value)
{
    asm("outb %%al, %%dx"::"d"(port), "a"(value));
}

void out16(WORD port, WORD value)
{
    asm("outw %%ax, %%dx"::"d"(port), "a"(value));
}

void out32(WORD port, WORD value)
{
    asm("outl %%eax, %%dx"::"d"(port), "a"(value));
}

}
