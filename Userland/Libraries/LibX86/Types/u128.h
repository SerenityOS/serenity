/*
 * Copyright (c) 2021, Leon Albrecht <leon2002.la@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Concepts.h>
#include <AK/Format.h>
#include <AK/Types.h>

namespace X86 {

class u128 {
public:
    constexpr u128() = default;
    template<Unsigned T>
    constexpr u128(T val)
        : m_low(val)
    {
    }
    constexpr u128(u64 val_low, u64 val_high)
        : m_low(val_low)
        , m_high(val_high)
    {
    }

    ALWAYS_INLINE u8* bytes()
    {
        return m_bytes;
    }
    ALWAYS_INLINE const u8* bytes() const
    {
        return m_bytes;
    }

    ALWAYS_INLINE u16* words()
    {
        return (u16*)m_bytes;
    }
    ALWAYS_INLINE const u16* words() const
    {
        return (const u16*)m_bytes;
    }

    ALWAYS_INLINE u32* double_words()
    {
        return (u32*)m_bytes;
    }
    ALWAYS_INLINE const u32* double_words() const
    {
        return (const u32*)m_bytes;
    }

    ALWAYS_INLINE constexpr u64& low()
    {
        return m_low;
    }
    ALWAYS_INLINE constexpr const u64& low() const
    {
        return m_low;
    }

    ALWAYS_INLINE constexpr u64& high()
    {
        return m_high;
    }
    ALWAYS_INLINE constexpr const u64& high() const
    {
        return m_high;
    }

    // conversion
    template<Unsigned T>
    ALWAYS_INLINE constexpr operator T() const
    {
        return m_low;
    }

    ALWAYS_INLINE constexpr operator bool() const
    {
        return m_low || m_high;
    }

    // comparisons
    template<Unsigned T>
    ALWAYS_INLINE constexpr bool operator==(const T& other) const
    {
        return (!m_high) && m_low == other;
    }
    template<Unsigned T>
    ALWAYS_INLINE constexpr bool operator!=(const T& other) const
    {
        return m_high || m_low != other;
    }
    template<Unsigned T>
    ALWAYS_INLINE constexpr bool operator>(const T& other) const
    {
        return m_high || m_low > other;
    }
    template<Unsigned T>
    ALWAYS_INLINE constexpr bool operator<(const T& other) const
    {
        return !m_high && m_low < other;
    }
    template<Unsigned T>
    ALWAYS_INLINE constexpr bool operator>=(const T& other) const
    {
        return *this == other || *this > other;
    }
    template<Unsigned T>
    ALWAYS_INLINE constexpr bool operator<=(const T& other) const
    {
        return *this == other || *this < other;
    }

    ALWAYS_INLINE constexpr bool operator==(const u128& other) const
    {
        return m_low == other.low() && m_high == other.high();
    }
    ALWAYS_INLINE constexpr bool operator!=(const u128& other) const
    {
        return m_low != other.low() || m_high != other.high();
    }
    ALWAYS_INLINE constexpr bool operator>(const u128& other) const
    {
        return m_high > other.high()
            || (m_high == other.high() && m_low > other.low());
    }
    ALWAYS_INLINE constexpr bool operator<(const u128& other) const
    {
        return m_high < other.high()
            || (m_high == other.high() && m_low < other.low());
    }
    ALWAYS_INLINE constexpr bool operator>=(const u128& other) const
    {
        return *this == other || *this > other;
    }
    ALWAYS_INLINE constexpr bool operator<=(const u128& other) const
    {
        return *this == other || *this < other;
    }

    // bitwise
    template<Unsigned T>
    ALWAYS_INLINE constexpr T operator&(const T& other) const
    {
        return m_low & other;
    }
    template<Unsigned T>
    ALWAYS_INLINE constexpr u128 operator|(const T& other) const
    {
        return { m_low | other, m_high };
    }
    template<Unsigned T>
    ALWAYS_INLINE constexpr u128 operator^(const T& other) const
    {
        return { m_low ^ other, m_high };
    }
    template<Unsigned T>
    ALWAYS_INLINE constexpr u128 operator<<(const T& other) const
    {
        u64 overflow = m_low >> (64 - other);
        return { m_low << other, (m_high << other) | overflow };
    }
    template<Unsigned T>
    ALWAYS_INLINE constexpr u128 operator>>(const T& other) const
    {
        u64 underflow = m_high & other;
        return { (m_low >> other) | (underflow << (64 - other)), m_high >> other };
    }

    ALWAYS_INLINE constexpr u128 operator&(const u128& other) const
    {
        return { m_low & other.low(), m_high & other.high() };
    }
    ALWAYS_INLINE constexpr u128 operator|(const u128& other) const
    {
        return { m_low | other.low(), m_high | other.high() };
    }
    ALWAYS_INLINE constexpr u128 operator^(const u128& other) const
    {
        return { m_low ^ other.low(), m_high ^ other.high() };
    }

    // bitwise assign
    template<Unsigned T>
    constexpr u128& operator&=(const T& other)
    {
        m_high = 0;
        m_low &= other;
        return *this;
    }
    template<Unsigned T>
    constexpr u128& operator|=(const T& other)
    {
        m_low |= other;
        return *this;
    }
    template<Unsigned T>
    constexpr u128& operator^=(const T& other)
    {
        m_low ^= other;
        return *this;
    }
    template<Unsigned T>
    constexpr u128& operator>>=(const T& other)
    {
        *this = *this >> other;
        return *this;
    }
    template<Unsigned T>
    constexpr u128& operator<<=(const T& other)
    {
        *this = *this << other;
        return *this;
    }

    constexpr u128& operator&=(const u128& other)
    {
        m_high &= other.high();
        m_low &= other.low();
        return *this;
    }
    constexpr u128& operator|=(const u128& other)
    {
        m_high |= other.high();
        m_low |= other.low();
        return *this;
    }
    constexpr u128& operator^=(const u128& other)
    {
        m_high ^= other.high();
        m_low ^= other.low();
        return *this;
    }

private:
    union {
        u8 m_bytes[16] = { 0 };
        struct {
            u64 m_low;
            u64 m_high;
        };
    };
};

static_assert(sizeof(u128) == 16);

template<typename T>
concept Unsigned_128 = IsUnsigned<T> || IsSame<T, u128>;

}

using X86::u128;
using X86::Unsigned_128;

template<>
struct AK::Formatter<u128> : StandardFormatter {
    Formatter() = default;
    explicit Formatter(StandardFormatter formatter)
        : StandardFormatter(formatter)
    {
    }

    void format(AK::FormatBuilder&, u128);
};
