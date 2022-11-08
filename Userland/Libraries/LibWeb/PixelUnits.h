/*
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
class DevicePixels {
public:
    constexpr DevicePixels(int value)
        : m_value(value)
    {
    }

    constexpr int const& value() const { return m_value; }
    constexpr int& value() { return m_value; }
    explicit operator int() const { return value(); }
    explicit operator float() const { return static_cast<float>(value()); }

    constexpr bool operator==(DevicePixels const& other) const { return m_value == other.m_value; }
    template<Arithmetic T>
    constexpr bool operator==(T const& other) const { return m_value == other; }

    constexpr bool operator!=(DevicePixels const& other) const { return m_value != other.m_value; }
    template<Arithmetic T>
    constexpr bool operator!=(T const& other) const { return m_value != other; }

    constexpr bool operator>(DevicePixels const& other) const { return m_value > other.m_value; }
    template<Arithmetic T>
    constexpr bool operator>(T const& other) const { return m_value > other; }

    constexpr bool operator<(DevicePixels const& other) const { return m_value < other.m_value; }
    template<Arithmetic T>
    constexpr bool operator<(T const& other) const { return m_value < other; }

    constexpr bool operator>=(DevicePixels const& other) const { return m_value >= other.m_value; }
    template<Arithmetic T>
    constexpr bool operator>=(T const& other) const { return m_value >= other; }

    constexpr bool operator<=(DevicePixels const& other) const { return m_value <= other.m_value; }
    template<Arithmetic T>
    constexpr bool operator<=(T const& other) const { return m_value <= other; }

    constexpr DevicePixels operator+(DevicePixels const& other) const { return m_value + other.m_value; }
    constexpr DevicePixels operator-(DevicePixels const& other) const { return m_value - other.m_value; }
    constexpr DevicePixels operator+() const { return +m_value; }
    constexpr DevicePixels operator-() const { return -m_value; }
    constexpr DevicePixels operator++() { return ++m_value; }
    constexpr DevicePixels operator++(int) { return m_value++; }
    constexpr DevicePixels operator--() { return --m_value; }
    constexpr DevicePixels operator--(int) { return m_value--; }
    template<Arithmetic T>
    constexpr DevicePixels operator*(T const& other) const { return m_value * other; }
    template<Arithmetic T>
    constexpr DevicePixels operator/(T const& other) const { return m_value / other; }
    constexpr float operator/(DevicePixels const& other) const { return m_value / other.m_value; }
    template<Arithmetic T>
    constexpr DevicePixels operator%(T const& other) const { return m_value % other; }
    constexpr float operator%(DevicePixels const& other) const { return m_value % other.m_value; }

    constexpr DevicePixels& operator+=(DevicePixels const& other)
    {
        m_value += other.m_value;
        return *this;
    }

    constexpr DevicePixels& operator-=(DevicePixels const& other)
    {
        m_value -= other.m_value;
        return *this;
    }

    constexpr DevicePixels& operator*=(DevicePixels const& other)
    {
        m_value *= other.m_value;
        return *this;
    }

    constexpr DevicePixels& operator/=(DevicePixels const& other)
    {
        m_value /= other.m_value;
        return *this;
    }

private:
    int m_value {};
};

ALWAYS_INLINE DevicePixels operator*(float const& a, DevicePixels const& b)
{
    return b * a;
}

/// CSSPixels: A position or length in CSS "reference pixels", independent of zoom or screen DPI.
/// See https://www.w3.org/TR/css-values-3/#reference-pixel
class CSSPixels {
public:
    constexpr CSSPixels() = default;

    template<Arithmetic T>
    constexpr CSSPixels(T value)
        : m_value { static_cast<float>(value) }
    {
    }

    constexpr float const& value() const { return m_value; }
    constexpr float& value() { return m_value; }
    explicit operator float() const { return value(); }

    constexpr bool operator==(CSSPixels const& other) const { return m_value == other.m_value; }
    template<Arithmetic T>
    constexpr bool operator==(T const& other) const { return m_value == other; }

    constexpr bool operator!=(CSSPixels const& other) const { return m_value != other.m_value; }
    template<Arithmetic T>
    constexpr bool operator!=(T const& other) const { return m_value != other; }

    constexpr bool operator>(CSSPixels const& other) const { return m_value > other.m_value; }
    template<Arithmetic T>
    constexpr bool operator>(T const& other) const { return m_value > other; }

    constexpr bool operator<(CSSPixels const& other) const { return m_value < other.m_value; }
    template<Arithmetic T>
    constexpr bool operator<(T const& other) const { return m_value < other; }

    constexpr bool operator>=(CSSPixels const& other) const { return m_value >= other.m_value; }
    template<Arithmetic T>
    constexpr bool operator>=(T const& other) const { return m_value >= other; }

    constexpr bool operator<=(CSSPixels const& other) const { return m_value <= other.m_value; }
    template<Arithmetic T>
    constexpr bool operator<=(T const& other) const { return m_value <= other; }

    constexpr CSSPixels operator+(CSSPixels const& other) const { return m_value + other.m_value; }
    constexpr CSSPixels operator-(CSSPixels const& other) const { return m_value - other.m_value; }
    constexpr CSSPixels operator+() const { return +m_value; }
    constexpr CSSPixels operator-() const { return -m_value; }
    constexpr CSSPixels operator++() { return ++m_value; }
    constexpr CSSPixels operator++(int) { return m_value++; }
    constexpr CSSPixels operator--() { return --m_value; }
    constexpr CSSPixels operator--(int) { return m_value--; }
    template<Arithmetic T>
    constexpr CSSPixels operator*(T const& other) const { return m_value * other; }
    template<Arithmetic T>
    constexpr CSSPixels operator/(T const& other) const { return m_value / other; }
    constexpr float operator/(CSSPixels const& other) const { return m_value / other.m_value; }

    constexpr CSSPixels& operator+=(CSSPixels const& other)
    {
        m_value += other.m_value;
        return *this;
    }

    constexpr CSSPixels& operator-=(CSSPixels const& other)
    {
        m_value -= other.m_value;
        return *this;
    }

    constexpr CSSPixels& operator*=(CSSPixels const& other)
    {
        m_value *= other.m_value;
        return *this;
    }

    constexpr CSSPixels& operator/=(CSSPixels const& other)
    {
        m_value /= other.m_value;
        return *this;
    }

private:
    float m_value {};
};

ALWAYS_INLINE CSSPixels operator*(float const& a, CSSPixels const& b)
{
    return b * a;
}

using CSSPixelLine = Gfx::Line<CSSPixels>;
using CSSPixelPoint = Gfx::Point<CSSPixels>;
using CSSPixelRect = Gfx::Rect<CSSPixels>;
using CSSPixelSize = Gfx::Size<CSSPixels>;

using DevicePixelLine = Gfx::Line<DevicePixels>;
using DevicePixelPoint = Gfx::Point<DevicePixels>;
using DevicePixelRect = Gfx::Rect<DevicePixels>;
using DevicePixelSize = Gfx::Size<DevicePixels>;

}

constexpr Web::CSSPixels floor(Web::CSSPixels const& value)
{
    return ::floorf(value.value());
}

constexpr Web::CSSPixels ceil(Web::CSSPixels const& value)
{
    return ::ceilf(value.value());
}

constexpr Web::CSSPixels round(Web::CSSPixels const& value)
{
    return ::roundf(value.value());
}

constexpr Web::CSSPixels fmod(Web::CSSPixels const& x, Web::CSSPixels const& y)
{
    return ::fmodf(x.value(), y.value());
}

constexpr Web::CSSPixels abs(Web::CSSPixels const& value)
{
    return AK::abs(value.value());
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
        return double_hash(key.value());
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
        return double_hash(key.value());
    }

    static bool equals(Web::DevicePixels const& a, Web::DevicePixels const& b)
    {
        return a == b;
    }
};

template<>
struct Formatter<Web::CSSPixels> : Formatter<float> {
    ErrorOr<void> format(FormatBuilder& builder, Web::CSSPixels const& value)
    {
        return Formatter<float>::format(builder, value.value());
    }
};

template<>
struct Formatter<Web::DevicePixels> : Formatter<float> {
    ErrorOr<void> format(FormatBuilder& builder, Web::DevicePixels const& value)
    {
        return Formatter<float>::format(builder, value.value());
    }
};

}
