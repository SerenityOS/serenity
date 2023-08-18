/*
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 * Copyright (c) 2012-2023, Apple Inc. All rights reserved.
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Concepts.h>
#include <AK/DistinctNumeric.h>
#include <AK/Math.h>
#include <AK/Traits.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Rect.h>
#include <math.h>

namespace Web {

/// DevicePixels: A position or length on the physical display.
AK_TYPEDEF_DISTINCT_NUMERIC_GENERAL(int, DevicePixels, Arithmetic, CastToUnderlying, Comparison, Increment);

template<Integral T>
constexpr bool operator==(DevicePixels left, T right) { return left.value() == right; }

template<Integral T>
constexpr bool operator!=(DevicePixels left, T right) { return left.value() != right; }

template<Integral T>
constexpr bool operator>(DevicePixels left, T right) { return left.value() > right; }

template<Integral T>
constexpr bool operator<(DevicePixels left, T right) { return left.value() < right; }

template<Integral T>
constexpr bool operator>=(DevicePixels left, T right) { return left.value() >= right; }

template<Integral T>
constexpr bool operator<=(DevicePixels left, T right) { return left.value() <= right; }

template<Integral T>
constexpr DevicePixels operator*(DevicePixels left, T right) { return left.value() * right; }
template<Integral T>
constexpr DevicePixels operator*(T left, DevicePixels right) { return right * left; }

template<Integral T>
constexpr DevicePixels operator/(DevicePixels left, T right) { return left.value() / right; }

template<Integral T>
constexpr DevicePixels operator%(DevicePixels left, T right) { return left.value() % right; }

/// CSSPixels: A position or length in CSS "reference pixels", independent of zoom or screen DPI.
/// See https://www.w3.org/TR/css-values-3/#reference-pixel
class CSSPixels {
public:
    static constexpr i32 fractional_bits = 6;
    static constexpr i32 fixed_point_denominator = 1 << fractional_bits;

    static constexpr i32 radix_mask = fixed_point_denominator - 1;

    static constexpr i32 max_integer_value = NumericLimits<int>::max() >> fractional_bits;
    static constexpr i32 min_integer_value = NumericLimits<int>::min() >> fractional_bits;

    constexpr CSSPixels() = default;
    template<Signed I>
    constexpr CSSPixels(I value)
    {
        if (value > max_integer_value) [[unlikely]]
            m_value = NumericLimits<int>::max();
        else if (value < min_integer_value) [[unlikely]]
            m_value = NumericLimits<int>::min();
        else
            m_value = static_cast<int>(value) << fractional_bits;
    }

    CSSPixels(float value)
    {
        if (!isnan(value))
            m_value = AK::clamp_to_int(value * fixed_point_denominator);
    }

    CSSPixels(double value)
    {
        if (!isnan(value))
            m_value = AK::clamp_to_int(value * fixed_point_denominator);
    }

    template<Unsigned U>
    constexpr CSSPixels(U value)
    {
        if (value > max_integer_value) [[unlikely]]
            m_value = NumericLimits<int>::max();
        else
            m_value = static_cast<int>(value) << fractional_bits;
    }

    static constexpr CSSPixels from_raw(int value)
    {
        CSSPixels res;
        res.set_raw_value(value);
        return res;
    }

    static constexpr CSSPixels min()
    {
        return from_raw(NumericLimits<int>::min());
    }

    static constexpr CSSPixels max()
    {
        return from_raw(NumericLimits<int>::max());
    }

    float to_float() const;
    double to_double() const;
    int to_int() const;

    constexpr int raw_value() const { return m_value; }
    constexpr void set_raw_value(int value) { m_value = value; }

    constexpr bool might_be_saturated() const { return raw_value() == NumericLimits<i32>::max() || raw_value() == NumericLimits<i32>::min(); }

    constexpr bool operator==(CSSPixels const& other) const = default;

    explicit operator double() const { return to_double(); }
    explicit operator float() const { return to_float(); }
    explicit operator int() const { return to_int(); }

    constexpr CSSPixels& operator++()
    {
        m_value = Checked<int>::saturating_add(m_value, fixed_point_denominator);
        return *this;
    }
    constexpr CSSPixels& operator--()
    {
        m_value = Checked<int>::saturating_sub(m_value, fixed_point_denominator);
        return *this;
    }

    constexpr int operator<=>(CSSPixels const& other) const
    {
        return raw_value() > other.raw_value()
            ? 1
            : raw_value() < other.raw_value()
            ? -1
            : 0;
    }

    constexpr CSSPixels operator+() const { return from_raw(+raw_value()); }
    constexpr CSSPixels operator-() const { return from_raw(-raw_value()); }

    constexpr CSSPixels operator+(CSSPixels const& other) const
    {
        return from_raw(Checked<int>::saturating_add(raw_value(), other.raw_value()));
    }

    constexpr CSSPixels operator-(CSSPixels const& other) const
    {
        return from_raw(Checked<int>::saturating_sub(raw_value(), other.raw_value()));
    }

    constexpr CSSPixels operator*(CSSPixels const& other) const
    {
        i64 value = raw_value();
        value *= other.raw_value();

        int int_value = AK::clamp_to_int(value >> fractional_bits);

        // Rounding:
        // If last bit cut off was 1:
        if (value & (1u << (fractional_bits - 1))) {
            // If any bit after was 1 as well
            if (value & (radix_mask >> 1u)) {
                // We need to round away from 0
                int_value = Checked<int>::saturating_add(int_value, 1);
            } else {
                // Otherwise we round to the next even value
                // Which means we add the least significant bit of the raw integer value
                int_value = Checked<int>::saturating_add(int_value, int_value & 1);
            }
        }

        return from_raw(int_value);
    }

    constexpr CSSPixels operator/(CSSPixels const& other) const
    {
        i64 mult = raw_value();
        mult <<= fractional_bits;
        mult /= other.raw_value();

        int int_value = AK::clamp_to_int(mult);
        return from_raw(int_value);
    }

    constexpr CSSPixels& operator+=(CSSPixels const& other)
    {
        *this = *this + other;
        return *this;
    }
    constexpr CSSPixels& operator-=(CSSPixels const& other)
    {
        *this = *this - other;
        return *this;
    }
    constexpr CSSPixels& operator*=(CSSPixels const& other)
    {
        *this = *this * other;
        return *this;
    }
    constexpr CSSPixels& operator/=(CSSPixels const& other)
    {
        *this = *this / other;
        return *this;
    }

    constexpr CSSPixels abs() const { return from_raw(::abs(m_value)); }

private:
    i32 m_value { 0 };
};

constexpr bool operator==(CSSPixels left, int right) { return left == CSSPixels(right); }
inline bool operator==(CSSPixels left, float right) { return left.to_float() == right; }
inline bool operator==(CSSPixels left, double right) { return left.to_double() == right; }

constexpr bool operator>(CSSPixels left, int right) { return left > CSSPixels(right); }
inline bool operator>(CSSPixels left, float right) { return left.to_float() > right; }
inline bool operator>(CSSPixels left, double right) { return left.to_double() > right; }

constexpr bool operator<(CSSPixels left, int right) { return left < CSSPixels(right); }
inline bool operator<(CSSPixels left, float right) { return left.to_float() < right; }
inline bool operator<(CSSPixels left, double right) { return left.to_double() < right; }

constexpr CSSPixels operator*(CSSPixels left, int right) { return left * CSSPixels(right); }
constexpr CSSPixels operator*(CSSPixels left, unsigned long right) { return left * CSSPixels(right); }
inline float operator*(CSSPixels left, float right) { return left.to_float() * right; }
inline double operator*(CSSPixels left, double right) { return left.to_double() * right; }

constexpr CSSPixels operator*(int left, CSSPixels right) { return right * CSSPixels(left); }
constexpr CSSPixels operator*(unsigned long left, CSSPixels right) { return right * CSSPixels(left); }
inline float operator*(float left, CSSPixels right) { return right.to_float() * left; }
inline double operator*(double left, CSSPixels right) { return right.to_double() * left; }

constexpr CSSPixels operator/(CSSPixels left, int right) { return left / CSSPixels(right); }
constexpr CSSPixels operator/(CSSPixels left, unsigned long right) { return left / CSSPixels(right); }
inline float operator/(CSSPixels left, float right) { return left.to_float() / right; }
inline double operator/(CSSPixels left, double right) { return left.to_double() / right; }

using CSSPixelLine = Gfx::Line<CSSPixels>;
using CSSPixelPoint = Gfx::Point<CSSPixels>;
using CSSPixelRect = Gfx::Rect<CSSPixels>;
using CSSPixelSize = Gfx::Size<CSSPixels>;

using DevicePixelLine = Gfx::Line<DevicePixels>;
using DevicePixelPoint = Gfx::Point<DevicePixels>;
using DevicePixelRect = Gfx::Rect<DevicePixels>;
using DevicePixelSize = Gfx::Size<DevicePixels>;

}

inline Web::CSSPixels abs(Web::CSSPixels const& value)
{
    return value.abs();
}

constexpr Web::CSSPixels floor(Web::CSSPixels const& value)
{
    return Web::CSSPixels::from_raw(value.raw_value() & ~Web::CSSPixels::radix_mask);
}

constexpr Web::CSSPixels ceil(Web::CSSPixels const& value)
{
    auto floor_value = value.raw_value() & ~Web::CSSPixels::radix_mask;
    auto ceil_value = floor_value + (value.raw_value() & Web::CSSPixels::radix_mask ? Web::CSSPixels::fixed_point_denominator : 0);
    return Web::CSSPixels::from_raw(ceil_value);
}

constexpr Web::CSSPixels round(Web::CSSPixels const& value)
{
    // FIXME: Maybe do this with bit-fiddling instead
    if (value > 0)
        return floor(value + Web::CSSPixels::from_raw(Web::CSSPixels::fixed_point_denominator >> 1 /* 0.5 */));
    return ceil(value - Web::CSSPixels::from_raw(Web::CSSPixels::fixed_point_denominator >> 1 /* 0.5 */));
}

