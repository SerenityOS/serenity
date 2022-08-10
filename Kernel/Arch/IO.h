/*
 * Copyright (c) 2022, Filiph Sandström <filiph.sandstrom@filfatstudios.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Platform.h>

#if ARCH(AARCH64)
#    include <Kernel/Arch/aarch64/IO.h>
#else
#    include <Kernel/Arch/x86/IO.h>
#endif

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
