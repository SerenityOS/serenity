/*
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Bloom.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Label.h>
#include <LibGUI/ValueSlider.h>
#include <LibGUI/Widget.h>
#include <LibGfx/BitmapMixer.h>
#include <LibGfx/Filters/FastBoxBlurFilter.h>
#include <LibGfx/Filters/LumaFilter.h>
#include <LibGfx/FontDatabase.h>

namespace PixelPaint::Filters {

void Bloom::apply(Gfx::Bitmap& target_bitmap, Gfx::Bitmap const& source_bitmap) const
{
    auto intermediate_bitmap_or_error = source_bitmap.clone();
    if (intermediate_bitmap_or_error.is_error())
        return;

    auto intermediate_bitmap = intermediate_bitmap_or_error.release_value();

    Gfx::LumaFilter luma_filter(intermediate_bitmap);
    luma_filter.apply(m_luma_lower, 255);

    Gfx::FastBoxBlurFilter blur_filter(intermediate_bitmap);
    blur_filter.apply_three_passes(m_blur_radius);

    Gfx::BitmapMixer mixer(target_bitmap);
    mixer.mix_with(intermediate_bitmap, Gfx::BitmapMixer::MixingMethod::Lightest);
}

RefPtr<GUI::Widget> Bloom::get_settings_widget()
{
    if (!m_settings_widget) {
        m_settings_widget = GUI::Widget::construct();
        m_settings_widget->set_layout<GUI::VerticalBoxLayout>();

        auto& name_label = m_settings_widget->add<GUI::Label>("Bloom Filter");
        name_label.set_font_weight(Gfx::FontWeight::Bold);
        name_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        name_label.set_fixed_height(20);

        auto& luma_lower_container = m_settings_widget->add<GUI::Widget>();
        luma_lower_container.set_fixed_height(50);
        luma_lower_container.set_layout<GUI::VerticalBoxLayout>();
        luma_lower_container.layout()->set_margins({ 4, 0, 4, 0 });

        auto& luma_lower_label = luma_lower_container.add<GUI::Label>("Luma lower bound:");
        luma_lower_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        luma_lower_label.set_fixed_height(20);

        auto& luma_lower_slider = luma_lower_container.add<GUI::ValueSlider>(Orientation::Horizontal);
        luma_lower_slider.set_range(0, 255);
        luma_lower_slider.set_value(m_luma_lower);
        luma_lower_slider.on_change = [&](int value) {
            m_luma_lower = value;
        };

        auto& radius_container = m_settings_widget->add<GUI::Widget>();
        radius_container.set_fixed_height(50);
        radius_container.set_layout<GUI::VerticalBoxLayout>();
        radius_container.layout()->set_margins({ 4, 0, 4, 0 });

        auto& radius_label = radius_container.add<GUI::Label>("Blur Radius:");
        radius_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        radius_label.set_fixed_height(20);

        auto& radius_slider = radius_container.add<GUI::ValueSlider>(Orientation::Horizontal, "px");
        radius_slider.set_range(0, 50);
        radius_slider.set_value(m_blur_radius);
        radius_slider.on_change = [&](int value) {
            m_blur_radius = value;
        };
    }

    return m_settings_widget;
}

}
