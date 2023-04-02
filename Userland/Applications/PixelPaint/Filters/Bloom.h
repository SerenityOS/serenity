/*
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "InplaceFilter.h"

namespace PixelPaint::Filters {

class Bloom final : public InplaceFilter {
public:
    virtual void apply(Gfx::Bitmap& target_bitmap) const override;

    virtual ErrorOr<RefPtr<GUI::Widget>> get_settings_widget() override;

    virtual StringView filter_name() const override { return "Bloom Filter"sv; }

    Bloom(ImageEditor* editor)
        : InplaceFilter(editor) {};

private:
    int m_luma_lower { 128 };
    int m_blur_radius { 15 };
};

}
