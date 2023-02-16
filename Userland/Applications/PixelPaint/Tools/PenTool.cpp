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

void PenTool::draw_point(Gfx::Bitmap& bitmap, Gfx::Color color, Gfx::IntPoint point)
{
    GUI::Painter painter(bitmap);
    painter.draw_line(point, point, color, size());
}

void PenTool::draw_line(Gfx::Bitmap& bitmap, Gfx::Color color, Gfx::IntPoint start, Gfx::IntPoint end)
{
    GUI::Painter painter(bitmap);
    painter.draw_line(start, end, color, size());
}

ErrorOr<GUI::Widget*> PenTool::get_properties_widget()
{
    if (!m_properties_widget) {
        auto properties_widget = TRY(GUI::Widget::try_create());
        (void)TRY(properties_widget->try_set_layout<GUI::VerticalBoxLayout>());

        auto size_container = TRY(properties_widget->try_add<GUI::Widget>());
        size_container->set_fixed_height(20);
        (void)TRY(size_container->try_set_layout<GUI::HorizontalBoxLayout>());

        auto size_label = TRY(size_container->try_add<GUI::Label>("Thickness:"));
        size_label->set_text_alignment(Gfx::TextAlignment::CenterLeft);
        size_label->set_fixed_size(80, 20);

        auto size_slider = TRY(size_container->try_add<GUI::ValueSlider>(Orientation::Horizontal, String::from_utf8_short_string("px"sv)));
        size_slider->set_range(1, 20);
        size_slider->set_value(size());

        size_slider->on_change = [this](int value) {
            set_size(value);
        };
        set_primary_slider(size_slider);
        m_properties_widget = properties_widget;
    }

    return m_properties_widget.ptr();
}

}
