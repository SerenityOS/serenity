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
    draw_marching_ants(painter, m_mask);
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
    // Top line
    for (int x = rect.left(); x <= rect.right(); ++x)
        draw_marching_ants_pixel(painter, x, rect.top());

    // Right line
    for (int y = rect.top() + 1; y <= rect.bottom(); ++y)
        draw_marching_ants_pixel(painter, rect.right(), y);

    // Bottom line
    for (int x = rect.right() - 1; x >= rect.left(); --x)
        draw_marching_ants_pixel(painter, x, rect.bottom());

    // Left line
    for (int y = rect.bottom() - 1; y > rect.top(); --y)
        draw_marching_ants_pixel(painter, rect.left(), y);
}

void Selection::draw_marching_ants(Gfx::Painter& painter, Mask const& mask) const
{
    // If the zoom is < 100%, we can skip pixels to save a lot of time drawing the ants
    int step = max(1, (int)floorf(1.0f / m_editor.scale()));

    // Only check the visible selection area when drawing for performance
    auto rect = m_editor.rect();
    rect = Gfx::enclosing_int_rect(m_editor.frame_to_content_rect(rect));
    rect.inflate(step * 2, step * 2); // prevent borders from having visible ants if the selection extends beyond it

    // Scan the image horizontally to find vertical borders
    for (int y = rect.top(); y <= rect.bottom(); y += step) {

        bool previous_selected = false;
        for (int x = rect.left(); x <= rect.right(); x += step) {
            bool this_selected = mask.get(x, y) > 0;

            if (this_selected != previous_selected) {
                Gfx::IntRect image_pixel { x, y, 1, 1 };
                auto pixel = m_editor.content_to_frame_rect(image_pixel).to_type<int>();
                auto end = max(pixel.top(), pixel.bottom()); // for when the zoom is < 100%

                for (int pixel_y = pixel.top(); pixel_y <= end; pixel_y++) {
                    draw_marching_ants_pixel(painter, pixel.left(), pixel_y);
                }
            }

            previous_selected = this_selected;
        }
    }

    // Scan the image vertically to find horizontal borders
    for (int x = rect.left(); x <= rect.right(); x += step) {

        bool previous_selected = false;
        for (int y = rect.top(); y <= rect.bottom(); y += step) {
            bool this_selected = mask.get(x, y) > 0;

            if (this_selected != previous_selected) {
                Gfx::IntRect image_pixel { x, y, 1, 1 };
                auto pixel = m_editor.content_to_frame_rect(image_pixel).to_type<int>();
                auto end = max(pixel.left(), pixel.right()); // for when the zoom is < 100%

                for (int pixel_x = pixel.left(); pixel_x <= end; pixel_x++) {
                    draw_marching_ants_pixel(painter, pixel_x, pixel.top());
                }
            }

            previous_selected = this_selected;
        }
    }
}

void Selection::clear()
{
    m_mask = {};
    m_editor.update();
}

void Selection::merge(Mask const& mask, MergeMode mode)
{
    switch (mode) {
    case MergeMode::Set:
        m_mask = mask;
        break;
    case MergeMode::Add:
        m_mask.add(mask);
        break;
    case MergeMode::Subtract:
        m_mask.subtract(mask);
        break;
    case MergeMode::Intersect:
        m_mask.intersect(mask);
        break;
    default:
        VERIFY_NOT_REACHED();
    }
}

void Selection::draw_marching_ants_pixel(Gfx::Painter& painter, int x, int y) const
{
    int pattern_index = x + y + m_marching_ants_offset;

    if (pattern_index % (marching_ant_length * 2) < marching_ant_length) {
        painter.set_pixel(x, y, Color::Black);
    } else {
        painter.set_pixel(x, y, Color::White);
    }
}

}
