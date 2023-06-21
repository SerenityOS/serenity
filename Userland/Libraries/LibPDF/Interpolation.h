/*
 * Copyright (c) 2022, Rodrigo Tobar <rtobarc@gmail.com>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Span.h>
#include <AK/Vector.h>

namespace PDF {

class LinearInterpolation1D {

public:
    LinearInterpolation1D(float x_min, float x_max, float y_min, float y_max);
    float interpolate(float) const;
    void interpolate(Span<float> const& x, Span<float> y) const;

private:
    float m_x_min;
    float m_y_min;
    float m_slope;
};

}
