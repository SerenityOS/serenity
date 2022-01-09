/*
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Filter.h"

namespace PixelPaint::Filters {

class FastBoxBlur final : public Filter {
public:
    virtual void apply(Gfx::Bitmap& target_bitmap, Gfx::Bitmap const& source_bitmap) const override;
    virtual RefPtr<GUI::Widget> get_settings_widget() override;

    virtual StringView filter_name() override { return "Fast Box Blur (& Gauss)"sv; }

    FastBoxBlur(ImageEditor* editor)
        : Filter(editor) {};

private:
    size_t m_radius { 5 };
    bool m_approximate_gauss { false };
};

}
