/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 * Copyright (c) 2023, the SerenityOS developers.
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ClassicSkin.h"

namespace Snake {

ClassicSkin::ClassicSkin(Color color)
    : m_skin_color(color)
{
}

void ClassicSkin::draw_tile_at(Gfx::Painter& painter, Gfx::IntRect const& rect)
{
    painter.fill_rect(rect, m_skin_color.darkened(0.77));

    Gfx::IntRect left_side(rect.x(), rect.y(), 2, rect.height());
    Gfx::IntRect top_side(rect.x(), rect.y(), rect.width(), 2);
    Gfx::IntRect right_side(rect.right() - 2, rect.y(), 2, rect.height());
    Gfx::IntRect bottom_side(rect.x(), rect.bottom() - 2, rect.width(), 2);
    auto top_left_color = m_skin_color.lightened(0.88);
    auto bottom_right_color = m_skin_color.darkened(0.55);
    painter.fill_rect(left_side, top_left_color);
    painter.fill_rect(right_side, bottom_right_color);
    painter.fill_rect(top_side, top_left_color);
    painter.fill_rect(bottom_side, bottom_right_color);
}

void ClassicSkin::draw_head(Gfx::Painter& painter, Gfx::IntRect const& head, Direction)
{
    painter.fill_rect(head, m_skin_color);
}
void ClassicSkin::draw_body(Gfx::Painter& painter, Gfx::IntRect const& rect, Direction, Direction)
{
    draw_tile_at(painter, rect);
}
void ClassicSkin::draw_tail(Gfx::Painter& painter, Gfx::IntRect const& tail, Direction)
{
    draw_tile_at(painter, tail);
}

}
