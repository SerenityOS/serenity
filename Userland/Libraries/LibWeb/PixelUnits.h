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
#include <AK/Traits.h>
#include <LibGfx/Forward.h>
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
    CSSPixels() = default;
    CSSPixels(int value);
    CSSPixels(unsigned int value);
    CSSPixels(unsigned long value);
    CSSPixels(float value);
    CSSPixels(double value);

    static CSSPixels from_raw(int value)
    {
        CSSPixels res;
        res.set_raw_value(value);
        return res;
    }

    float to_float() const;
    double to_double() const;
    int to_int() const;

    inline int raw_value() const { return m_value; }
    inline void set_raw_value(int value) { m_value = value; }

    bool might_be_saturated() const;

    bool operator==(CSSPixels const& other) const;

    explicit operator double() const { return to_double(); }

    CSSPixels& operator++();
    CSSPixels& operator--();

    int operator<=>(CSSPixels const& other) const;

    CSSPixels operator+() const;
    CSSPixels operator-() const;

    CSSPixels operator+(CSSPixels const& other) const;
    CSSPixels operator-(CSSPixels const& other) const;
    CSSPixels operator*(CSSPixels const& other) const;
    CSSPixels operator/(CSSPixels const& other) const;

    CSSPixels& operator+=(CSSPixels const& other);
    CSSPixels& operator-=(CSSPixels const& other);
    CSSPixels& operator*=(CSSPixels const& other);
    CSSPixels& operator/=(CSSPixels const& other);

    CSSPixels abs() const;

private:
    i32 m_value { 0 };
};

inline bool operator==(CSSPixels left, int right) { return left == CSSPixels(right); }
inline bool operator==(CSSPixels left, float right) { return left.to_float() == right; }
inline bool operator==(CSSPixels left, double right) { return left.to_double() == right; }

inline bool operator>(CSSPixels left, int right) { return left > CSSPixels(right); }
inline bool operator>(CSSPixels left, float right) { return left.to_float() > right; }
inline bool operator>(CSSPixels left, double right) { return left.to_double() > right; }

inline bool operator<(CSSPixels left, int right) { return left < CSSPixels(right); }
inline bool operator<(CSSPixels left, float right) { return left.to_float() < right; }
inline bool operator<(CSSPixels left, double right) { return left.to_double() < right; }

inline CSSPixels operator*(CSSPixels left, int right) { return left * CSSPixels(right); }
inline CSSPixels operator*(CSSPixels left, unsigned long right) { return left * CSSPixels(right); }
inline float operator*(CSSPixels left, float right) { return left.to_float() * right; }
inline double operator*(CSSPixels left, double right) { return left.to_double() * right; }

inline CSSPixels operator*(int left, CSSPixels right) { return right * CSSPixels(left); }
inline CSSPixels operator*(unsigned long left, CSSPixels right) { return right * CSSPixels(left); }
inline float operator*(float left, CSSPixels right) { return right.to_float() * left; }
inline double operator*(double left, CSSPixels right) { return right.to_double() * left; }

inline CSSPixels operator/(CSSPixels left, int right) { return left / CSSPixels(right); }
inline CSSPixels operator/(CSSPixels left, unsigned long right) { return left / CSSPixels(right); }
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
    // FIXME: Actually floor value
    return value;
}

constexpr Web::CSSPixels ceil(Web::CSSPixels const& value)
{
    // FIXME: Actually ceil value
    return value;
}

constexpr Web::CSSPixels round(Web::CSSPixels const& value)
{
    // FIXME: Actually round value
    return value;
}

constexpr Web::DevicePixels abs(Web::DevicePixels const& value)
{
    return AK::abs(value.value());
}

namespace AK {

template<>
struct Traits<Web::CSSPixels> : public GenericTraits<Web::CSSPixels> {
    static unsigned hash(Web::CSSPixels const& key)
    {
        VERIFY(isfinite(key.to_double()));
        return Traits<double>::hash(key.to_double());
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
