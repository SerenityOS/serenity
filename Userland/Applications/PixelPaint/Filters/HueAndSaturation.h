/*
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "InplaceFilter.h"

namespace PixelPaint::Filters {

class HueAndSaturation final : public InplaceFilter {
public:
    virtual void apply(Gfx::Bitmap& target_bitmap) const override;
    virtual ErrorOr<RefPtr<GUI::Widget>> get_settings_widget() override;

    virtual StringView filter_name() const override { return "Hue/Saturation"sv; }

    HueAndSaturation(ImageEditor* editor)
        : InplaceFilter(editor) {};

private:
    float m_hue { 0 };
    float m_saturation { 0 };
    float m_lightness { 0 };
};

}
