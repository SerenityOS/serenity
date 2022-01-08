/*
 * Copyright (c) 2022, Xavier Defrang <xavier.defrang@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StdLibExtras.h>
#include <LibGfx/Filters/ColorFilter.h>
#include <math.h>

namespace Gfx {

class SepiaFilter : public ColorFilter {
public:
    SepiaFilter(float amount = 1.0f)
        : m_amount(amount)
    {
    }
    virtual ~SepiaFilter() { }

    virtual char const* class_name() const override { return "SepiaFilter"; }

protected:
    Color convert_color(Color original) override { return original.sepia(m_amount); };

private:
    float m_amount;
};

}
