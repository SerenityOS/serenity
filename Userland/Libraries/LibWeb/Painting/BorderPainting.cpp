/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/AntiAliasingPainter.h>
#include <LibGfx/Painter.h>
#include <LibWeb/Painting/BorderPainting.h>
#include <LibWeb/Painting/PaintContext.h>

namespace Web::Painting {

BorderRadiiData normalized_border_radii_data(Layout::Node const& node, Gfx::FloatRect const& rect, CSS::BorderRadiusData top_left_radius, CSS::BorderRadiusData top_right_radius, CSS::BorderRadiusData bottom_right_radius, CSS::BorderRadiusData bottom_left_radius)
{
    BorderRadiusData bottom_left_radius_px {};
    BorderRadiusData bottom_right_radius_px {};
    BorderRadiusData top_left_radius_px {};
    BorderRadiusData top_right_radius_px {};

    auto width_length = CSS::Length::make_px(rect.width());
    bottom_left_radius_px.horizontal_radius = bottom_left_radius.horizontal_radius.resolved(node, width_length).to_px(node);
    bottom_right_radius_px.horizontal_radius = bottom_right_radius.horizontal_radius.resolved(node, width_length).to_px(node);
    top_left_radius_px.horizontal_radius = top_left_radius.horizontal_radius.resolved(node, width_length).to_px(node);
    top_right_radius_px.horizontal_radius = top_right_radius.horizontal_radius.resolved(node, width_length).to_px(node);

    auto height_length = CSS::Length::make_px(rect.height());
    bottom_left_radius_px.vertical_radius = bottom_left_radius.vertical_radius.resolved(node, height_length).to_px(node);
    bottom_right_radius_px.vertical_radius = bottom_right_radius.vertical_radius.resolved(node, height_length).to_px(node);
    top_left_radius_px.vertical_radius = top_left_radius.vertical_radius.resolved(node, height_length).to_px(node);
    top_right_radius_px.vertical_radius = top_right_radius.vertical_radius.resolved(node, height_length).to_px(node);

    // Scale overlapping curves according to https://www.w3.org/TR/css-backgrounds-3/#corner-overlap
    auto f = 1.0f;
    auto width_reciprocal = 1.0f / rect.width();
    auto height_reciprocal = 1.0f / rect.height();
    f = max(f, width_reciprocal * (top_left_radius_px.horizontal_radius + top_right_radius_px.horizontal_radius));
    f = max(f, height_reciprocal * (top_right_radius_px.vertical_radius + bottom_right_radius_px.vertical_radius));
    f = max(f, width_reciprocal * (bottom_left_radius_px.horizontal_radius + bottom_right_radius_px.horizontal_radius));
    f = max(f, height_reciprocal * (top_left_radius_px.vertical_radius + bottom_left_radius_px.vertical_radius));

    f = 1.0f / f;

    top_left_radius_px.horizontal_radius *= f;
    top_left_radius_px.vertical_radius *= f;
    top_right_radius_px.horizontal_radius *= f;
    top_right_radius_px.vertical_radius *= f;
    bottom_right_radius_px.horizontal_radius *= f;
    bottom_right_radius_px.vertical_radius *= f;
    bottom_left_radius_px.horizontal_radius *= f;
    bottom_left_radius_px.vertical_radius *= f;

    return BorderRadiiData { top_left_radius_px, top_right_radius_px, bottom_right_radius_px, bottom_left_radius_px };
}

void paint_border(PaintContext& context, BorderEdge edge, Gfx::IntRect const& rect, BordersData const& borders_data)
{
    auto const& border_data = [&] {
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
    int int_width = ceil(width);

    struct Points {
        Gfx::IntPoint p1;
        Gfx::IntPoint p2;
    };

    auto points_for_edge = [](BorderEdge edge, Gfx::IntRect const& rect) -> Points {
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
        context.painter().draw_line(p1, p2, color, int_width, gfx_line_style);
        return;
    }

    context.painter().fill_rect(rect, color);
}

void paint_all_borders(PaintContext& context, Gfx::FloatRect const& bordered_rect, BorderRadiiData const& border_radii_data, BordersData const& borders_data)
{
    if (borders_data.top.width <= 0 && borders_data.right.width <= 0 && borders_data.left.width <= 0 && borders_data.bottom.width <= 0)
        return;

    Gfx::IntRect border_rect = bordered_rect.to_rounded<int>();

    auto top_left = border_radii_data.top_left.as_corner();
    auto top_right = border_radii_data.top_right.as_corner();
    auto bottom_right = border_radii_data.bottom_right.as_corner();
    auto bottom_left = border_radii_data.bottom_left.as_corner();

    // Disable border radii if the corresponding borders don't exist:
    if (borders_data.bottom.width <= 0 && borders_data.left.width <= 0)
        bottom_left = { 0, 0 };
    if (borders_data.bottom.width <= 0 && borders_data.right.width <= 0)
        bottom_right = { 0, 0 };
    if (borders_data.top.width <= 0 && borders_data.left.width <= 0)
        top_left = { 0, 0 };
    if (borders_data.top.width <= 0 && borders_data.right.width <= 0)
        top_right = { 0, 0 };

    auto int_width = [&](auto value) -> int {
        return ceil(value);
    };

    Gfx::IntRect top_border_rect = {
        border_rect.x() + top_left.horizontal_radius,
        border_rect.y(),
        border_rect.width() - top_left.horizontal_radius - top_right.horizontal_radius,
        int_width(borders_data.top.width)
    };
    Gfx::IntRect right_border_rect = {
        border_rect.x() + (border_rect.width() - int_width(borders_data.right.width)),
        border_rect.y() + top_right.vertical_radius,
        int_width(borders_data.right.width),
        border_rect.height() - top_right.vertical_radius - bottom_right.vertical_radius
    };
    Gfx::IntRect bottom_border_rect = {
        border_rect.x() + bottom_left.horizontal_radius,
        border_rect.y() + (border_rect.height() - int_width(borders_data.bottom.width)),
        border_rect.width() - bottom_left.horizontal_radius - bottom_right.horizontal_radius,
        int_width(borders_data.bottom.width)
    };
    Gfx::IntRect left_border_rect = {
        border_rect.x(),
        border_rect.y() + top_left.vertical_radius,
        int_width(borders_data.left.width),
        border_rect.height() - top_left.vertical_radius - bottom_left.vertical_radius
    };

    // Avoid overlapping pixels on the edges.
    if (!top_left)
        top_border_rect.shrink(0, 0, 0, left_border_rect.width());
    if (!top_right)
        top_border_rect.shrink(0, right_border_rect.width(), 0, 0);
    if (!bottom_left)
        bottom_border_rect.shrink(0, 0, 0, left_border_rect.width());
    if (!bottom_right)
        bottom_border_rect.shrink(0, right_border_rect.width(), 0, 0);

    auto border_color_no_alpha = borders_data.top.color;
    border_color_no_alpha.set_alpha(255);

    // Paint the strait line part of the border:
    Painting::paint_border(context, Painting::BorderEdge::Top, top_border_rect, borders_data);
    Painting::paint_border(context, Painting::BorderEdge::Right, right_border_rect, borders_data);
    Painting::paint_border(context, Painting::BorderEdge::Bottom, bottom_border_rect, borders_data);
    Painting::paint_border(context, Painting::BorderEdge::Left, left_border_rect, borders_data);

    // Cache the smallest possible bitmap to render just the corners for the border.
    auto expand_width = abs(int_width(borders_data.left.width) - int_width(borders_data.right.width));
    auto expand_height = abs(int_width(borders_data.top.width) - int_width(borders_data.bottom.width));
    Gfx::IntRect corner_mask_rect {
        0, 0,
        max(
            top_left.horizontal_radius + top_right.horizontal_radius + expand_width,
            bottom_left.horizontal_radius + bottom_right.horizontal_radius + expand_height),
        max(
            top_left.vertical_radius + bottom_left.vertical_radius + expand_width,
            top_right.vertical_radius + bottom_right.vertical_radius + expand_height)
    };
    auto allocate_mask_bitmap = [&]() -> RefPtr<Gfx::Bitmap> {
        auto bitmap = Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRA8888, corner_mask_rect.size());
        if (!bitmap.is_error())
            return bitmap.release_value();
        return nullptr;
    };
    static thread_local auto corner_bitmap = allocate_mask_bitmap();

    // Only reallocate the corner bitmap is the existing one is too small.
    // (should mean no more allocations after the first paint -- amortised zero allocations :^))
    Gfx::Painter painter = ({
        Optional<Gfx::Painter> painter;
        if (corner_bitmap && corner_bitmap->rect().contains(corner_mask_rect)) {
            painter = Gfx::Painter { *corner_bitmap };
            painter->clear_rect(corner_mask_rect, Gfx::Color());
        } else {
            corner_bitmap = allocate_mask_bitmap();
            if (!corner_bitmap)
                return dbgln("Failed to allocate border corner bitmap with size {}", corner_mask_rect.size());
            painter = Gfx::Painter { *corner_bitmap };
        }
        *painter;
    });

    Gfx::AntiAliasingPainter aa_painter { painter };

    // Paint a little tile sheet for the corners
    // TODO: Support various line styles on the corners (dotted, dashes, etc)

    // Paint the outer (minimal) corner rounded rectangle:
    aa_painter.fill_rect_with_rounded_corners(corner_mask_rect, border_color_no_alpha, top_left, top_right, bottom_right, bottom_left);

    // Subtract the inner corner rectangle:
    auto inner_corner_mask_rect = corner_mask_rect.shrunken(
        int_width(borders_data.top.width),
        int_width(borders_data.right.width),
        int_width(borders_data.bottom.width),
        int_width(borders_data.left.width));
    auto inner_top_left = top_left;
    auto inner_top_right = top_right;
    auto inner_bottom_right = bottom_right;
    auto inner_bottom_left = bottom_left;
    inner_top_left.horizontal_radius = max(0, inner_top_left.horizontal_radius - int_width(borders_data.left.width));
    inner_top_left.vertical_radius = max(0, inner_top_left.vertical_radius - int_width(borders_data.top.width));
    inner_top_right.horizontal_radius = max(0, inner_top_right.horizontal_radius - int_width(borders_data.right.width));
    inner_top_right.vertical_radius = max(0, inner_top_right.vertical_radius - int_width(borders_data.top.width));
    inner_bottom_right.horizontal_radius = max(0, inner_bottom_right.horizontal_radius - int_width(borders_data.right.width));
    inner_bottom_right.vertical_radius = max(0, inner_bottom_right.vertical_radius - int_width(borders_data.bottom.width));
    inner_bottom_left.horizontal_radius = max(0, inner_bottom_left.horizontal_radius - int_width(borders_data.left.width));
    inner_bottom_left.vertical_radius = max(0, inner_bottom_left.vertical_radius - int_width(borders_data.bottom.width));
    aa_painter.fill_rect_with_rounded_corners(inner_corner_mask_rect, border_color_no_alpha, inner_top_left, inner_top_right, inner_bottom_right, inner_bottom_left, Gfx::AntiAliasingPainter::BlendMode::AlphaSubtract);

    // TODO: Support dual color corners. Other browsers will render a rounded corner between two borders of
    // different colors using both colours, normally split at a 45 degree angle (though the exact angle is interpolated).
    auto blit_corner = [&](Gfx::IntPoint const& position, Gfx::IntRect const& src_rect, Color corner_color) {
        context.painter().blit_filtered(position, *corner_bitmap, src_rect, [&](auto const& corner_pixel) {
            return corner_color.with_alpha((corner_color.alpha() * corner_pixel.alpha()) / 255);
        });
    };

    // Blit the corners into to their corresponding locations:
    if (top_left)
        blit_corner(border_rect.top_left(), top_left.as_rect(), borders_data.top.color);

    if (top_right)
        blit_corner(border_rect.top_right().translated(-top_right.horizontal_radius + 1, 0), top_right.as_rect().translated(corner_mask_rect.width() - top_right.horizontal_radius, 0), borders_data.top.color);

    if (bottom_right)
        blit_corner(border_rect.bottom_right().translated(-bottom_right.horizontal_radius + 1, -bottom_right.vertical_radius + 1), bottom_right.as_rect().translated(corner_mask_rect.width() - bottom_right.horizontal_radius, corner_mask_rect.height() - bottom_right.vertical_radius), borders_data.bottom.color);

    if (bottom_left)
        blit_corner(border_rect.bottom_left().translated(0, -bottom_left.vertical_radius + 1), bottom_left.as_rect().translated(0, corner_mask_rect.height() - bottom_left.vertical_radius), borders_data.bottom.color);
}

}
