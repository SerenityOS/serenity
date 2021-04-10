/*
 * Copyright (c) 2021, Leon Albrecht <leon2002.la@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "u128.h"

#include <AK/Concepts.h>
#include <AK/Format.h>
#include <AK/Types.h>

namespace X86 {

class u256 {
public:
    constexpr u256() = default;
    constexpr u256(u64 val)
        : m_low(val)
    {
    }
    constexpr u256(u128 val)
        : m_low(val)
    {
    }
    constexpr u256(u128 val_low, u128 val_high)
        : m_low(val_low)
        , m_high(val_high)
    {
    }

    ALWAYS_INLINE u8* bytes()
    {
        return (u8*)this;
    }
    ALWAYS_INLINE const u8* bytes() const
    {
        return (const u8*)this;
    }

    ALWAYS_INLINE u16* words()
    {
        return (u16*)this;
    }
    ALWAYS_INLINE const u16* words() const
    {
        return (const u16*)this;
    }

    ALWAYS_INLINE u32* double_words()
    {
        return (u32*)this;
    }
    ALWAYS_INLINE const u32* double_words() const
    {
        return (const u32*)this;
    }

    ALWAYS_INLINE constexpr u128& low()
    {
        return m_low;
    }
    ALWAYS_INLINE constexpr const u128& low() const
    {
        return m_low;
    }

    ALWAYS_INLINE constexpr u128& high()
    {
        return m_high;
    }
    ALWAYS_INLINE constexpr const u128& high() const
    {
        return m_high;
    }
    // conversion
    template<Unsigned_128 T>
    ALWAYS_INLINE constexpr operator T() const
    {
        return m_low;
    }

    ALWAYS_INLINE constexpr operator bool() const
    {
        return m_low || m_high;
    }

    // comparisons
    template<Unsigned_128 T>
    ALWAYS_INLINE constexpr bool operator==(const T& other) const
    {
        return !m_high && m_low == other;
    }
    template<Unsigned_128 T>
    ALWAYS_INLINE constexpr bool operator!=(const T& other) const
    {
        return m_high || m_low != other;
    }
    template<Unsigned_128 T>
    ALWAYS_INLINE constexpr bool operator>(const T& other) const
    {
        return m_high || m_low > other;
    }
    template<Unsigned_128 T>
    ALWAYS_INLINE constexpr bool operator<(const T& other) const
    {
        return !m_high && m_low < other;
    }
    template<Unsigned_128 T>
    ALWAYS_INLINE constexpr bool operator>=(const T& other) const
    {
        return *this == other || *this > other;
    }
    template<Unsigned_128 T>
    ALWAYS_INLINE constexpr bool operator<=(const T& other) const
    {
        return *this == other || *this < other;
    }

    ALWAYS_INLINE constexpr bool operator==(const u256& other) const
    {
        return m_low == other.low() && m_high == other.high();
    }
    ALWAYS_INLINE constexpr bool operator!=(const u256& other) const
    {
        return m_low != other.low() || m_high != other.high();
    }
    ALWAYS_INLINE constexpr bool operator>(const u256& other) const
    {
        return m_high > other.high()
            || (m_high == other.high() && m_low > other.low());
    }
    ALWAYS_INLINE constexpr bool operator<(const u256& other) const
    {
        return m_high < other.high()
            || (m_high == other.high() && m_low < other.low());
    }
    ALWAYS_INLINE constexpr bool operator>=(const u256& other) const
    {
        return *this == other || *this > other;
    }
    ALWAYS_INLINE constexpr bool operator<=(const u256& other) const
    {
        return *this == other || *this < other;
    }

    // bitwise
    template<Unsigned_128 T>
    ALWAYS_INLINE constexpr T operator&(const T& other) const
    {
        return m_low & other;
    }
    template<Unsigned_128 T>
    ALWAYS_INLINE constexpr u256 operator|(const T& other) const
    {
        return { m_low | other, m_high };
    }
    template<Unsigned_128 T>
    ALWAYS_INLINE constexpr u256 operator^(const T& other) const
    {
        return { m_low ^ other, m_high };
    }
    template<Unsigned_128 T>
    ALWAYS_INLINE constexpr u256 operator<<(const T& other) const
    {
        u128 overflow = m_low >> (128 - other);
        return { m_low << other, (m_high << other) | overflow };
    }
    template<Unsigned_128 T>
    ALWAYS_INLINE constexpr u256 operator>>(const T& other) const
    {
        u128 underflow = m_high & other;
        return { (m_low >> other) | (underflow << (128 - other)), m_high >> other };
    }

    ALWAYS_INLINE constexpr u256 operator&(const u256& other) const
    {
        return { m_low & other.low(), m_high & other.high() };
    }
    ALWAYS_INLINE constexpr u256 operator|(const u256& other) const
    {
        return { m_low | other.low(), m_high | other.high() };
    }
    ALWAYS_INLINE constexpr u256 operator^(const u256& other) const
    {
        return { m_low ^ other.low(), m_high ^ other.high() };
    }

    // bitwise assign
    template<Unsigned_128 T>
    constexpr u256& operator&=(const T& other)
    {
        m_high = 0;
        m_low &= other;
        return *this;
    }
    template<Unsigned_128 T>
    constexpr u256& operator|=(const T& other)
    {
        m_low |= other;
        return *this;
    }
    template<Unsigned_128 T>
    constexpr u256& operator^=(const T& other)
    {
        m_low ^= other;
        return *this;
    }
    template<Unsigned_128 T>
    constexpr u256& operator>>=(const T& other)
    {
        *this = *this >> other;
        return *this;
    }
    template<Unsigned_128 T>
    constexpr u256& operator<<=(const T& other)
    {
        *this = *this << other;
        return *this;
    }

    constexpr u256& operator&=(const u256& other)
    {
        m_high &= other.high();
        m_low &= other.low();
        return *this;
    }
    constexpr u256& operator|=(const u256& other)
    {
        m_high |= other.high();
        m_low |= other.low();
        return *this;
    }
    constexpr u256& operator^=(const u256& other)
    {
        m_high ^= other.high();
        m_low ^= other.low();
        return *this;
    }

private:
    // FIXME: Somehow make this a union to directly expose the bytes (see u128)
    u128 m_low {};
    u128 m_high {};
};

static_assert(sizeof(u256) == 32);

template<typename T>
concept Unsigned_256 = IsUnsigned<T> || IsSame<T, u128> || IsSame<T, u256>;

}

using X86::u256;
using X86::Unsigned_256;

template<>
struct AK::Formatter<u256> : StandardFormatter {
    Formatter() = default;
    explicit Formatter(StandardFormatter formatter)
        : StandardFormatter(formatter)
    {
    }

    void format(AK::FormatBuilder&, u256);
};
