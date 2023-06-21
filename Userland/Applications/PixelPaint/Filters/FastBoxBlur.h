/*
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "InplaceFilter.h"
#include <LibGUI/CheckBox.h>
#include <LibGUI/ValueSlider.h>

namespace PixelPaint::Filters {

class FastBoxBlur final : public InplaceFilter {
public:
    virtual void apply(Gfx::Bitmap& target_bitmap) const override;
    virtual ErrorOr<RefPtr<GUI::Widget>> get_settings_widget() override;

    virtual StringView filter_name() const override { return "Fast Box Blur (& Gauss)"sv; }

    FastBoxBlur(ImageEditor* editor)
        : InplaceFilter(editor) {};

private:
    size_t m_radius { 5 };

    bool m_use_asymmetric_radii { false };
    bool m_use_vector { false };
    size_t m_radius_x { 0 };
    size_t m_radius_y { 0 };
    size_t m_angle { 0 };

    bool m_approximate_gauss { false };

    RefPtr<GUI::Widget> m_radius_container { nullptr };
    RefPtr<GUI::Widget> m_asymmetric_radius_container { nullptr };
    RefPtr<GUI::Widget> m_vector_container { nullptr };
    RefPtr<GUI::CheckBox> m_gaussian_checkbox { nullptr };
    RefPtr<GUI::CheckBox> m_vector_checkbox { nullptr };
    RefPtr<GUI::ValueSlider> m_radius_x_slider { nullptr };
    RefPtr<GUI::ValueSlider> m_radius_y_slider { nullptr };
    RefPtr<GUI::ValueSlider> m_angle_slider { nullptr };
    RefPtr<GUI::ValueSlider> m_magnitude_slider { nullptr };
};

}
