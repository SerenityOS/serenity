/*
 * Copyright (c) 2021, Musab Kılıç <musabkilic@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Filters/ColorFilter.h>

namespace Gfx {

class InvertFilter : public ColorFilter {
public:
    InvertFilter() { }
    virtual ~InvertFilter() { }

    virtual char const* class_name() const override { return "InvertFilter"; }

protected:
    Color convert_color(Color original) override { return original.inverted(); };
};

}
