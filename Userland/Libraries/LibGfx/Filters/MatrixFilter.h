/*
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Math.h>
#include <LibGfx/Filters/ColorFilter.h>
#include <LibGfx/Matrix3x3.h>

namespace Gfx {

class MatrixFilter : public ColorFilter {
public:
    MatrixFilter(FloatMatrix3x3 operation, float amount = 1.0f)
        : ColorFilter(amount)
        , m_operation(operation)
    {
    }

protected:
    Color convert_color(Color original) override
    {
        auto constexpr u8_max = AK::NumericLimits<u8>::max();
        auto safe_float_to_u8 = [](float value) -> u8 {
            return AK::clamp(static_cast<int>(value * u8_max), 0, u8_max);
        };
        FloatVector3 rgb = {
            original.red() / float(u8_max),
            original.green() / float(u8_max),
            original.blue() / float(u8_max),
        };
        rgb = m_operation * rgb;
        return Color {
            safe_float_to_u8(rgb[0]),
            safe_float_to_u8(rgb[1]),
            safe_float_to_u8(rgb[2]),
            original.alpha()
        };
    }

private:
    FloatMatrix3x3 const m_operation;
};

}
