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
#include "ImageEditor.h"
#include "Layer.h"
#include <LibGUI/Action.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Painter.h>
#include <math.h>

namespace PixelPaint {

static Gfx::IntPoint constrain_line_angle(const Gfx::IntPoint& start_pos, const Gfx::IntPoint& end_pos, float angle_increment)
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

void LineTool::on_mousedown(Layer&, GUI::MouseEvent& layer_event, GUI::MouseEvent&)
{
    if (layer_event.button() != GUI::MouseButton::Left && layer_event.button() != GUI::MouseButton::Right)
        return;

    if (m_drawing_button != GUI::MouseButton::None)
        return;

    m_drawing_button = layer_event.button();

    m_line_start_position = layer_event.position();
    m_line_end_position = layer_event.position();

    m_editor->update();
}

void LineTool::on_mouseup(Layer& layer, GUI::MouseEvent& event, GUI::MouseEvent&)
{
    if (event.button() == m_drawing_button) {
        GUI::Painter painter(layer.bitmap());
        painter.draw_line(m_line_start_position, m_line_end_position, m_editor->color_for(m_drawing_button), m_thickness);
        m_drawing_button = GUI::MouseButton::None;
        layer.did_modify_bitmap(*m_editor->image());
    }
}

void LineTool::on_mousemove(Layer&, GUI::MouseEvent& layer_event, GUI::MouseEvent&)
{
    if (m_drawing_button == GUI::MouseButton::None)
        return;

    if (!m_constrain_angle) {
        m_line_end_position = layer_event.position();
    } else {
        const float ANGLE_STEP = M_PI / 8.0f;
        m_line_end_position = constrain_line_angle(m_line_start_position, layer_event.position(), ANGLE_STEP);
    }
    m_editor->update();
}

void LineTool::on_second_paint(const Layer& layer, GUI::PaintEvent& event)
{
    if (m_drawing_button == GUI::MouseButton::None)
        return;

    GUI::Painter painter(*m_editor);
    painter.add_clip_rect(event.rect());
    auto preview_start = m_editor->layer_position_to_editor_position(layer, m_line_start_position).to_type<int>();
    auto preview_end = m_editor->layer_position_to_editor_position(layer, m_line_end_position).to_type<int>();
    painter.draw_line(preview_start, preview_end, m_editor->color_for(m_drawing_button), m_thickness);
}

void LineTool::on_keydown(GUI::KeyEvent& event)
{
    if (event.key() == Key_Escape && m_drawing_button != GUI::MouseButton::None) {
        m_drawing_button = GUI::MouseButton::None;
        m_editor->update();
        event.accept();
    }

    if (event.key() == Key_Shift) {
        m_constrain_angle = true;
        m_editor->update();
        event.accept();
    }
}

void LineTool::on_keyup(GUI::KeyEvent& event)
{
    if (event.key() == Key_Shift) {
        m_constrain_angle = false;
        m_editor->update();
        event.accept();
    }
}

void LineTool::on_tool_button_contextmenu(GUI::ContextMenuEvent& event)
{
    if (!m_context_menu) {
        m_context_menu = GUI::Menu::construct();
        m_thickness_actions.set_exclusive(true);
        auto insert_action = [&](int size, bool checked = false) {
            auto action = GUI::Action::create_checkable(String::number(size), [this, size](auto&) {
                m_thickness = size;
            });
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

}
