/*
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2022, Mustafa Quraish <mustafa@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FastBoxBlur.h"
#include "../FilterParams.h"
#include <LibGUI/Label.h>
#include <LibGUI/ValueSlider.h>
#include <LibGfx/Filters/FastBoxBlurFilter.h>

namespace PixelPaint::Filters {

void FastBoxBlur::apply(Gfx::Bitmap& target_bitmap, Gfx::Bitmap const& source_bitmap) const
{
    // This filter only works in-place, so if we have different target and source, we first copy over
    // the source bitmap to the target one.
    if (&target_bitmap != &source_bitmap) {
        VERIFY(source_bitmap.size_in_bytes() == target_bitmap.size_in_bytes());
        memcpy(target_bitmap.scanline(0), source_bitmap.scanline(0), source_bitmap.size_in_bytes());
    }

    Gfx::FastBoxBlurFilter filter(target_bitmap);
    if (m_approximate_gauss)
        filter.apply_three_passes(m_radius);
    else
        filter.apply_single_pass(m_radius);
}

RefPtr<GUI::Widget> FastBoxBlur::get_settings_widget()
{
    if (!m_settings_widget) {
        m_settings_widget = GUI::Widget::construct();
        m_settings_widget->set_layout<GUI::VerticalBoxLayout>();

        auto& name_label = m_settings_widget->add<GUI::Label>("Fast Box Blur Filter");
        name_label.set_font_weight(Gfx::FontWeight::Bold);
        name_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        name_label.set_fixed_height(20);

        auto& radius_container = m_settings_widget->add<GUI::Widget>();
        radius_container.set_fixed_height(20);
        radius_container.set_layout<GUI::HorizontalBoxLayout>();
        radius_container.layout()->set_margins({ 4, 0, 4, 0 });

        auto& radius_label = radius_container.add<GUI::Label>("Radius:");
        radius_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        radius_label.set_fixed_size(50, 20);

        auto& radius_slider = radius_container.add<GUI::ValueSlider>(Orientation::Horizontal, "px");
        radius_slider.set_range(0, 25);
        radius_slider.set_value(m_radius);
        radius_slider.on_change = [&](int value) {
            m_radius = value;
            update_preview();
        };

        auto& gaussian_container = m_settings_widget->add<GUI::Widget>();
        gaussian_container.set_fixed_height(20);
        gaussian_container.set_layout<GUI::HorizontalBoxLayout>();
        gaussian_container.layout()->set_margins({ 4, 0, 4, 0 });

        auto& gaussian_checkbox = gaussian_container.add<GUI::CheckBox>("Approximate Gaussian Blur");
        gaussian_checkbox.set_checked(m_approximate_gauss);
        gaussian_checkbox.set_tooltip("A real gaussian blur can be approximated by running the box blur multiple times with different weights.");
        gaussian_checkbox.on_checked = [this](bool checked) {
            m_approximate_gauss = checked;
            update_preview();
        };
    }

    return m_settings_widget;
}
}
