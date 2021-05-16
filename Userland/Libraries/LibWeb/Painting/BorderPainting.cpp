/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Painter.h>
#include <LibWeb/Painting/BorderPainting.h>
#include <LibWeb/Painting/PaintContext.h>

namespace Web::Painting {

void paint_border(PaintContext& context, BorderEdge edge, const Gfx::FloatRect& rect, const CSS::ComputedValues& style)
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
            p1.translate_by(int_width / 2, int_width / 2);
            p2.translate_by(-int_width / 2, int_width / 2);
            break;
        case BorderEdge::Right:
            p1.translate_by(-int_width / 2, int_width / 2);
            p2.translate_by(-int_width / 2, -int_width / 2);
            break;
        case BorderEdge::Bottom:
            p1.translate_by(int_width / 2, -int_width / 2);
            p2.translate_by(-int_width / 2, -int_width / 2);
            break;
        case BorderEdge::Left:
            p1.translate_by(int_width / 2, int_width / 2);
            p2.translate_by(int_width / 2, -int_width / 2);
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

    bool has_top_left_radius = !style.border_top_left_radius().is_undefined();
    bool has_top_right_radius = !style.border_top_right_radius().is_undefined();
    bool has_bottom_left_radius = !style.border_bottom_left_radius().is_undefined();
    bool has_bottom_right_radius = !style.border_bottom_right_radius().is_undefined();

    switch (edge) {
    case BorderEdge::Top:
        p1_step = has_top_left_radius ? 0 : style.border_left().width / (float)int_width;
        p2_step = has_top_right_radius ? 0 : style.border_right().width / (float)int_width;
        for (int i = 0; i < int_width; ++i) {
            draw_line(p1, p2);
            p1.translate_by(p1_step, 1);
            p2.translate_by(-p2_step, 1);
        }
        break;
    case BorderEdge::Right:
        p1_step = has_top_right_radius ? 0 : style.border_top().width / (float)int_width;
        p2_step = has_bottom_right_radius ? 0 : style.border_bottom().width / (float)int_width;
        for (int i = int_width - 1; i >= 0; --i) {
            draw_line(p1, p2);
            p1.translate_by(-1, p1_step);
            p2.translate_by(-1, -p2_step);
        }
        break;
    case BorderEdge::Bottom:
        p1_step = has_bottom_left_radius ? 0 : style.border_left().width / (float)int_width;
        p2_step = has_bottom_right_radius ? 0 : style.border_right().width / (float)int_width;
        for (int i = int_width - 1; i >= 0; --i) {
            draw_line(p1, p2);
            p1.translate_by(p1_step, -1);
            p2.translate_by(-p2_step, -1);
        }
        break;
    case BorderEdge::Left:
        p1_step = has_top_left_radius ? 0 : style.border_top().width / (float)int_width;
        p2_step = has_bottom_left_radius ? 0 : style.border_bottom().width / (float)int_width;
        for (int i = 0; i < int_width; ++i) {
            draw_line(p1, p2);
            p1.translate_by(1, p1_step);
            p2.translate_by(1, -p2_step);
        }
        break;
    }
}

}
