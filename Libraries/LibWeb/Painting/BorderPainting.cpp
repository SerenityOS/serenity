/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include <LibGfx/Painter.h>
#include <LibWeb/Painting/BorderPainting.h>
#include <LibWeb/Painting/PaintContext.h>

namespace Web::Painting {

void paint_border(PaintContext& context, BorderEdge edge, const Gfx::FloatRect& rect, const LayoutStyle& style)
{
    const auto& border_data = [&] {
        switch (edge) {
        case BorderEdge::Top:
            return style.border_top();
        case BorderEdge::Right:
            return style.border_right();
        case BorderEdge::Bottom:
            return style.border_bottom();
        default: // BorderEdge::Left:
            return style.border_left();
        }
    }();

    float width = border_data.width;
    if (width <= 0)
        return;

    auto color = border_data.color;
    auto border_style = border_data.line_style;
    int int_width = max((int)width, 1);

    struct Points {
        Gfx::FloatPoint p1;
        Gfx::FloatPoint p2;
    };

    auto points_for_edge = [](BorderEdge edge, const Gfx::FloatRect& rect) -> Points {
        switch (edge) {
        case BorderEdge::Top:
            return { rect.top_left(), rect.top_right() };
        case BorderEdge::Right:
            return { rect.top_right(), rect.bottom_right() };
        case BorderEdge::Bottom:
            return { rect.bottom_left(), rect.bottom_right() };
        default: // Edge::Left
            return { rect.top_left(), rect.bottom_left() };
        }
    };

    auto [p1, p2] = points_for_edge(edge, rect);

    if (border_style == CSS::LineStyle::Inset) {
        auto top_left_color = Color::from_rgb(0x5a5a5a);
        auto bottom_right_color = Color::from_rgb(0x888888);
        color = (edge == BorderEdge::Left || edge == BorderEdge::Top) ? top_left_color : bottom_right_color;
    } else if (border_style == CSS::LineStyle::Outset) {
        auto top_left_color = Color::from_rgb(0x888888);
        auto bottom_right_color = Color::from_rgb(0x5a5a5a);
        color = (edge == BorderEdge::Left || edge == BorderEdge::Top) ? top_left_color : bottom_right_color;
    }

    auto gfx_line_style = Gfx::Painter::LineStyle::Solid;
    if (border_style == CSS::LineStyle::Dotted)
        gfx_line_style = Gfx::Painter::LineStyle::Dotted;
    if (border_style == CSS::LineStyle::Dashed)
        gfx_line_style = Gfx::Painter::LineStyle::Dashed;

    if (gfx_line_style != Gfx::Painter::LineStyle::Solid) {
        switch (edge) {
        case BorderEdge::Top:
            p1.move_by(int_width / 2, int_width / 2);
            p2.move_by(-int_width / 2, int_width / 2);
            break;
        case BorderEdge::Right:
            p1.move_by(-int_width / 2, int_width / 2);
            p2.move_by(-int_width / 2, -int_width / 2);
            break;
        case BorderEdge::Bottom:
            p1.move_by(int_width / 2, -int_width / 2);
            p2.move_by(-int_width / 2, -int_width / 2);
            break;
        case BorderEdge::Left:
            p1.move_by(int_width / 2, int_width / 2);
            p2.move_by(int_width / 2, -int_width / 2);
            break;
        }
        context.painter().draw_line({ (int)p1.x(), (int)p1.y() }, { (int)p2.x(), (int)p2.y() }, color, int_width, gfx_line_style);
        return;
    }

    auto draw_line = [&](auto& p1, auto& p2) {
        context.painter().draw_line({ (int)p1.x(), (int)p1.y() }, { (int)p2.x(), (int)p2.y() }, color, 1, gfx_line_style);
    };

    float p1_step = 0;
    float p2_step = 0;

    switch (edge) {
    case BorderEdge::Top:
        p1_step = style.border_left().width / (float)int_width;
        p2_step = style.border_right().width / (float)int_width;
        for (int i = 0; i < int_width; ++i) {
            draw_line(p1, p2);
            p1.move_by(p1_step, 1);
            p2.move_by(-p2_step, 1);
        }
        break;
    case BorderEdge::Right:
        p1_step = style.border_top().width / (float)int_width;
        p2_step = style.border_bottom().width / (float)int_width;
        for (int i = int_width - 1; i >= 0; --i) {
            draw_line(p1, p2);
            p1.move_by(-1, p1_step);
            p2.move_by(-1, -p2_step);
        }
        break;
    case BorderEdge::Bottom:
        p1_step = style.border_left().width / (float)int_width;
        p2_step = style.border_right().width / (float)int_width;
        for (int i = int_width - 1; i >= 0; --i) {
            draw_line(p1, p2);
            p1.move_by(p1_step, -1);
            p2.move_by(-p2_step, -1);
        }
        break;
    case BorderEdge::Left:
        p1_step = style.border_top().width / (float)int_width;
        p2_step = style.border_bottom().width / (float)int_width;
        for (int i = 0; i < int_width; ++i) {
            draw_line(p1, p2);
            p1.move_by(1, p1_step);
            p2.move_by(1, -p2_step);
        }
        break;
    }
}

}
