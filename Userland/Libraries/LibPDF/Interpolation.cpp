/*
 * Copyright (c) 2022, Rodrigo Tobar <rtobarc@gmail.com>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibPDF/Interpolation.h>

namespace PDF {

static float slope(float x_min, float x_max, float y_min, float y_max)
{
    return (y_max - y_min) / (x_max - x_min);
}

LinearInterpolation1D::LinearInterpolation1D(float x_min, float x_max, float y_min, float y_max)
    : m_x_min(x_min)
    , m_y_min(y_min)
    , m_slope(slope(x_min, x_max, y_min, y_max))
{
}

float LinearInterpolation1D::interpolate(float x) const
{
    return m_y_min + ((x - m_x_min) * m_slope);
}

void LinearInterpolation1D::interpolate(Span<float> const& x, Span<float> y) const
{
    for (size_t i = 0; i < x.size(); ++i) {
        y[i] = interpolate(x[i]);
    }
}

}
