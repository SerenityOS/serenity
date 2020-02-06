/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "RectangleTool.h"
#include "PaintableWidget.h"
#include <LibGfx/Rect.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GMenu.h>
#include <LibGUI/GPainter.h>
#include <LibM/math.h>

RectangleTool::RectangleTool()
{
}

RectangleTool::~RectangleTool()
{
}

void RectangleTool::draw_using(GUI::Painter& painter)
{
    auto rect_to_draw = Gfx::Rect::from_two_points(m_rectangle_start_position, m_rectangle_end_position);
    switch (m_mode) {
    case Mode::Fill:
        painter.fill_rect(rect_to_draw, m_widget->color_for(m_drawing_button));
        break;
    case Mode::Outline:
        painter.draw_rect(rect_to_draw, m_widget->color_for(m_drawing_button));
        break;
    case Mode::Gradient:
        painter.fill_rect_with_gradient(rect_to_draw, m_widget->primary_color(), m_widget->secondary_color());
        break;
    default:
        ASSERT_NOT_REACHED();
    }
}

void RectangleTool::on_mousedown(GUI::MouseEvent& event)
{
    if (event.button() != GUI::MouseButton::Left && event.button() != GUI::MouseButton::Right)
        return;

    if (m_drawing_button != GUI::MouseButton::None)
        return;

    m_drawing_button = event.button();
    m_rectangle_start_position = event.position();
    m_rectangle_end_position = event.position();
    m_widget->update();
}

void RectangleTool::on_mouseup(GUI::MouseEvent& event)
{
    if (event.button() == m_drawing_button) {
        GUI::Painter painter(m_widget->bitmap());
        draw_using(painter);
        m_drawing_button = GUI::MouseButton::None;
        m_widget->update();
    }
}

void RectangleTool::on_mousemove(GUI::MouseEvent& event)
{
    if (m_drawing_button == GUI::MouseButton::None)
        return;

    if (!m_widget->rect().contains(event.position()))
        return;

    m_rectangle_end_position = event.position();
    m_widget->update();
}

void RectangleTool::on_second_paint(GUI::PaintEvent& event)
{
    if (m_drawing_button == GUI::MouseButton::None)
        return;

    GUI::Painter painter(*m_widget);
    painter.add_clip_rect(event.rect());
    draw_using(painter);
}

void RectangleTool::on_keydown(GUI::KeyEvent& event)
{
    if (event.key() == Key_Escape && m_drawing_button != GUI::MouseButton::None) {
        m_drawing_button = GUI::MouseButton::None;
        m_widget->update();
        event.accept();
    }
}

void RectangleTool::on_contextmenu(GUI::ContextMenuEvent& event)
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
