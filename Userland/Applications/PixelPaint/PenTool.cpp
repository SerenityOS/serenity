/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PenTool.h"
#include "ImageEditor.h"
#include "Layer.h"
#include <LibGUI/Action.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Label.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Slider.h>

namespace PixelPaint {

PenTool::PenTool()
{
}

PenTool::~PenTool()
{
}

void PenTool::on_mousedown(Layer& layer, GUI::MouseEvent& event, GUI::MouseEvent&)
{
    if (event.button() != GUI::MouseButton::Left && event.button() != GUI::MouseButton::Right)
        return;

    GUI::Painter painter(layer.bitmap());
    painter.draw_line(event.position(), event.position(), m_editor->color_for(event), m_thickness);
    layer.did_modify_bitmap(Gfx::IntRect::centered_on(event.position(), Gfx::IntSize { m_thickness, m_thickness }));
    m_last_drawing_event_position = event.position();
}

void PenTool::on_mouseup(Layer&, GUI::MouseEvent& event, GUI::MouseEvent&)
{
    if (event.button() == GUI::MouseButton::Left || event.button() == GUI::MouseButton::Right) {
        m_last_drawing_event_position = { -1, -1 };
        m_editor->did_complete_action();
    }
}

void PenTool::on_mousemove(Layer& layer, GUI::MouseEvent& event, GUI::MouseEvent&)
{
    if (!(event.buttons() & GUI::MouseButton::Left || event.buttons() & GUI::MouseButton::Right))
        return;
    GUI::Painter painter(layer.bitmap());

    Gfx::IntRect changed_rect;
    if (m_last_drawing_event_position != Gfx::IntPoint(-1, -1)) {
        painter.draw_line(m_last_drawing_event_position, event.position(), m_editor->color_for(event), m_thickness);
        changed_rect = Gfx::IntRect::from_two_points(m_last_drawing_event_position, event.position());
    } else {
        painter.draw_line(event.position(), event.position(), m_editor->color_for(event), m_thickness);
        changed_rect = Gfx::IntRect::from_two_points(event.position(), event.position());
    }
    changed_rect.inflate(m_thickness, m_thickness);
    layer.did_modify_bitmap(changed_rect);

    m_last_drawing_event_position = event.position();
}

GUI::Widget* PenTool::get_properties_widget()
{
    if (!m_properties_widget) {
        m_properties_widget = GUI::Widget::construct();
        m_properties_widget->set_layout<GUI::VerticalBoxLayout>();

        auto& thickness_container = m_properties_widget->add<GUI::Widget>();
        thickness_container.set_fixed_height(20);
        thickness_container.set_layout<GUI::HorizontalBoxLayout>();

        auto& thickness_label = thickness_container.add<GUI::Label>("Thickness:");
        thickness_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        thickness_label.set_fixed_size(80, 20);

        auto& thickness_slider = thickness_container.add<GUI::HorizontalSlider>();
        thickness_slider.set_fixed_height(20);
        thickness_slider.set_range(1, 20);
        thickness_slider.set_value(m_thickness);
        thickness_slider.on_change = [this](int value) {
            m_thickness = value;
        };
    }

    return m_properties_widget.ptr();
}

}
