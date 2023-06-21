/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Filter.h"
#include <LibGUI/Widget.h>

namespace PixelPaint::Filters {

class Median : public Filter {
public:
    virtual void apply(Gfx::Bitmap& target_bitmap, Gfx::Bitmap const& source_bitmap) const override;

    virtual ErrorOr<RefPtr<GUI::Widget>> get_settings_widget() override;

    virtual StringView filter_name() const override { return "Median Filter"sv; }

    Median(ImageEditor* editor)
        : Filter(editor)
    {
    }

private:
    unsigned filter_size() const { return m_filter_radius * 2 - 1; }

    unsigned m_filter_radius { 2 };
};

}