constexpr Web::DevicePixels abs(Web::DevicePixels const& value)
{
    return AK::abs(value.value());
}

template<>
template<>
[[nodiscard]] ALWAYS_INLINE Web::CSSPixelRect Web::CSSPixelRect::to_rounded<Web::CSSPixels>() const
{
    return {
        round(x()),
        round(y()),
        round(width()),
        round(height()),
    };
}

namespace AK {

template<>
struct Traits<Web::CSSPixels> : public GenericTraits<Web::CSSPixels> {
    static unsigned hash(Web::CSSPixels const& key)
    {
        return Traits<int>::hash(key.raw_value());
    }

    static bool equals(Web::CSSPixels const& a, Web::CSSPixels const& b)
    {
        return a == b;
    }
};

template<>
struct Traits<Web::DevicePixels> : public GenericTraits<Web::DevicePixels> {
    static unsigned hash(Web::DevicePixels const& key)
    {
        return Traits<Web::DevicePixels::Type>::hash(key.value());
    }

    static bool equals(Web::DevicePixels const& a, Web::DevicePixels const& b)
    {
        return a == b;
    }
};

template<>
struct Formatter<Web::CSSPixels> : Formatter<double> {
    ErrorOr<void> format(FormatBuilder& builder, Web::CSSPixels const& value)
    {
        return Formatter<double>::format(builder, value.to_double());
    }
};

template<>
struct Formatter<Web::DevicePixels> : Formatter<Web::DevicePixels::Type> {
    ErrorOr<void> format(FormatBuilder& builder, Web::DevicePixels const& value)
    {
        return Formatter<Web::DevicePixels::Type>::format(builder, value.value());
    }
};

}
