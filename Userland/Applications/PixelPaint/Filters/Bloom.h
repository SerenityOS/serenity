/*
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Filter.h"

namespace PixelPaint::Filters {

class Bloom final : public Filter {
public:
    virtual void apply(Gfx::Bitmap& target_bitmap, Gfx::Bitmap const& source_bitmap) const override;

    virtual RefPtr<GUI::Widget> get_settings_widget() override;

    virtual StringView filter_name() override { return "Bloom Filter"sv; }

    Bloom(ImageEditor* editor)
        : Filter(editor) {};

private:
    int m_luma_lower { 128 };
    int m_blur_radius { 15 };
};

}
