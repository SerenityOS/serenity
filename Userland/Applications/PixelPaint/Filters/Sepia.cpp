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

void Sepia::apply() const
{
    if (!m_editor)
        return;
    if (auto* layer = m_editor->active_layer()) {
        Gfx::SepiaFilter filter(m_amount);
        filter.apply(layer->bitmap(), layer->rect(), layer->bitmap(), layer->rect());
        layer->did_modify_bitmap(layer->rect());
        m_editor->did_complete_action();
    }
}

RefPtr<GUI::Widget> Sepia::get_settings_widget()
{
    if (!m_settings_widget) {
        m_settings_widget = GUI::Widget::construct();
        m_settings_widget->set_layout<GUI::VerticalBoxLayout>();

        auto& name_label = m_settings_widget->add<GUI::Label>("Sepia Filter");
        name_label.set_font_weight(Gfx::FontWeight::Bold);
        name_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        name_label.set_fixed_height(20);

        auto& amount_container = m_settings_widget->add<GUI::Widget>();
        amount_container.set_fixed_height(20);
        amount_container.set_layout<GUI::HorizontalBoxLayout>();
        amount_container.layout()->set_margins({ 4, 0, 4, 0 });

        auto& amount_label = amount_container.add<GUI::Label>("Amount:");
        amount_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        amount_label.set_fixed_size(50, 20);

        auto& amount_slider = amount_container.add<GUI::ValueSlider>(Orientation::Horizontal, "%");
        amount_slider.set_range(0, 100);
        amount_slider.set_value(m_amount * 100);
        amount_slider.on_change = [&](int value) {
            m_amount = value * 0.01f;
        };
    }

    return m_settings_widget;
}

}
