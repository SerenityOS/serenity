/*
 * Copyright (c) 2022, Xavier Defrang <xavier.defrang@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Sepia.h"
#include "../FilterParams.h"
#include <LibGUI/Label.h>
#include <LibGUI/ValueSlider.h>

namespace PixelPaint::Filters {

void Sepia::apply(Gfx::Bitmap& target_bitmap, Gfx::Bitmap const& source_bitmap) const
{
    Gfx::SepiaFilter filter(m_amount);
    filter.apply(target_bitmap, target_bitmap.rect(), source_bitmap, source_bitmap.rect());
}

ErrorOr<RefPtr<GUI::Widget>> Sepia::get_settings_widget()
{
    if (!m_settings_widget) {
        auto settings_widget = GUI::Widget::construct();
        settings_widget->set_layout<GUI::VerticalBoxLayout>();

        auto& name_label = settings_widget->add<GUI::Label>("Sepia Filter"_string);
        name_label.set_font_weight(Gfx::FontWeight::Bold);
        name_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        name_label.set_fixed_height(20);

        auto& amount_container = settings_widget->add<GUI::Widget>();
        amount_container.set_fixed_height(20);
        amount_container.set_layout<GUI::HorizontalBoxLayout>(GUI::Margins { 4, 0, 4, 0 });

        auto& amount_label = amount_container.add<GUI::Label>("Amount:"_string);
        amount_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        amount_label.set_fixed_size(50, 20);

        auto& amount_slider = amount_container.add<GUI::ValueSlider>(Orientation::Horizontal, "%"_string);
        amount_slider.set_range(0, 100);
        amount_slider.set_value(m_amount * 100);
        amount_slider.on_change = [this](int value) {
            m_amount = value * 0.01f;
            update_preview();
        };
        m_settings_widget = settings_widget;
    }

    return m_settings_widget;
}

}
