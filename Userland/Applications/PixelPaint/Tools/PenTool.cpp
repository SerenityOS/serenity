/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PenTool.h"
#include "../ImageEditor.h"
#include "../Layer.h"
#include <LibGUI/Action.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Label.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Painter.h>
#include <LibGUI/ValueSlider.h>

namespace PixelPaint {

PenTool::PenTool()
{
    set_size(1);
}

void PenTool::draw_point(Gfx::Bitmap& bitmap, Gfx::Color const& color, Gfx::IntPoint const& point)
{
    GUI::Painter painter(bitmap);
    painter.draw_line(point, point, color, size());
}

void PenTool::draw_line(Gfx::Bitmap& bitmap, Gfx::Color const& color, Gfx::IntPoint const& start, Gfx::IntPoint const& end)
{
    GUI::Painter painter(bitmap);
    painter.draw_line(start, end, color, size());
}

GUI::Widget* PenTool::get_properties_widget()
{
    if (!m_properties_widget) {
        m_properties_widget = GUI::Widget::construct();
        m_properties_widget->set_layout<GUI::VerticalBoxLayout>();

        auto& size_container = m_properties_widget->add<GUI::Widget>();
        size_container.set_fixed_height(20);
        size_container.set_layout<GUI::HorizontalBoxLayout>();

        auto& size_label = size_container.add<GUI::Label>("Thickness:");
        size_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        size_label.set_fixed_size(80, 20);

        auto& size_slider = size_container.add<GUI::ValueSlider>(Orientation::Horizontal, "px");
        size_slider.set_range(1, 20);
        size_slider.set_value(size());

        size_slider.on_change = [&](int value) {
            set_size(value);
        };
        set_primary_slider(&size_slider);
    }

    return m_properties_widget.ptr();
}

}
