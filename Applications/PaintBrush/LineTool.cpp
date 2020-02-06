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

#include "LineTool.h"
#include "PaintableWidget.h"
#include <LibGUI/GAction.h>
#include <LibGUI/GMenu.h>
#include <LibGUI/GPainter.h>
#include <LibM/math.h>

static Gfx::Point constrain_line_angle(const Gfx::Point& start_pos, const Gfx::Point& end_pos, float angle_increment)
{
    float current_angle = atan2(end_pos.y() - start_pos.y(), end_pos.x() - start_pos.x()) + M_PI * 2.;

    float constrained_angle = ((int)((current_angle + angle_increment / 2.) / angle_increment)) * angle_increment;

    auto diff = end_pos - start_pos;
    float line_length = sqrt(diff.x() * diff.x() + diff.y() * diff.y());

    return { start_pos.x() + (int)(cos(constrained_angle) * line_length),
        start_pos.y() + (int)(sin(constrained_angle) * line_length) };
}

LineTool::LineTool()
{
}

LineTool::~LineTool()
{
}

void LineTool::on_mousedown(GUI::MouseEvent& event)
{
    if (event.button() != GUI::MouseButton::Left && event.button() != GUI::MouseButton::Right)
        return;

    if (m_drawing_button != GUI::MouseButton::None)
        return;

    m_drawing_button = event.button();
    m_line_start_position = event.position();
    m_line_end_position = event.position();
    m_widget->update();
}

void LineTool::on_mouseup(GUI::MouseEvent& event)
{
    if (event.button() == m_drawing_button) {
        GUI::Painter painter(m_widget->bitmap());
        painter.draw_line(m_line_start_position, m_line_end_position, m_widget->color_for(m_drawing_button), m_thickness);
        m_drawing_button = GUI::MouseButton::None;
        m_widget->update();
    }
}

void LineTool::on_mousemove(GUI::MouseEvent& event)
{
    if (m_drawing_button == GUI::MouseButton::None)
        return;

    if (!m_widget->rect().contains(event.position()))
        return;

    if (!m_constrain_angle) {
        m_line_end_position = event.position();
    } else {
        const float ANGLE_STEP = M_PI / 8.0f;
        m_line_end_position = constrain_line_angle(m_line_start_position, event.position(), ANGLE_STEP);
    }
    m_widget->update();
}

void LineTool::on_second_paint(GUI::PaintEvent& event)
{
    if (m_drawing_button == GUI::MouseButton::None)
        return;

    GUI::Painter painter(*m_widget);
    painter.add_clip_rect(event.rect());
    painter.draw_line(m_line_start_position, m_line_end_position, m_widget->color_for(m_drawing_button), m_thickness);
}

void LineTool::on_keydown(GUI::KeyEvent& event)
{
    if (event.key() == Key_Escape && m_drawing_button != GUI::MouseButton::None) {
        m_drawing_button = GUI::MouseButton::None;
        m_widget->update();
        event.accept();
    }

    if (event.key() == Key_Shift) {
        m_constrain_angle = true;
        m_widget->update();
        event.accept();
    }
}

void LineTool::on_keyup(GUI::KeyEvent& event)
{
    if (event.key() == Key_Shift) {
        m_constrain_angle = false;
        m_widget->update();
        event.accept();
    }
}

void LineTool::on_contextmenu(GUI::ContextMenuEvent& event)
{
    if (!m_context_menu) {
        m_context_menu = GUI::Menu::construct();
        m_thickness_actions.set_exclusive(true);
        auto insert_action = [&](int size, bool checked = false) {
            auto action = GUI::Action::create(String::number(size), [this, size](auto& action) {
                m_thickness = size;
                action.set_checked(true);
            });
            action->set_checkable(true);
            action->set_checked(checked);
            m_thickness_actions.add_action(*action);
            m_context_menu->add_action(move(action));
        };
        insert_action(1, true);
        insert_action(2);
        insert_action(3);
        insert_action(4);
    }
    m_context_menu->popup(event.screen_position());
}
