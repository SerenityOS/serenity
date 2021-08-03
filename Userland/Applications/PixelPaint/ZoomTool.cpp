/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ZoomTool.h"
#include "ImageEditor.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Label.h>
#include <LibGUI/Slider.h>

namespace PixelPaint {

ZoomTool::ZoomTool()
{
}

ZoomTool::~ZoomTool()
{
}

void ZoomTool::on_mousedown(Layer&, GUI::MouseEvent& event, GUI::MouseEvent&)
{
    if (event.button() != GUI::MouseButton::Left && event.button() != GUI::MouseButton::Right)
        return;

    auto scale_factor = (event.button() == GUI::MouseButton::Left) ? m_sensitivity : -m_sensitivity;
    m_editor->scale_centered_on_position(event.position(), scale_factor);
}

GUI::Widget* ZoomTool::get_properties_widget()
{
    if (!m_properties_widget) {
        m_properties_widget = GUI::Widget::construct();
        m_properties_widget->set_layout<GUI::VerticalBoxLayout>();

        auto& sensitivity_container = m_properties_widget->add<GUI::Widget>();
        sensitivity_container.set_fixed_height(20);
        sensitivity_container.set_layout<GUI::HorizontalBoxLayout>();

        auto& sensitivity_label = sensitivity_container.add<GUI::Label>("Sensitivity:");
        sensitivity_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        sensitivity_label.set_fixed_size(80, 20);

        auto& sensitivity_slider = sensitivity_container.add<GUI::HorizontalSlider>();
        sensitivity_slider.set_fixed_height(20);
        sensitivity_slider.set_range(1, 100);
        sensitivity_slider.set_value(100 * m_sensitivity);
        sensitivity_slider.set_tooltip(String::formatted("{:.2}", (double)m_sensitivity / 100.0));

        sensitivity_slider.on_change = [&](int value) {
            m_sensitivity = (double)value / 100.0;
            sensitivity_slider.set_tooltip(String::formatted("{:.2}", (double)value / 100.0));
        };
    }

    return m_properties_widget.ptr();
}

}
