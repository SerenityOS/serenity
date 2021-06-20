/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "RectangleSelectTool.h"
#include "ImageEditor.h"
#include "Layer.h"
#include <LibGUI/Painter.h>

namespace PixelPaint {

RectangleSelectTool::RectangleSelectTool()
{
}

RectangleSelectTool::~RectangleSelectTool()
{
}

void RectangleSelectTool::on_mousedown(Layer&, GUI::MouseEvent&, GUI::MouseEvent& image_event)
{
    if (image_event.button() != GUI::MouseButton::Left)
        return;

    m_selecting = true;
    m_editor->selection().begin_interactive_selection();

    m_selection_start = image_event.position();
    m_selection_end = image_event.position();
    m_editor->update();
}

void RectangleSelectTool::on_mousemove(Layer&, GUI::MouseEvent&, GUI::MouseEvent& image_event)
{
    if (!m_selecting)
        return;

    if (m_moving_mode != MovingMode::None) {
        auto delta = m_selection_end - image_event.position();
        if (m_moving_mode == MovingMode::MovingOrigin)
            m_selection_start -= delta;
        else if (m_moving_mode == MovingMode::AroundCenter)
            m_selection_start += delta;
    }

    m_selection_end = image_event.position();
    m_editor->update();
}

void RectangleSelectTool::on_mouseup(Layer&, GUI::MouseEvent&, GUI::MouseEvent& image_event)
{
    if (!m_selecting || image_event.button() != GUI::MouseButton::Left)
        return;

    m_selecting = false;
    m_editor->selection().end_interactive_selection();

    m_editor->update();

    auto rect_in_image = Gfx::IntRect::from_two_points(m_selection_start, m_selection_end);
    m_editor->selection().set(rect_in_image);
}

void RectangleSelectTool::on_keydown(GUI::KeyEvent& key_event)
{
    if (key_event.key() == KeyCode::Key_Space)
        m_moving_mode = MovingMode::MovingOrigin;
    else if (key_event.key() == KeyCode::Key_Control)
        m_moving_mode = MovingMode::AroundCenter;
}

void RectangleSelectTool::on_keyup(GUI::KeyEvent& key_event)
{
    if (key_event.key() == KeyCode::Key_Space && m_moving_mode == MovingMode::MovingOrigin)
        m_moving_mode = MovingMode::None;
    else if (key_event.key() == KeyCode::Key_Control && m_moving_mode == MovingMode::AroundCenter)
        m_moving_mode = MovingMode::None;
}

void RectangleSelectTool::on_second_paint(Layer const&, GUI::PaintEvent& event)
{
    if (!m_selecting)
        return;

    GUI::Painter painter(*m_editor);
    painter.add_clip_rect(event.rect());

    auto rect_in_image = Gfx::IntRect::from_two_points(m_selection_start, m_selection_end);
    auto rect_in_editor = m_editor->image_rect_to_editor_rect(rect_in_image);

    m_editor->selection().draw_marching_ants(painter, rect_in_editor.to_type<int>());
}

}
