/*
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>
#include <LibGfx/Filters/ColorFilter.h>

namespace Gfx {

class BrightnessFilter : public ColorFilter {
public:
    using ColorFilter::ColorFilter;
    virtual ~BrightnessFilter() = default;

    virtual StringView class_name() const override { return "BrightnessFilter"sv; }

    virtual bool amount_handled_in_filter() const override
    {
        return true;
    }

protected:
    Color convert_color(Color original) override
    {
        auto convert_channel = [&](u8 channel) {
            return static_cast<u8>(clamp(round_to<int>(channel * m_amount), 0, 255));
        };
        return Gfx::Color {
            convert_channel(original.red()),
            convert_channel(original.green()),
            convert_channel(original.blue()),
            original.alpha()
        };
    }
};

}
