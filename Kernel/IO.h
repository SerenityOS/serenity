#pragma once

#include "types.h"

namespace IO {

inline byte in8(word port)
{
    byte value;
    asm volatile("inb %1, %0":"=a"(value):"Nd"(port));
    return value;
}

inline word in16(word port)
{
    word value;
    asm volatile("inw %1, %0":"=a"(value):"Nd"(port));
    return value;
}

inline dword in32(dword port)
{
    dword value;
    asm volatile("inl %1, %0":"=a"(value):"Nd"(port));
    return value;
}

inline void out8(word port, byte value)
{
    asm volatile("outb %0, %1"::"a"(value), "Nd"(port));
}

inline void out16(word port, word value)
{
    asm volatile("outw %0, %1"::"a"(value), "Nd"(port));
}

inline void out32(word port, dword value)
{
    asm volatile("outl %0, %1"::"a"(value), "Nd"(port));
}
}
