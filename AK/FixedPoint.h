/*
 * Copyright (c) 2021, Leon Albrecht <leon2002.la@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Concepts.h>
#include <AK/Format.h>
#include <AK/IntegralMath.h>
#include <AK/NumericLimits.h>
#include <AK/Types.h>

#ifndef KERNEL
#    include <AK/Math.h>
#endif

namespace AK {

// FIXME: this always uses round to nearest break-tie to even
// FIXME: use the Integral concept to constrain Underlying
template<size_t precision, typename Underlying>
class FixedPoint {
    using This = FixedPoint<precision, Underlying>;
    constexpr static Underlying radix_mask = (static_cast<Underlying>(1) << precision) - 1;

    template<size_t P, typename U>
    friend class FixedPoint;

public:
    constexpr FixedPoint() = default;
    template<Integral I>
    constexpr FixedPoint(I value)
        : m_value(static_cast<Underlying>(value) << precision)
    {
    }

    template<FloatingPoint F>
    constexpr FixedPoint(F value)
        : m_value(static_cast<Underlying>(value * (static_cast<Underlying>(1) << precision)))
    {
    }

    template<size_t P, typename U>
    explicit constexpr FixedPoint(FixedPoint<P, U> const& other)
        : m_value(other.template cast_to<precision, Underlying>().m_value)
    {
    }

#ifndef KERNEL
    template<FloatingPoint F>
    explicit ALWAYS_INLINE operator F() const
    {
        return (F)m_value * pow<F>(0.5, precision);
    }
#endif

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

    // http://www.claysturner.com/dsp/BinaryLogarithm.pdf
    constexpr This log2() const
    {
        // 0.5
        This b = create_raw(1 << (precision - 1));
        This y = 0;
        This x = *this;

        // FIXME: There's no negative infinity.
        if (x.raw() <= 0)
            return create_raw(NumericLimits<Underlying>::min());

        if (x != 1) {
            i32 shift_amount = AK::log2<Underlying>(x.raw()) - precision;
            if (shift_amount > 0)
                x >>= shift_amount;
            else
                x <<= -shift_amount;
            y += shift_amount;
        }

        for (size_t i = 0; i < precision; ++i) {
            x *= x;
            if (x >= 2) {
                x >>= 1;
                y += b;
            }
            b >>= 1;
        }

        return y;
    }

    constexpr bool signbit() const
    requires(IsSigned<Underlying>)
    {
        return m_value >> (sizeof(Underlying) * 8 - 1);
    }

    constexpr This operator-() const
    requires(IsSigned<Underlying>)
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
    template<Integral I>
    constexpr This operator>>(I other) const
    {
        return create_raw(m_value >> other);
    }
    template<Integral I>
    constexpr This operator<<(I other) const
    {
        return create_raw(m_value << other);
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
    template<Integral I>
    This& operator>>=(I other)
    {
        m_value >>= other;
        return *this;
    }
    template<Integral I>
    This& operator<<=(I other)
    {
        m_value <<= other;
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
        return !(*this <= other);
    }
    template<Integral I>
    bool operator>=(I other) const
    {
        return !(*this < other);
    }
    template<Integral I>
    bool operator<(I other) const
    {
        return (m_value >> precision) < other || m_value < (other << precision);
    }
    template<Integral I>
    bool operator<=(I other) const
    {
        return *this < other || *this == other;
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

    template<size_t P, typename U>
    operator FixedPoint<P, U>() const
    {
        return cast_to<P, U>();
    }

private:
    template<size_t P, typename U>
    constexpr FixedPoint<P, U> cast_to() const
    {
        U raw_value = static_cast<U>(m_value >> precision) << P;
        if constexpr (precision > P)
            raw_value |= (m_value & radix_mask) >> (precision - P);
        else if constexpr (precision < P)
            raw_value |= static_cast<U>(m_value & radix_mask) << (P - precision);
        else
            raw_value |= m_value & radix_mask;

        return FixedPoint<P, U>::create_raw(raw_value);
    }

    static This create_raw(Underlying value)
    {
        This t {};
        t.raw() = value;
        return t;
    }

    Underlying m_value;
};

}

#if USING_AK_GLOBALLY
using AK::FixedPoint;
#endif
