#pragma once

#include <AK/Types.h>

namespace IO {

inline byte in8(word port)
{
    byte value;
    asm volatile("inb %1, %0"
                 : "=a"(value)
                 : "Nd"(port));
    return value;
}

inline word in16(word port)
{
    word value;
    asm volatile("inw %1, %0"
                 : "=a"(value)
                 : "Nd"(port));
    return value;
}

inline dword in32(word port)
{
    dword value;
    asm volatile("inl %1, %0"
                 : "=a"(value)
                 : "Nd"(port));
    return value;
}

inline void repeated_in16(word port, byte* buffer, int buffer_size)
{
    asm volatile("rep insw"
                 : "+D"(buffer), "+c"(buffer_size)
                 : "d"(port)
                 : "memory");
}

inline void out8(word port, byte value)
{
    asm volatile("outb %0, %1" ::"a"(value), "Nd"(port));
}

inline void out16(word port, word value)
{
    asm volatile("outw %0, %1" ::"a"(value), "Nd"(port));
}

inline void out32(word port, dword value)
{
    asm volatile("outl %0, %1" ::"a"(value), "Nd"(port));
}

inline void repeated_out16(word port, const byte* data, int data_size)
{
    asm volatile("rep outsw"
                 : "+S"(data), "+c"(data_size)
                 : "d"(port));
}

}
