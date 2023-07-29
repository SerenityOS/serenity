/*
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Math.h>
#include <LibWeb/PixelUnits.h>

namespace Web {

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

}
