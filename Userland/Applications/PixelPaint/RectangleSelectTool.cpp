/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "RectangleSelectTool.h"
#include "Image.h"
#include "ImageEditor.h"
#include "Layer.h"
#include <LibCore/Timer.h>
#include <LibGUI/Painter.h>

namespace PixelPaint {

constexpr int marching_ant_length = 4;

RectangleSelectTool::RectangleSelectTool()
{
    m_marching_ants_timer = Core::Timer::create_repeating(80, [this] {
        if (!m_editor)
            return;
        ++m_marching_ants_offset;
        m_marching_ants_offset %= marching_ant_length;
        m_editor->update();
    });
    m_marching_ants_timer->start();
}

RectangleSelectTool::~RectangleSelectTool()
{
}

void RectangleSelectTool::on_mousedown(Layer&, GUI::MouseEvent&, GUI::MouseEvent& image_event)
{
    if (image_event.button() != GUI::MouseButton::Left)
        return;

    m_selecting = true;
    m_selection_start = image_event.position();
    m_selection_end = image_event.position();
    m_editor->update();
}

void RectangleSelectTool::on_mousemove(Layer&, GUI::MouseEvent&, GUI::MouseEvent& image_event)
{
    if (!m_selecting)
        return;

    m_selection_end = image_event.position();
    m_editor->update();
}

void RectangleSelectTool::on_mouseup(Layer&, GUI::MouseEvent&, GUI::MouseEvent& image_event)
{
    if (!m_selecting || image_event.button() != GUI::MouseButton::Left)
        return;

    m_selecting = false;
    m_editor->update();

    auto rect_in_image = Gfx::IntRect::from_two_points(m_selection_start, m_selection_end);
    m_editor->selection().set(rect_in_image);
}

void RectangleSelectTool::draw_marching_ants(Gfx::Painter& painter, Gfx::IntRect const& rect) const
{
    int offset = m_marching_ants_offset;

    auto draw_pixel = [&](int x, int y) {
        if ((offset % marching_ant_length) != 0)
            painter.set_pixel(x, y, Color::Black);
        offset++;
    };

    // Top line
    for (int x = rect.left(); x <= rect.right(); ++x)
        draw_pixel(x, rect.top());

    // Right line
    for (int y = rect.top() + 1; y <= rect.bottom(); ++y)
        draw_pixel(rect.right(), y);

    // Bottom line
    for (int x = rect.right() - 1; x >= rect.left(); --x)
        draw_pixel(x, rect.bottom());

    // Left line
    for (int y = rect.bottom() - 1; y > rect.top(); --y)
        draw_pixel(rect.left(), y);
}

void RectangleSelectTool::on_second_paint(Layer const&, GUI::PaintEvent& event)
{
    if (!m_selecting)
        return;

    GUI::Painter painter(*m_editor);
    painter.add_clip_rect(event.rect());

    auto rect_in_image = Gfx::IntRect::from_two_points(m_selection_start, m_selection_end);
    auto rect_in_editor = m_editor->image_rect_to_editor_rect(rect_in_image);

    draw_marching_ants(painter, rect_in_editor.to_type<int>());
}

}
