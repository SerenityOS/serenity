/*
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "HueAndSaturation.h"
#include "../FilterParams.h"
#include <LibGUI/Label.h>
#include <LibGUI/ValueSlider.h>
#include <LibGfx/Filters/HueRotateFilter.h>
#include <LibGfx/Filters/SaturateFilter.h>
#include <LibGfx/Filters/TintFilter.h>

namespace PixelPaint::Filters {

void HueAndSaturation::apply(Gfx::Bitmap& target_bitmap) const
{
    auto apply_filter = [&](auto filter) {
        filter.apply(target_bitmap, target_bitmap.rect(), target_bitmap, target_bitmap.rect());
    };
    apply_filter(Gfx::HueRotateFilter { m_hue + 360 });
    apply_filter(Gfx::SaturateFilter { m_saturation / 100 + 1 });
    auto lightness = m_lightness / 100;
    apply_filter(lightness < 0
            ? Gfx::TintFilter(Gfx::Color::Black, -lightness)
            : Gfx::TintFilter(Gfx::Color::White, lightness));
}

RefPtr<GUI::Widget> HueAndSaturation::get_settings_widget()
{
    if (!m_settings_widget) {
        m_settings_widget = GUI::Widget::construct();
        m_settings_widget->set_layout<GUI::VerticalBoxLayout>();

        auto add_slider = [&](auto name, int min, int max, auto member) {
            auto& name_label = m_settings_widget->add<GUI::Label>(name);
            name_label.set_font_weight(Gfx::FontWeight::Bold);
            name_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
            name_label.set_fixed_height(20);

            auto& slider = m_settings_widget->add<GUI::ValueSlider>(Orientation::Horizontal);
            slider.set_range(min, max);
            slider.set_value(m_hue);
            slider.on_change = [this, member](int value) {
                this->*member = value;
                update_preview();
            };
        };

        add_slider("Hue", -180, 180, &HueAndSaturation::m_hue);
        add_slider("Saturation", -100, 100, &HueAndSaturation::m_saturation);
        add_slider("Lightness", -100, 100, &HueAndSaturation::m_lightness);
    }

    return m_settings_widget;
}

}
