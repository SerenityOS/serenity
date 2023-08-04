/*
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Math.h>
#include <LibWeb/PixelUnits.h>

namespace Web {

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

bool CSSPixels::operator==(CSSPixels const& other) const = default;

CSSPixels& CSSPixels::operator++()
{
    m_value = Checked<int>::saturating_add(m_value, fixed_point_denominator);
    return *this;
}
CSSPixels& CSSPixels::operator--()
{
    m_value = Checked<int>::saturating_sub(m_value, fixed_point_denominator);
    return *this;
}

int CSSPixels::operator<=>(CSSPixels const& other) const
{
    return raw_value() > other.raw_value() ? 1 : raw_value() < other.raw_value() ? -1
                                                                                 : 0;
}

CSSPixels CSSPixels::operator+() const
{
    return from_raw(+raw_value());
}

CSSPixels CSSPixels::operator-() const
{
    return from_raw(-raw_value());
}

CSSPixels CSSPixels::operator+(CSSPixels const& other) const
{
    return from_raw(Checked<int>::saturating_add(raw_value(), other.raw_value()));
}

CSSPixels CSSPixels::operator-(CSSPixels const& other) const
{
    return from_raw(Checked<int>::saturating_sub(raw_value(), other.raw_value()));
}

CSSPixels CSSPixels::operator*(CSSPixels const& other) const
{
    i64 value = raw_value();
    value *= other.raw_value();

    int int_value = AK::clamp_to_int(value >> fractional_bits);

    // Rounding:
    // If last bit cut off was 1:
    if (value & (1u << (fractional_bits - 1))) {
        // If the bit after was 1 as well
        if (value & (radix_mask >> 2u)) {
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

CSSPixels CSSPixels::operator/(CSSPixels const& other) const
{
    i64 mult = raw_value();
    mult <<= fractional_bits;
    mult /= other.raw_value();

    int int_value = AK::clamp_to_int(mult);
    return from_raw(int_value);
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
    return from_raw(::abs(m_value));
}

}
