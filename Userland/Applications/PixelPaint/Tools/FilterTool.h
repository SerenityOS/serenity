/*
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Tool.h"
#include <AK/OwnPtr.h>
#include <AK/Vector.h>
#include <LibGfx/Filters/Filter.h>

namespace PixelPaint {

class FilterTool final : public Tool {
public:
    FilterTool() = default;

    virtual GUI::Widget* get_properties_widget() override;

private:
    RefPtr<GUI::Widget> m_properties_widget;

    enum class FilterType {
        Unselected,
        LaplacianCardinal,
        LaplacianDiagonal,
        Gauss3,
        Gauss5,
        BoxBlur3,
        BoxBlur5,
        Sharpen,
        Grayscale,
        Invert,
        __Count,
    };

    FilterType m_selected_filter { FilterType::Unselected };

    Vector<String> m_filter_names;
    Vector<OwnPtr<Gfx::Filter>> m_filters;
    Vector<OwnPtr<Gfx::Filter::Parameters>> m_filter_parameters;
};
}
