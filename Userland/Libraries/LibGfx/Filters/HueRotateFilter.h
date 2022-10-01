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

class HueRotateFilter : public ColorFilter {
public:
    HueRotateFilter(float angle_degrees)
        : ColorFilter(angle_degrees)
        , m_operation(calculate_operation_matrix(angle_degrees))
    {
    }

    virtual bool amount_handled_in_filter() const override
    {
        return true;
    }

    float angle_degrees() const
    {
        return m_amount;
    }

    virtual StringView class_name() const override { return "HueRotateFilter"sv; }

protected:
    Color convert_color(Color original) override
    {
        FloatVector3 rgb = {
            original.red() / 256.0f,
            original.green() / 256.0f,
            original.blue() / 256.0f,
        };
        rgb = m_operation * rgb;
        auto safe_float_to_u8 = [](float value) -> u8 {
            return static_cast<u8>(AK::clamp(value, 0.0f, 1.0f) * 256);
        };
        return Color {
            safe_float_to_u8(rgb[0]),
            safe_float_to_u8(rgb[1]),
            safe_float_to_u8(rgb[2]),
            original.alpha()
        };
    }

private:
    static FloatMatrix3x3 calculate_operation_matrix(float angle)
    {
        float angle_rads = angle * (AK::Pi<float> / 180);
        float cos_angle = 0;
        float sin_angle = 0;
        AK::sincos(angle_rads, sin_angle, cos_angle);
        // The matrices here are taken directly from the SVG filter specification:
        // https://drafts.fxtf.org/filter-effects-1/#feColorMatrixElement
        // clang-format off
        return FloatMatrix3x3 {
            +0.213, +0.715, +0.072,
            +0.213, +0.715, +0.072,
            +0.213, +0.715, +0.072
        } + cos_angle * FloatMatrix3x3 {
            +0.787, -0.715, -0.072,
            -0.213, +0.285, -0.072,
            -0.213, -0.715, +0.928
        } + sin_angle * FloatMatrix3x3 {
            -0.213, -0.715, +0.928,
            +0.143, +0.140, -0.283,
            -0.787, +0.715, +0.072
        };
        // clang-format on
    }

    FloatMatrix3x3 m_operation;
};

}
