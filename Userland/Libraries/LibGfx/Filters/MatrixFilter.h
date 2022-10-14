/*
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

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
        auto safe_float_to_u8 = [](float value) -> u8 {
            return AK::clamp(static_cast<int>(value), 0, AK::NumericLimits<u8>::max());
        };
        FloatVector3 rgb = {
            float(original.red()),
            float(original.green()),
            float(original.blue())
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
