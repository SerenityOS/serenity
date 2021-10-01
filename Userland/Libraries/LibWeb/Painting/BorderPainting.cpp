/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Painter.h>
#include <LibWeb/Painting/BorderPainting.h>
#include <LibWeb/Painting/PaintContext.h>

namespace Web::Painting {

BorderRadiusData normalized_border_radius_data(Layout::Node const& node, Gfx::FloatRect const& rect, CSS::Length top_left_radius, CSS::Length top_right_radius, CSS::Length bottom_right_radius, CSS::Length bottom_left_radius)
{
    // FIXME: some values should be relative to the height() if specified, but which? For now, all relative values are relative to the width.
    auto bottom_left_radius_px = bottom_left_radius.resolved_or_zero(node, rect.width()).to_px(node);
    auto bottom_right_radius_px = bottom_right_radius.resolved_or_zero(node, rect.width()).to_px(node);
    auto top_left_radius_px = top_left_radius.resolved_or_zero(node, rect.width()).to_px(node);
    auto top_right_radius_px = top_right_radius.resolved_or_zero(node, rect.width()).to_px(node);

    // Scale overlapping curves according to https://www.w3.org/TR/css-backgrounds-3/#corner-overlap
    auto f = 1.0f;
    f = min(f, rect.width() / (float)(top_left_radius_px + top_right_radius_px));
    f = min(f, rect.height() / (float)(top_right_radius_px + bottom_right_radius_px));
    f = min(f, rect.width() / (float)(bottom_left_radius_px + bottom_right_radius_px));
    f = min(f, rect.height() / (float)(top_left_radius_px + bottom_left_radius_px));

    top_left_radius_px = (int)(top_left_radius_px * f);
    top_right_radius_px = (int)(top_right_radius_px * f);
    bottom_right_radius_px = (int)(bottom_right_radius_px * f);
    bottom_left_radius_px = (int)(bottom_left_radius_px * f);

    return BorderRadiusData { top_left_radius_px, top_right_radius_px, bottom_right_radius_px, bottom_left_radius_px };
}

void paint_border(PaintContext& context, BorderEdge edge, Gfx::FloatRect const& rect, BorderRadiusData const& border_radius_data, BordersData const& borders_data)
{
    const auto& border_data = [&] {
        switch (edge) {
        case BorderEdge::Top:
            return borders_data.top;
        case BorderEdge::Right:
            return borders_data.right;
        case BorderEdge::Bottom:
            return borders_data.bottom;
        default: // BorderEdge::Left:
            return borders_data.left;
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

    bool has_top_left_radius = border_radius_data.top_left > 0;
    bool has_top_right_radius = border_radius_data.top_right > 0;
    bool has_bottom_left_radius = border_radius_data.bottom_left > 0;
    bool has_bottom_right_radius = border_radius_data.bottom_right > 0;

    switch (edge) {
    case BorderEdge::Top:
        p1_step = has_top_left_radius ? 0 : borders_data.left.width / (float)int_width;
        p2_step = has_top_right_radius ? 0 : borders_data.right.width / (float)int_width;
        for (int i = 0; i < int_width; ++i) {
            draw_line(p1, p2);
            p1.translate_by(p1_step, 1);
            p2.translate_by(-p2_step, 1);
        }
        break;
    case BorderEdge::Right:
        p1_step = has_top_right_radius ? 0 : borders_data.top.width / (float)int_width;
        p2_step = has_bottom_right_radius ? 0 : borders_data.bottom.width / (float)int_width;
        for (int i = int_width - 1; i >= 0; --i) {
            draw_line(p1, p2);
            p1.translate_by(-1, p1_step);
            p2.translate_by(-1, -p2_step);
        }
        break;
    case BorderEdge::Bottom:
        p1_step = has_bottom_left_radius ? 0 : borders_data.left.width / (float)int_width;
        p2_step = has_bottom_right_radius ? 0 : borders_data.right.width / (float)int_width;
        for (int i = int_width - 1; i >= 0; --i) {
            draw_line(p1, p2);
            p1.translate_by(p1_step, -1);
            p2.translate_by(-p2_step, -1);
        }
        break;
    case BorderEdge::Left:
        p1_step = has_top_left_radius ? 0 : borders_data.top.width / (float)int_width;
        p2_step = has_bottom_left_radius ? 0 : borders_data.bottom.width / (float)int_width;
        for (int i = 0; i < int_width; ++i) {
            draw_line(p1, p2);
            p1.translate_by(1, p1_step);
            p2.translate_by(1, -p2_step);
        }
        break;
    }
}

void paint_all_borders(PaintContext& context, Gfx::FloatRect const& bordered_rect, BorderRadiusData const& border_radius_data, BordersData const& borders_data)
{
    auto const border_rect = bordered_rect;

    auto const top_left_radius = border_radius_data.top_left;
    auto const top_right_radius = border_radius_data.top_right;
    auto const bottom_right_radius = border_radius_data.bottom_right;
    auto const bottom_left_radius = border_radius_data.bottom_left;

    // FIXME: Support elliptical border radii.

    Gfx::FloatRect top_border_rect = {
        border_rect.x() + top_left_radius,
        border_rect.y(),
        border_rect.width() - top_left_radius - top_right_radius,
        border_rect.height()
    };
    Gfx::FloatRect right_border_rect = {
        border_rect.x(),
        border_rect.y() + top_right_radius,
        border_rect.width(),
        border_rect.height() - top_right_radius - bottom_right_radius
    };
    Gfx::FloatRect bottom_border_rect = {
        border_rect.x() + bottom_left_radius,
        border_rect.y(),
        border_rect.width() - bottom_left_radius - bottom_right_radius,
        border_rect.height()
    };
    Gfx::FloatRect left_border_rect = {
        border_rect.x(),
        border_rect.y() + top_left_radius,
        border_rect.width(),
        border_rect.height() - top_left_radius - bottom_left_radius
    };

    Painting::paint_border(context, Painting::BorderEdge::Top, top_border_rect, border_radius_data, borders_data);
    Painting::paint_border(context, Painting::BorderEdge::Right, right_border_rect, border_radius_data, borders_data);
    Painting::paint_border(context, Painting::BorderEdge::Bottom, bottom_border_rect, border_radius_data, borders_data);
    Painting::paint_border(context, Painting::BorderEdge::Left, left_border_rect, border_radius_data, borders_data);

    // Draws a quarter circle clockwise
    auto draw_quarter_circle = [&](Gfx::FloatPoint const& from, Gfx::FloatPoint const& to, Gfx::Color color, int thickness) {
        Gfx::FloatPoint center = { 0, 0 };
        Gfx::FloatPoint offset = { 0, 0 };
        Gfx::FloatPoint circle_position = { 0, 0 };

        auto radius = fabsf(from.x() - to.x());

        if (from.x() < to.x() && from.y() > to.y()) {
            // top-left
            center.set_x(radius);
            center.set_y(radius);
            offset.set_y(1);
        } else if (from.x() < to.x() && from.y() < to.y()) {
            // top-right
            circle_position.set_x(from.x());
            center.set_y(radius);
            offset.set_x(-1);
            offset.set_y(1);
        } else if (from.x() > to.x() && from.y() < to.y()) {
            // bottom-right
            circle_position.set_x(to.x());
            circle_position.set_y(from.y());
            offset.set_x(-1);
        } else if (from.x() > to.x() && from.y() > to.y()) {
            // bottom-left
            circle_position.set_y(to.y());
            center.set_x(radius);
        } else {
            // You are lying about your intentions of drawing a quarter circle, your coordinates are (partly) the same!
            return;
        }

        Gfx::FloatRect circle_rect = {
            border_rect.x() + circle_position.x(),
            border_rect.y() + circle_position.y(),
            radius,
            radius
        };

        context.painter().draw_circle_arc_intersecting(
            Gfx::enclosing_int_rect(circle_rect),
            (center + offset).to_rounded<int>(),
            radius,
            color,
            thickness);
    };

    // FIXME: Which color to use?
    if (top_left_radius != 0) {
        Gfx::FloatPoint arc_start = { 0, top_left_radius };
        Gfx::FloatPoint arc_end = { top_left_radius, 0 };
        draw_quarter_circle(arc_start, arc_end, borders_data.top.color, borders_data.top.width);
    }

    if (top_right_radius != 0) {
        Gfx::FloatPoint arc_start = { top_left_radius + top_border_rect.width(), 0 };
        Gfx::FloatPoint arc_end = { bordered_rect.width(), top_right_radius };
        draw_quarter_circle(arc_start, arc_end, borders_data.top.color, borders_data.top.width);
    }

    if (bottom_right_radius != 0) {
        Gfx::FloatPoint arc_start = { bordered_rect.width(), top_right_radius + right_border_rect.height() };
        Gfx::FloatPoint arc_end = { bottom_border_rect.width() + bottom_left_radius, bordered_rect.height() };
        draw_quarter_circle(arc_start, arc_end, borders_data.bottom.color, borders_data.bottom.width);
    }

    if (bottom_left_radius != 0) {
        Gfx::FloatPoint arc_start = { bottom_left_radius, bordered_rect.height() };
        Gfx::FloatPoint arc_end = { 0, bordered_rect.height() - bottom_left_radius };
        draw_quarter_circle(arc_start, arc_end, borders_data.bottom.color, borders_data.bottom.width);
    }
}

}
