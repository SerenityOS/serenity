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

class IOAddress {
public:
    IOAddress() = default;
    explicit IOAddress(u16 address)
        : m_address(address)
    {
    }

    IOAddress offset(u16 o) const { return IOAddress(m_address + o); }
    u16 get() const { return m_address; }
    void set(u16 address) { m_address = address; }
    void mask(u16 m) { m_address &= m; }

    template<typename T>
    ALWAYS_INLINE T in()
    {
        static_assert(sizeof(T) <= 4);
        if constexpr (sizeof(T) == 4)
            return IO::in32(get());
        if constexpr (sizeof(T) == 2)
            return IO::in16(get());
        if constexpr (sizeof(T) == 1)
            return IO::in8(get());
        VERIFY_NOT_REACHED();
    }

    template<typename T>
    ALWAYS_INLINE void out(T value) const
    {
        static_assert(sizeof(T) <= 4);
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
        VERIFY_NOT_REACHED();
    }

    inline void out(u32 value, u8 bit_width) const
    {
        if (bit_width == 32) {
            IO::out32(get(), value);
            return;
        }
        if (bit_width == 16) {
            IO::out16(get(), value);
            return;
        }
        if (bit_width == 8) {
            IO::out8(get(), value);
            return;
        }
        VERIFY_NOT_REACHED();
    }

    bool is_null() const { return m_address == 0; }

    bool operator==(IOAddress const& other) const { return m_address == other.m_address; }
    bool operator!=(IOAddress const& other) const { return m_address != other.m_address; }
    bool operator>(IOAddress const& other) const { return m_address > other.m_address; }
    bool operator>=(IOAddress const& other) const { return m_address >= other.m_address; }
    bool operator<(IOAddress const& other) const { return m_address < other.m_address; }
    bool operator<=(IOAddress const& other) const { return m_address <= other.m_address; }

private:
    u16 m_address { 0 };
};

template<>
struct AK::Formatter<IOAddress> : AK::Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, IOAddress value)
    {
        return Formatter<FormatString>::format(builder, "IO {:x}"sv, value.get());
    }
};
