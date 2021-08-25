/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "RectangleTool.h"
#include "ImageEditor.h"
#include "Layer.h"
#include <LibGUI/Action.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Label.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Painter.h>
#include <LibGUI/RadioButton.h>
#include <LibGfx/Rect.h>

namespace PixelPaint {

RectangleTool::RectangleTool()
{
}

RectangleTool::~RectangleTool()
{
}

void RectangleTool::draw_using(GUI::Painter& painter, Gfx::IntRect const& rect)
{
    switch (m_mode) {
    case Mode::Fill:
        painter.fill_rect(rect, m_editor->color_for(m_drawing_button));
        break;
    case Mode::Outline:
        painter.draw_rect(rect, m_editor->color_for(m_drawing_button));
        break;
    case Mode::Gradient:
        painter.fill_rect_with_gradient(rect, m_editor->primary_color(), m_editor->secondary_color());
        break;
    default:
        VERIFY_NOT_REACHED();
    }
}

void RectangleTool::on_mousedown(Layer* layer, MouseEvent& event)
{
    if (!layer)
        return;

    auto& layer_event = event.layer_event();
    if (layer_event.button() != GUI::MouseButton::Left && layer_event.button() != GUI::MouseButton::Right)
        return;

    if (m_drawing_button != GUI::MouseButton::None)
        return;

    m_drawing_button = layer_event.button();
    m_rectangle_start_position = layer_event.position();
    m_rectangle_end_position = layer_event.position();
    m_editor->update();
}

void RectangleTool::on_mouseup(Layer* layer, MouseEvent& event)
{
    if (!layer)
        return;

    if (event.layer_event().button() == m_drawing_button) {
        GUI::Painter painter(layer->bitmap());
        auto rect = Gfx::IntRect::from_two_points(m_rectangle_start_position, m_rectangle_end_position);
        draw_using(painter, rect);
        m_drawing_button = GUI::MouseButton::None;
        layer->did_modify_bitmap();
        m_editor->did_complete_action();
    }
}

void RectangleTool::on_mousemove(Layer* layer, MouseEvent& event)
{
    if (!layer)
        return;

    if (m_drawing_button == GUI::MouseButton::None)
        return;

    m_rectangle_end_position = event.layer_event().position();
    m_editor->update();
}

void RectangleTool::on_second_paint(Layer const* layer, GUI::PaintEvent& event)
{
    if (!layer || m_drawing_button == GUI::MouseButton::None)
        return;

    GUI::Painter painter(*m_editor);
    painter.add_clip_rect(event.rect());
    auto rect = Gfx::IntRect::from_two_points(
        m_editor->layer_position_to_editor_position(*layer, m_rectangle_start_position).to_type<int>(),
        m_editor->layer_position_to_editor_position(*layer, m_rectangle_end_position).to_type<int>());
    draw_using(painter, rect);
}

void RectangleTool::on_keydown(GUI::KeyEvent& event)
{
    if (event.key() == Key_Escape && m_drawing_button != GUI::MouseButton::None) {
        m_drawing_button = GUI::MouseButton::None;
        m_editor->update();
        event.accept();
    }
}

GUI::Widget* RectangleTool::get_properties_widget()
{
    if (!m_properties_widget) {
        m_properties_widget = GUI::Widget::construct();
        m_properties_widget->set_layout<GUI::VerticalBoxLayout>();

        auto& mode_container = m_properties_widget->add<GUI::Widget>();
        mode_container.set_fixed_height(70);
        mode_container.set_layout<GUI::HorizontalBoxLayout>();
        auto& mode_label = mode_container.add<GUI::Label>("Mode:");
        mode_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        mode_label.set_fixed_size(80, 20);

        auto& mode_radio_container = mode_container.add<GUI::Widget>();
        mode_radio_container.set_layout<GUI::VerticalBoxLayout>();
        auto& outline_mode_radio = mode_radio_container.add<GUI::RadioButton>("Outline");
        auto& fill_mode_radio = mode_radio_container.add<GUI::RadioButton>("Fill");
        auto& gradient_mode_radio = mode_radio_container.add<GUI::RadioButton>("Gradient");

        outline_mode_radio.on_checked = [&](bool) {
            m_mode = Mode::Outline;
        };
        fill_mode_radio.on_checked = [&](bool) {
            m_mode = Mode::Fill;
        };
        gradient_mode_radio.on_checked = [&](bool) {
            m_mode = Mode::Gradient;
        };

        outline_mode_radio.set_checked(true);
    }

    return m_properties_widget.ptr();
}

}
