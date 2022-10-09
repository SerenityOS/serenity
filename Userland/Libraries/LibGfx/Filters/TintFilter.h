/*
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>
#include <LibGfx/Filters/ColorFilter.h>

namespace Gfx {

class TintFilter : public ColorFilter {
public:
    TintFilter(Color color, float amount)
        : ColorFilter(amount)
        , m_color(color)
    {
    }

    virtual StringView class_name() const override { return "TintFilter"sv; }

protected:
    Color convert_color(Color) override
    {
        // Note: ColorFilter will blend by amount
        return m_color;
    };

private:
    Gfx::Color m_color;
};

}
