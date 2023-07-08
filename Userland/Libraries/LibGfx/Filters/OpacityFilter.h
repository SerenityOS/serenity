/*
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>
#include <LibGfx/Filters/ColorFilter.h>

namespace Gfx {

class OpacityFilter : public ColorFilter {
public:
    using ColorFilter::ColorFilter;
    virtual ~OpacityFilter() = default;

    virtual StringView class_name() const override { return "OpacityFilter"sv; }

    virtual bool amount_handled_in_filter() const override
    {
        return true;
    }

protected:
    Color convert_color(Color original) override
    {
        return original.with_alpha(m_amount * 255);
    }
};

}
