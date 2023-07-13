/*
 * Copyright (c) 2022, Xavier Defrang <xavier.defrang@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>
#include <LibGfx/Filters/ColorFilter.h>
#include <math.h>

namespace Gfx {

class SepiaFilter : public ColorFilter {
public:
    using ColorFilter::ColorFilter;
    virtual ~SepiaFilter() = default;

    virtual StringView class_name() const override { return "SepiaFilter"sv; }

    virtual bool amount_handled_in_filter() const override
    {
        return true;
    }

protected:
    Color convert_color(Color original) override { return original.sepia(m_amount); }
};

}
