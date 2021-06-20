/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Selection.h"
#include "ImageEditor.h"
#include <LibGfx/Painter.h>

namespace PixelPaint {

constexpr int marching_ant_length = 4;

void Selection::paint(Gfx::Painter& painter)
{
    draw_marching_ants(painter, m_editor.image_rect_to_editor_rect(m_rect).to_type<int>());
}

Selection::Selection(ImageEditor& editor)
    : m_editor(editor)
{
    m_marching_ants_timer = Core::Timer::create_repeating(80, [this] {
        ++m_marching_ants_offset;
        m_marching_ants_offset %= (marching_ant_length * 2);
        if (!is_empty() || m_in_interactive_selection)
            m_editor.update();
    });
    m_marching_ants_timer->start();
}

void Selection::draw_marching_ants(Gfx::Painter& painter, Gfx::IntRect const& rect) const
{
    int offset = m_marching_ants_offset;

    auto draw_pixel = [&](int x, int y) {
        if ((offset % (marching_ant_length * 2)) < marching_ant_length) {
            painter.set_pixel(x, y, Color::Black);
        } else {
            painter.set_pixel(x, y, Color::White);
        }
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

void Selection::clear()
{
    m_rect = {};
    m_editor.update();
}

}
