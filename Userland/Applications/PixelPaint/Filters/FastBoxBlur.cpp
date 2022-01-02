/*
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FastBoxBlur.h"
#include "../FilterParams.h"
#include <LibGUI/Label.h>
#include <LibGUI/ValueSlider.h>
#include <LibGfx/Filters/FastBoxBlurFilter.h>

namespace PixelPaint::Filters {

void FastBoxBlur::apply() const
{
    if (!m_editor)
        return;
    if (auto* layer = m_editor->active_layer()) {
        Gfx::FastBoxBlurFilter filter(layer->bitmap());

        if (m_approximate_gauss)
            filter.apply_three_passes(m_radius);
        else
            filter.apply_single_pass(m_radius);

        layer->did_modify_bitmap(layer->rect());
        m_editor->did_complete_action();
    }
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
        };
    }

    return m_settings_widget;
}
}
