/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@cs.toronto.edu>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "EraseTool.h"
#include "ImageEditor.h"
#include "Layer.h"
#include <LibGUI/Action.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/Label.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Painter.h>
#include <LibGUI/ValueSlider.h>
#include <LibGfx/Bitmap.h>

namespace PixelPaint {

EraseTool::EraseTool()
{
}

EraseTool::~EraseTool()
{
}

Color EraseTool::color_for(GUI::MouseEvent const&)
{
    if (m_use_secondary_color)
        return m_editor->secondary_color();
    return Color(255, 255, 255, 0);
}

void EraseTool::draw_point(Gfx::Bitmap& bitmap, Gfx::Color const& color, Gfx::IntPoint const& point)
{
    int radius = size() / 2;
    Gfx::IntRect rect { point.x() - radius, point.y() - radius, size(), size() };
    GUI::Painter painter(bitmap);
    painter.clear_rect(rect, color);
}

GUI::Widget* EraseTool::get_properties_widget()
{
    if (!m_properties_widget) {
        m_properties_widget = GUI::Widget::construct();
        m_properties_widget->set_layout<GUI::VerticalBoxLayout>();

        auto& size_container = m_properties_widget->add<GUI::Widget>();
        size_container.set_fixed_height(20);
        size_container.set_layout<GUI::HorizontalBoxLayout>();

        auto& size_label = size_container.add<GUI::Label>("Size:");
        size_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        size_label.set_fixed_size(80, 20);

        auto& size_slider = size_container.add<GUI::ValueSlider>(Orientation::Horizontal, "px");
        size_slider.set_range(1, 100);
        size_slider.set_value(size());

        size_slider.on_change = [&](int value) {
            set_size(value);
        };
        set_primary_slider(&size_slider);

        auto& secondary_color_container = m_properties_widget->add<GUI::Widget>();
        secondary_color_container.set_fixed_height(20);
        secondary_color_container.set_layout<GUI::HorizontalBoxLayout>();

        auto& use_secondary_color_checkbox = secondary_color_container.add<GUI::CheckBox>();
        use_secondary_color_checkbox.set_checked(m_use_secondary_color);
        use_secondary_color_checkbox.set_text("Use secondary color");
        use_secondary_color_checkbox.on_checked = [&](bool checked) {
            m_use_secondary_color = checked;
        };
    }

    return m_properties_widget.ptr();
}

}
