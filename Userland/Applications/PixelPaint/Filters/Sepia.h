/*
 * Copyright (c) 2022, Xavier Defrang <xavier.defrang@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Filter.h"

namespace PixelPaint::Filters {

class Sepia final : public Filter {
public:
    virtual void apply(Gfx::Bitmap& target_bitmap, Gfx::Bitmap const& source_bitmap) const override;
    virtual ErrorOr<RefPtr<GUI::Widget>> get_settings_widget() override;

    virtual StringView filter_name() const override { return "Sepia"sv; }

    Sepia(ImageEditor* editor)
        : Filter(editor) {};

private:
    float m_amount { 1.0f };
};

}
