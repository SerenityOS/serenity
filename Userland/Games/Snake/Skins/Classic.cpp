/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 * Copyright (c) 2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Classic.h"
#include <LibConfig/Client.h>

namespace Snake {

ClassicSkin::ClassicSkin()
{
    m_skin_color = Color::from_argb(Config::read_u32("Snake"sv, "Snake"sv, "BaseColor"sv, m_skin_color.value()));
}

void ClassicSkin::set_skin_color(Color color)
{
    m_skin_color = color;
    Config::write_u32("Snake"sv, "Snake"sv, "BaseColor"sv, color.value());
}

void ClassicSkin::draw_tile_at(Gfx::Painter& painter, Gfx::IntRect const& rect)
{
    painter.fill_rect(rect, m_skin_color.darkened(0.77));

    Gfx::IntRect left_side(rect.x(), rect.y(), 2, rect.height());
    Gfx::IntRect top_side(rect.x(), rect.y(), rect.width(), 2);
    Gfx::IntRect right_side(rect.right() - 1, rect.y(), 2, rect.height());
    Gfx::IntRect bottom_side(rect.x(), rect.bottom() - 1, rect.width(), 2);
    painter.fill_rect(left_side, m_skin_color.darkened(0.88));
    painter.fill_rect(right_side, m_skin_color.darkened(0.55));
    painter.fill_rect(top_side, m_skin_color.darkened(0.88));
    painter.fill_rect(bottom_side, m_skin_color.darkened(0.55));
}

void ClassicSkin::draw_head(Gfx::Painter& painter, Gfx::IntRect const& head, Gfx::IntRect const&)
{
    draw_tile_at(painter, head);
}
void ClassicSkin::draw_body(Gfx::Painter& painter, Gfx::IntRect const&, Gfx::IntRect const& body, Gfx::IntRect const&)
{
    draw_tile_at(painter, body);
}
void ClassicSkin::draw_tail(Gfx::Painter& painter, Gfx::IntRect const&, Gfx::IntRect const& tail)
{
    draw_tile_at(painter, tail);
}

}
