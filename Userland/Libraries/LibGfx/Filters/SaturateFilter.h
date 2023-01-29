/*
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Filters/MatrixFilter.h>

namespace Gfx {

class SaturateFilter : public MatrixFilter {
public:
    SaturateFilter(float amount)
        : MatrixFilter(calculate_saturate_matrix(amount))
    {
    }

    virtual bool amount_handled_in_filter() const override
    {
        return true;
    }

    virtual StringView class_name() const override { return "SaturateFilter"sv; }

private:
    static FloatMatrix3x3 calculate_saturate_matrix(float amount)
    {
        // The matrix is taken directly from the SVG filter specification:
        // https://drafts.fxtf.org/filter-effects-1/#feColorMatrixElement
        return FloatMatrix3x3 {
            0.213f + 0.787f * amount, 0.715f - 0.715f * amount, 0.072f - 0.072f * amount,
            0.213f - 0.213f * amount, 0.715f + 0.285f * amount, 0.072f - 0.072f * amount,
            0.213f - 0.213f * amount, 0.715f - 0.715f * amount, 0.072f + 0.928f * amount
        };
    }
};

}
