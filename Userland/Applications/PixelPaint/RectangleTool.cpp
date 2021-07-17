/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "RectangleTool.h"
#include "ImageEditor.h"
#include "Layer.h"
#include <LibGUI/Action.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Painter.h>
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

void RectangleTool::on_mousedown(Layer&, GUI::MouseEvent& event, GUI::MouseEvent&)
{
    if (event.button() != GUI::MouseButton::Left && event.button() != GUI::MouseButton::Right)
        return;

    if (m_drawing_button != GUI::MouseButton::None)
        return;

    m_drawing_button = event.button();
    m_rectangle_start_position = event.position();
    m_rectangle_end_position = event.position();
    m_editor->update();
}

void RectangleTool::on_mouseup(Layer& layer, GUI::MouseEvent& event, GUI::MouseEvent&)
{
    if (event.button() == m_drawing_button) {
        GUI::Painter painter(layer.bitmap());
        auto rect = Gfx::IntRect::from_two_points(m_rectangle_start_position, m_rectangle_end_position);
        draw_using(painter, rect);
        m_drawing_button = GUI::MouseButton::None;
        layer.did_modify_bitmap();
        m_editor->did_complete_action();
    }
}

void RectangleTool::on_mousemove(Layer&, GUI::MouseEvent& event, GUI::MouseEvent&)
{
    if (m_drawing_button == GUI::MouseButton::None)
        return;

    m_rectangle_end_position = event.position();
    m_editor->update();
}

void RectangleTool::on_second_paint(Layer const& layer, GUI::PaintEvent& event)
{
    if (m_drawing_button == GUI::MouseButton::None)
        return;

    GUI::Painter painter(*m_editor);
    painter.add_clip_rect(event.rect());
    auto rect = Gfx::IntRect::from_two_points(
        m_editor->layer_position_to_editor_position(layer, m_rectangle_start_position).to_type<int>(),
        m_editor->layer_position_to_editor_position(layer, m_rectangle_end_position).to_type<int>());
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

void RectangleTool::on_tool_button_contextmenu(GUI::ContextMenuEvent& event)
{
    if (!m_context_menu) {
        m_context_menu = GUI::Menu::construct();
        m_context_menu->add_action(GUI::Action::create("Fill", [this](auto&) {
            m_mode = Mode::Fill;
        }));
        m_context_menu->add_action(GUI::Action::create("Outline", [this](auto&) {
            m_mode = Mode::Outline;
        }));
        m_context_menu->add_action(GUI::Action::create("Gradient", [this](auto&) {
            m_mode = Mode::Gradient;
        }));
    }
    m_context_menu->popup(event.screen_position());
}

}
