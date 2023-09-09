/*
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Filters/MatrixFilter.h>

namespace Gfx {

class HueRotateFilter : public MatrixFilter {
public:
    HueRotateFilter(float angle_degrees)
        : MatrixFilter(calculate_hue_rotate_matrix(angle_degrees))
    {
    }

    virtual bool amount_handled_in_filter() const override
    {
        return true;
    }

    virtual StringView class_name() const override { return "HueRotateFilter"sv; }

private:
    static FloatMatrix3x3 calculate_hue_rotate_matrix(float angle_degrees)
    {
        float angle_rads = AK::to_radians(angle_degrees);
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
};

}
