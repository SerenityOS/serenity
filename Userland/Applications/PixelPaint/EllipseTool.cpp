/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "EllipseTool.h"
#include "ImageEditor.h"
#include "Layer.h"
#include <LibGUI/Action.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Rect.h>
#include <math.h>

namespace PixelPaint {

EllipseTool::EllipseTool()
{
}

EllipseTool::~EllipseTool()
{
}

void EllipseTool::draw_using(GUI::Painter& painter, Gfx::IntRect const& ellipse_intersecting_rect)
{
    switch (m_mode) {
    case Mode::Outline:
        painter.draw_ellipse_intersecting(ellipse_intersecting_rect, m_editor->color_for(m_drawing_button), m_thickness);
        break;
    case Mode::Fill:
        painter.fill_ellipse(ellipse_intersecting_rect, m_editor->color_for(m_drawing_button));
        break;
    default:
        VERIFY_NOT_REACHED();
    }
}

void EllipseTool::on_mousedown(Layer&, GUI::MouseEvent& event, GUI::MouseEvent&)
{
    if (event.button() != GUI::MouseButton::Left && event.button() != GUI::MouseButton::Right)
        return;

    if (m_drawing_button != GUI::MouseButton::None)
        return;

    m_drawing_button = event.button();
    m_ellipse_start_position = event.position();
    m_ellipse_end_position = event.position();
    m_editor->update();
}

void EllipseTool::on_mouseup(Layer& layer, GUI::MouseEvent& event, GUI::MouseEvent&)
{
    if (event.button() == m_drawing_button) {
        GUI::Painter painter(layer.bitmap());
        draw_using(painter, Gfx::IntRect::from_two_points(m_ellipse_start_position, m_ellipse_end_position));
        m_drawing_button = GUI::MouseButton::None;
        m_editor->update();
        m_editor->did_complete_action();
    }
}

void EllipseTool::on_mousemove(Layer&, GUI::MouseEvent& event, GUI::MouseEvent&)
{
    if (m_drawing_button == GUI::MouseButton::None)
        return;

    m_ellipse_end_position = event.position();
    m_editor->update();
}

void EllipseTool::on_second_paint(Layer const& layer, GUI::PaintEvent& event)
{
    if (m_drawing_button == GUI::MouseButton::None)
        return;

    GUI::Painter painter(*m_editor);
    painter.add_clip_rect(event.rect());
    auto preview_start = m_editor->layer_position_to_editor_position(layer, m_ellipse_start_position).to_type<int>();
    auto preview_end = m_editor->layer_position_to_editor_position(layer, m_ellipse_end_position).to_type<int>();
    draw_using(painter, Gfx::IntRect::from_two_points(preview_start, preview_end));
}

void EllipseTool::on_keydown(GUI::KeyEvent& event)
{
    if (event.key() == Key_Escape && m_drawing_button != GUI::MouseButton::None) {
        m_drawing_button = GUI::MouseButton::None;
        m_editor->update();
        event.accept();
    }
}

void EllipseTool::on_tool_button_contextmenu(GUI::ContextMenuEvent& event)
{
    if (!m_context_menu) {
        m_context_menu = GUI::Menu::construct();
        m_context_menu->add_action(GUI::Action::create("Outline", [this](auto&) {
            m_mode = Mode::Outline;
        }));
        m_context_menu->add_action(GUI::Action::create("Fill", [this](auto&) {
            m_mode = Mode::Fill;
        }));
        m_context_menu->add_separator();
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
