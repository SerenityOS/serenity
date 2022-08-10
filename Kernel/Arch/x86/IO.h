/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Assertions.h>
#include <AK/Format.h>
#include <AK/Types.h>

#include <AK/Platform.h>
VALIDATE_IS_X86()

namespace IO {

// Every character written to this IO port is written to the Bochs console
// (e.g. the console where Qemu is running).
static constexpr u16 BOCHS_DEBUG_PORT = 0xE9;

inline u8 in8(u16 port)
{
    u8 value;
    asm volatile("inb %1, %0"
                 : "=a"(value)
                 : "Nd"(port));
    return value;
}

inline u16 in16(u16 port)
{
    u16 value;
    asm volatile("inw %1, %0"
                 : "=a"(value)
                 : "Nd"(port));
    return value;
}

inline u32 in32(u16 port)
{
    u32 value;
    asm volatile("inl %1, %0"
                 : "=a"(value)
                 : "Nd"(port));
    return value;
}

inline void out8(u16 port, u8 value)
{
    asm volatile("outb %0, %1" ::"a"(value), "Nd"(port));
}

inline void out16(u16 port, u16 value)
{
    asm volatile("outw %0, %1" ::"a"(value), "Nd"(port));
}

inline void out32(u16 port, u32 value)
{
    asm volatile("outl %0, %1" ::"a"(value), "Nd"(port));
}

inline void delay(size_t microseconds)
{
    for (size_t i = 0; i < microseconds; ++i)
        IO::in8(0x80);
}

}
