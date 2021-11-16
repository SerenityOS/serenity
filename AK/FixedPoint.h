/*
 * Copyright (c) 2021, Leon Albrecht <leon2002.la@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Concepts.h>
#include <AK/Format.h>
#include <AK/Math.h>
#include <AK/Types.h>

namespace AK {

// FIXME: this always uses round to nearest break-tie to even
template<size_t precision, Integral Underlying = i32>
class FixedPoint {
    using This = FixedPoint<precision, Underlying>;
    constexpr static Underlying radix_mask = (1 << precision) - 1;

public:
    constexpr FixedPoint() = default;
    template<Integral I>
    constexpr FixedPoint(I value)
        : m_value(value << precision)
    {
    }

    template<FloatingPoint F>
    constexpr FixedPoint(F value)
        : m_value(static_cast<Underlying>(value * (1u << precision)))
    {
    }

    template<FloatingPoint F>
    explicit ALWAYS_INLINE operator F() const
    {
        return (F)m_value * pow<F>(0.5, precision);
    }
    template<Integral I>
    explicit constexpr operator I() const
    {
        I value = m_value >> precision;
        // fract(m_value) >= .5?
        if (m_value & (1u << (precision - 1))) {
            // fract(m_value) > .5?
            if (m_value & (radix_mask >> 2u)) {
                // yes: round "up";
                value += (m_value > 0 ? 1 : -1);
            } else {
                //  no: round to even;
                value += value & 1;
            }
        }
        return value;
    }

    constexpr Underlying raw() const
    {
        return m_value;
    }
    constexpr Underlying& raw()
    {
        return m_value;
    }

    constexpr This fract() const
    {
        return create_raw(m_value & radix_mask);
    }

    constexpr This round() const
    {
        return This { static_cast<Underlying>(*this) };
    }
    constexpr This floor() const
    {
        return create_raw(m_value & ~radix_mask);
    }
    constexpr This ceil() const
    {
        return create_raw((m_value & ~radix_mask)
            + (m_value & radix_mask ? 1 << precision : 0));
    }
    constexpr This trunk() const
    {
        return create_raw((m_value & ~radix_mask)
            + ((m_value & radix_mask)
                    ? (m_value > 0 ? 0 : (1 << precision))
                    : 0));
    }

    constexpr Underlying lround() const { return static_cast<Underlying>(*this); }
    constexpr Underlying lfloor() const { return m_value >> precision; }
    constexpr Underlying lceil() const
    {
        return (m_value >> precision)
            + (m_value & radix_mask ? 1 : 0);
    }
    constexpr Underlying ltrunk() const
    {
        return (m_value >> precision)
            + ((m_value & radix_mask)
                    ? m_value > 0 ? 0 : 1
                    : 0);
    }

    constexpr bool signbit() const requires(IsSigned<Underlying>)
    {
        return m_value >> (sizeof(Underlying) * 8 - 1);
    }

    constexpr This operator-() const requires(IsSigned<Underlying>)
    {
        return create_raw(-m_value);
    }

    constexpr This operator+(This const& other) const
    {
        return create_raw(m_value + other.m_value);
    }
    constexpr This operator-(This const& other) const
    {
        return create_raw(m_value - other.m_value);
    }
    constexpr This operator*(This const& other) const
    {
        // FIXME: Potential Overflow, although result could be represented accurately
        Underlying value = m_value * other.raw();
        This ret {};
        ret.raw() = value >> precision;
        // fract(value) >= .5?
        if (value & (1u << (precision - 1))) {
            // fract(value) > .5?
            if (value & (radix_mask >> 2u)) {
                // yes: round up;
                ret.raw() += (value > 0 ? 1 : -1);
            } else {
                //  no: round to even (aka unset last sigificant bit);
                ret.raw() += m_value & 1;
            }
        }
        return ret;
    }
    constexpr This operator/(This const& other) const
    {
        // FIXME: Better rounding?
        return create_raw((m_value / other.m_value) << (precision));
    }

    template<Integral I>
    constexpr This operator+(I other) const
    {
        return create_raw(m_value + (other << precision));
    }
    template<Integral I>
    constexpr This operator-(I other) const
    {
        return create_raw(m_value - (other << precision));
    }
    template<Integral I>
    constexpr This operator*(I other) const
    {
        return create_raw(m_value * other);
    }
    template<Integral I>
    constexpr This operator/(I other) const
    {
        return create_raw(m_value / other);
    }

    This& operator+=(This const& other)
    {
        m_value += other.raw();
        return *this;
    }
    This& operator-=(This const& other)
    {
        m_value -= other.raw();
        return *this;
    }
    This& operator*=(This const& other)
    {
        Underlying value = m_value * other.raw();
        m_value = value >> precision;
        // fract(value) >= .5?
        if (value & (1u << (precision - 1))) {
            // fract(value) > .5?
            if (value & (radix_mask >> 2u)) {
                // yes: round up;
                m_value += (value > 0 ? 1 : -1);
            } else {
                //  no: round to even (aka unset last sigificant bit);
                m_value += m_value & 1;
            }
        }
        return *this;
    }
    This& operator/=(This const& other)
    {
        // FIXME: See above
        m_value /= other.raw();
        m_value <<= precision;
        return *this;
    }

    template<Integral I>
    This& operator+=(I other)
    {
        m_value += other << precision;
        return *this;
    }
    template<Integral I>
    This& operator-=(I other)
    {
        m_value -= other << precision;
        return *this;
    }
    template<Integral I>
    This& operator*=(I other)
    {
        m_value *= other;
        return *this;
    }
    template<Integral I>
    This& operator/=(I other)
    {
        m_value /= other;
        return *this;
    }

    bool operator==(This const& other) const { return raw() == other.raw(); }
    bool operator!=(This const& other) const { return raw() != other.raw(); }
    bool operator>(This const& other) const { return raw() > other.raw(); }
    bool operator>=(This const& other) const { return raw() >= other.raw(); }
    bool operator<(This const& other) const { return raw() < other.raw(); }
    bool operator<=(This const& other) const { return raw() <= other.raw(); }

    // FIXE: There are probably better ways to do these
    template<Integral I>
    bool operator==(I other) const
    {
        return m_value >> precision == other && !(m_value & radix_mask);
    }
    template<Integral I>
    bool operator!=(I other) const
    {
        return (m_value >> precision) != other || m_value & radix_mask;
    }
    template<Integral I>
    bool operator>(I other) const
    {
        if (m_value > 0)
            return (m_value >> precision) > other || (m_value >> precision == other && (m_value & radix_mask));
        if (other > 0)
            return false;

        return (m_value >> precision) > other || !(m_value >> precision == other && (m_value & radix_mask));
    }
    template<Integral I>
    bool operator>=(I other) const
    {
        if (m_value > 0)
            return (m_value >> precision) >= other || (m_value >> precision == other && (m_value & radix_mask));
        if (other > 0)
            return false;

        return (m_value >> precision) >= other || !(m_value >> precision == other && (m_value & radix_mask));
    }
    template<Integral I>
    bool operator<(I other) const
    {
        if (m_value > 0)
            return (m_value >> precision) < other || !(m_value >> precision == other && (m_value & radix_mask));
        if (other > 0)
            return true;

        return (m_value >> precision) < other || (m_value >> precision == other && (m_value & radix_mask));
    }
    template<Integral I>
    bool operator<=(I other) const
    {
        if (m_value > 0)
            return (m_value >> precision) <= other || !(m_value >> precision == other && (m_value & radix_mask));
        if (other > 0)
            return true;

        return (m_value >> precision) <= other || (m_value >> precision == other && (m_value & radix_mask));
    }

    // Casting from a float should be faster than casting to a float
    template<FloatingPoint F>
    bool operator==(F other) const { return *this == (This)other; }
    template<FloatingPoint F>
    bool operator!=(F other) const { return *this != (This)other; }
    template<FloatingPoint F>
    bool operator>(F other) const { return *this > (This)other; }
    template<FloatingPoint F>
    bool operator>=(F other) const { return *this >= (This)other; }
    template<FloatingPoint F>
    bool operator<(F other) const { return *this < (This)other; }
    template<FloatingPoint F>
    bool operator<=(F other) const { return *this <= (This)other; }

private:
    static This create_raw(Underlying value)
    {
        This t {};
        t.raw() = value;
        return t;
    }

    Underlying m_value;
};

template<size_t precision, Integral Underlying>
struct Formatter<FixedPoint<precision, Underlying>> : StandardFormatter {
    Formatter() = default;
    explicit Formatter(StandardFormatter formatter)
        : StandardFormatter(formatter)
    {
    }

    ErrorOr<void> format(FormatBuilder& builder, FixedPoint<precision, Underlying> value)
    {
        Formatter<double> formatter { *this };
        return formatter.format(builder, (double)value);
    }
};

}

using AK::FixedPoint;
