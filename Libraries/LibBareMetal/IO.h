/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/Assertions.h>
#include <AK/LogStream.h>
#include <AK/String.h>
#include <AK/Types.h>

#if defined(KERNEL)
#    include <Kernel/Arch/i386/CPU.h>
#endif

namespace IO {

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

inline void repeated_in16(u16 port, u8* buffer, int buffer_size)
{
    asm volatile("rep insw"
                 : "+D"(buffer), "+c"(buffer_size)
                 : "d"(port)
                 : "memory");
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

inline void repeated_out16(u16 port, const u8* data, int data_size)
{
    asm volatile("rep outsw"
                 : "+S"(data), "+c"(data_size)
                 : "d"(port));
}

inline void delay()
{
    // ~3 microsecs
    for (auto i = 0; i < 32; i++) {
        IO::in8(0x80);
    }
}

}

class IOAddress {
public:
    IOAddress() {}
    explicit IOAddress(u16 address)
        : m_address(address)
    {
    }

    IOAddress offset(u16 o) const { return IOAddress(m_address + o); }
    u16 get() const { return m_address; }
    void set(u16 address) { m_address = address; }
    void mask(u16 m) { m_address &= m; }

    template<typename T>
    [[gnu::always_inline]] inline T in()
    {
        if constexpr (sizeof(T) == 4)
            return IO::in32(get());
        if constexpr (sizeof(T) == 2)
            return IO::in16(get());
        if constexpr (sizeof(T) == 1)
            return IO::in8(get());
        ASSERT_NOT_REACHED();
    }

    template<typename T>
    [[gnu::always_inline]] inline void out(T value)
    {
        if constexpr (sizeof(T) == 4) {
            IO::out32(get(), value);
            return;
        }
        if constexpr (sizeof(T) == 2) {
            IO::out16(get(), value);
            return;
        }
        if constexpr (sizeof(T) == 1) {
            IO::out8(get(), value);
            return;
        }
        ASSERT_NOT_REACHED();
    }

    bool is_null() const { return m_address == 0; }

    bool operator==(const IOAddress& other) const { return m_address == other.m_address; }
    bool operator!=(const IOAddress& other) const { return m_address != other.m_address; }
    bool operator>(const IOAddress& other) const { return m_address > other.m_address; }
    bool operator>=(const IOAddress& other) const { return m_address >= other.m_address; }
    bool operator<(const IOAddress& other) const { return m_address < other.m_address; }
    bool operator<=(const IOAddress& other) const { return m_address <= other.m_address; }

private:
    u16 m_address { 0 };
};

inline const LogStream& operator<<(const LogStream& stream, IOAddress value)
{
    return stream << "IO " << String::format("%x", value.get());
}
