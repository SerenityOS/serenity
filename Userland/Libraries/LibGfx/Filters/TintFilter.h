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
        , m_color(Color::from_rgb(color.value()))
    {
    }

    virtual bool amount_handled_in_filter() const override { return true; }

    virtual StringView class_name() const override { return "TintFilter"sv; }

protected:
    Color convert_color(Color dest) override
    {
        return Color::from_rgb(dest.value())
            .mixed_with(m_color, m_amount)
            .with_alpha(dest.alpha());
    }

private:
    Gfx::Color m_color;
};

}
