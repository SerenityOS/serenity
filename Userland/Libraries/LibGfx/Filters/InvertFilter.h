/*
 * Copyright (c) 2021, Musab Kılıç <musabkilic@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>
#include <LibGfx/Filters/ColorFilter.h>

namespace Gfx {

class InvertFilter : public ColorFilter {
public:
    using ColorFilter::ColorFilter;
    virtual ~InvertFilter() = default;

    virtual StringView class_name() const override { return "InvertFilter"sv; }

protected:
    Color convert_color(Color original) override { return original.inverted(); }
};

}
