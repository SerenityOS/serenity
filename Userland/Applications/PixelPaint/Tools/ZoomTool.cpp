/*
 * Copyright (c) 2021-2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ZoomTool.h"
#include "../ImageEditor.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Label.h>
#include <LibGUI/ValueSlider.h>

namespace PixelPaint {

void ZoomTool::on_mousedown(Layer*, MouseEvent& event)
{
    auto& raw_event = event.raw_event();
    if (raw_event.button() != GUI::MouseButton::Primary && raw_event.button() != GUI::MouseButton::Secondary)
        return;

    auto scale_factor = (raw_event.button() == GUI::MouseButton::Primary) ? m_sensitivity : -m_sensitivity;
    auto new_scale = m_editor->scale() * AK::exp2(scale_factor);
    m_editor->scale_centered(new_scale, raw_event.position());
}

NonnullRefPtr<GUI::Widget> ZoomTool::get_properties_widget()
{
    if (!m_properties_widget) {
        auto properties_widget = GUI::Widget::construct();
        properties_widget->set_layout<GUI::VerticalBoxLayout>();

        auto& sensitivity_container = properties_widget->add<GUI::Widget>();
        sensitivity_container.set_fixed_height(20);
        sensitivity_container.set_layout<GUI::HorizontalBoxLayout>();

        auto& sensitivity_label = sensitivity_container.add<GUI::Label>("Sensitivity:"_string);
        sensitivity_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        sensitivity_label.set_fixed_size(80, 20);

        auto& sensitivity_slider = sensitivity_container.add<GUI::ValueSlider>(Orientation::Horizontal, "%"_string);
        sensitivity_slider.set_range(1, 100);
        sensitivity_slider.set_value(100 * m_sensitivity);

        sensitivity_slider.on_change = [this](int value) {
            m_sensitivity = value / 100.0f;
        };
        set_primary_slider(&sensitivity_slider);
        m_properties_widget = properties_widget;
    }

    return *m_properties_widget;
}

}
