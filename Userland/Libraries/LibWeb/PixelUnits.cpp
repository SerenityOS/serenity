/*
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Math.h>
#include <LibWeb/PixelUnits.h>

namespace Web {

static i32 const fractional_bits = 6;
static constexpr i32 fixed_point_denominator = 1 << fractional_bits;

CSSPixels::CSSPixels(int value)
{
    m_value = value * fixed_point_denominator;
}

CSSPixels::CSSPixels(unsigned int value)
{
    m_value = value * fixed_point_denominator;
}

CSSPixels::CSSPixels(unsigned long value)
{
    m_value = value * fixed_point_denominator;
}

CSSPixels::CSSPixels(float value)
{
    if (!isnan(value))
        m_value = AK::clamp_to_int(value * fixed_point_denominator);
}

CSSPixels::CSSPixels(double value)
{
    if (!isnan(value))
        m_value = AK::clamp_to_int(value * fixed_point_denominator);
}

float CSSPixels::to_float() const
{
    return static_cast<float>(m_value) / fixed_point_denominator;
}

double CSSPixels::to_double() const
{
    return static_cast<double>(m_value) / fixed_point_denominator;
}

int CSSPixels::to_int() const
{
    return m_value / fixed_point_denominator;
}

bool CSSPixels::might_be_saturated() const
{
    return raw_value() == NumericLimits<i32>::max() || raw_value() == NumericLimits<i32>::min();
}

bool CSSPixels::operator==(CSSPixels const& other) const
{
    return raw_value() == other.raw_value();
}

CSSPixels& CSSPixels::operator++()
{
    m_value += fixed_point_denominator;
    return *this;
}
CSSPixels& CSSPixels::operator--()
{
    m_value -= fixed_point_denominator;
    return *this;
}

int CSSPixels::operator<=>(CSSPixels const& other) const
{
    return raw_value() > other.raw_value() ? 1 : raw_value() < other.raw_value() ? -1
                                                                                 : 0;
}

CSSPixels CSSPixels::operator+() const
{
    CSSPixels result;
    result.set_raw_value(+raw_value());
    return result;
}

CSSPixels CSSPixels::operator-() const
{
    CSSPixels result;
    result.set_raw_value(-raw_value());
    return result;
}

static inline int saturated_addition(int a, int b)
{
    i32 overflow = (b > 0 && a > NumericLimits<i32>::max() - b);
    i32 underflow = (b < 0 && a < NumericLimits<i32>::min() - b);
    return overflow ? NumericLimits<i32>::max() : (underflow ? NumericLimits<i32>::min() : a + b);
}

CSSPixels CSSPixels::operator+(CSSPixels const& other) const
{
    CSSPixels result;
    result.set_raw_value(saturated_addition(raw_value(), other.raw_value()));
    return result;
}

CSSPixels CSSPixels::operator-(CSSPixels const& other) const
{
    CSSPixels result;
    result.set_raw_value(raw_value() - other.raw_value());
    return result;
}

CSSPixels CSSPixels::operator*(CSSPixels const& other) const
{
    CSSPixels result;
    result.set_raw_value((raw_value() * other.raw_value()) >> fractional_bits);
    return result;
}

CSSPixels CSSPixels::operator/(CSSPixels const& other) const
{
    CSSPixels result;
    result.set_raw_value(static_cast<long long>(fixed_point_denominator) * raw_value() / other.raw_value());
    return result;
}

CSSPixels& CSSPixels::operator+=(CSSPixels const& other)
{
    *this = *this + other;
    return *this;
}

CSSPixels& CSSPixels::operator-=(CSSPixels const& other)
{
    *this = *this - other;
    return *this;
}

CSSPixels& CSSPixels::operator*=(CSSPixels const& other)
{
    *this = *this * other;
    return *this;
}

CSSPixels& CSSPixels::operator/=(CSSPixels const& other)
{
    *this = *this / other;
    return *this;
}

CSSPixels CSSPixels::abs() const
{
    CSSPixels result;
    result.set_raw_value(::abs(m_value));
    return result;
}

float CSSPixels::epsilon()
{
    return 1.0f / fixed_point_denominator;
}

}
