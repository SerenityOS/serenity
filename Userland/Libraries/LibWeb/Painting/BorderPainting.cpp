/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/AntiAliasingPainter.h>
#include <LibGfx/Painter.h>
#include <LibGfx/Path.h>
#include <LibWeb/Painting/BorderPainting.h>
#include <LibWeb/Painting/PaintContext.h>

namespace Web::Painting {

BorderRadiiData normalized_border_radii_data(Layout::Node const& node, CSSPixelRect const& rect, CSS::BorderRadiusData top_left_radius, CSS::BorderRadiusData top_right_radius, CSS::BorderRadiusData bottom_right_radius, CSS::BorderRadiusData bottom_left_radius)
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
    CSSPixels f = 1.0f;
    auto width_reciprocal = 1.0f / rect.width().value();
    auto height_reciprocal = 1.0f / rect.height().value();
    f = max(f, width_reciprocal * (top_left_radius_px.horizontal_radius + top_right_radius_px.horizontal_radius));
    f = max(f, height_reciprocal * (top_right_radius_px.vertical_radius + bottom_right_radius_px.vertical_radius));
    f = max(f, width_reciprocal * (bottom_left_radius_px.horizontal_radius + bottom_right_radius_px.horizontal_radius));
    f = max(f, height_reciprocal * (top_left_radius_px.vertical_radius + bottom_left_radius_px.vertical_radius));

    f = 1.0f / f.value();

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

void paint_border(PaintContext& context, BorderEdge edge, DevicePixelRect const& rect, BorderRadiiData const& border_radii_data, BordersData const& borders_data)
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

    CSSPixels width = border_data.width;
    if (width <= 0)
        return;

    auto color = border_data.color;
    auto border_style = border_data.line_style;
    auto device_pixel_width = context.enclosing_device_pixels(width);

    struct Points {
        DevicePixelPoint p1;
        DevicePixelPoint p2;
    };

    auto points_for_edge = [](BorderEdge edge, DevicePixelRect const& rect) -> Points {
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
        auto [p1, p2] = points_for_edge(edge, rect);
        switch (edge) {
        case BorderEdge::Top:
            p1.translate_by(device_pixel_width / 2, device_pixel_width / 2);
            p2.translate_by(-device_pixel_width / 2, device_pixel_width / 2);
            break;
        case BorderEdge::Right:
            p1.translate_by(-device_pixel_width / 2, device_pixel_width / 2);
            p2.translate_by(-device_pixel_width / 2, -device_pixel_width / 2);
            break;
        case BorderEdge::Bottom:
            p1.translate_by(device_pixel_width / 2, -device_pixel_width / 2);
            p2.translate_by(-device_pixel_width / 2, -device_pixel_width / 2);
            break;
        case BorderEdge::Left:
            p1.translate_by(device_pixel_width / 2, device_pixel_width / 2);
            p2.translate_by(device_pixel_width / 2, -device_pixel_width / 2);
            break;
        }
        if (border_style == CSS::LineStyle::Dotted) {
            Gfx::AntiAliasingPainter aa_painter { context.painter() };
            aa_painter.draw_line(p1.to_type<int>(), p2.to_type<int>(), color, device_pixel_width.value(), gfx_line_style);
            return;
        }
        context.painter().draw_line(p1.to_type<int>(), p2.to_type<int>(), color, device_pixel_width.value(), gfx_line_style);
        return;
    }

    auto draw_horizontal_or_vertical_line = [&](auto p1, auto p2) {
        // Note: Using fill_rect() here since draw_line() produces some overlapping pixels
        // at the end of a line, which cause issues on borders with transparency.
        p2.translate_by(1, 1);
        context.painter().fill_rect(Gfx::IntRect::from_two_points(p1.template to_type<int>(), p2.template to_type<int>()), color);
    };

    auto draw_border = [&](auto const& border, auto const& radius, auto const& opposite_border, auto const& opposite_radius, auto p1_step_translate, auto p2_step_translate) {
        auto [p1, p2] = points_for_edge(edge, rect);
        auto p1_step = radius ? 0 : border.width / static_cast<float>(device_pixel_width.value());
        auto p2_step = opposite_radius ? 0 : opposite_border.width / static_cast<float>(device_pixel_width.value());
        for (DevicePixels i = 0; i < device_pixel_width; ++i) {
            draw_horizontal_or_vertical_line(p1, p2);
            p1_step_translate(p1, p1_step);
            p2_step_translate(p2, p2_step);
        }
    };

    // FIXME: There is some overlap where two borders (without border radii meet),
    // which produces artifacts if the border color has some transparency.
    // (this only happens if the angle between the two borders is not 45 degrees)
    switch (edge) {
    case BorderEdge::Top:
        draw_border(
            borders_data.left, border_radii_data.top_left, borders_data.right, border_radii_data.top_right,
            [](auto& current_p1, auto step) {
                current_p1.translate_by(step, 1);
            },
            [](auto& current_p2, auto step) {
                current_p2.translate_by(-step, 1);
            });
        break;
    case BorderEdge::Right:
        draw_border(
            borders_data.top, border_radii_data.top_right, borders_data.bottom, border_radii_data.bottom_right,
            [](auto& current_p1, auto step) {
                current_p1.translate_by(-1, step);
            },
            [](auto& current_p2, auto step) {
                current_p2.translate_by(-1, -step);
            });
        break;
    case BorderEdge::Bottom:
        draw_border(
            borders_data.left, border_radii_data.bottom_left, borders_data.right, border_radii_data.bottom_right,
            [](auto& current_p1, auto step) {
                current_p1.translate_by(step, -1);
            },
            [](auto& current_p2, auto step) {
                current_p2.translate_by(-step, -1);
            });
        break;
    case BorderEdge::Left:
        draw_border(
            borders_data.top, border_radii_data.top_left, borders_data.bottom, border_radii_data.bottom_left,
            [](auto& current_p1, auto step) {
                current_p1.translate_by(1, step);
            },
            [](auto& current_p2, auto step) {
                current_p2.translate_by(1, -step);
            });
        break;
    }
}

RefPtr<Gfx::Bitmap> get_cached_corner_bitmap(DevicePixelSize corners_size)
{
    auto allocate_mask_bitmap = [&]() -> RefPtr<Gfx::Bitmap> {
        auto bitmap = Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRA8888, corners_size.to_type<int>());
        if (!bitmap.is_error())
            return bitmap.release_value();
        return nullptr;
    };
    // FIXME: Allocate per page?
    static thread_local auto corner_bitmap = allocate_mask_bitmap();
    // Only reallocate the corner bitmap is the existing one is too small.
    // (should mean no more allocations after the first paint -- amortised zero allocations :^))
    if (corner_bitmap && corner_bitmap->rect().size().contains(corners_size.to_type<int>())) {
        Gfx::Painter painter { *corner_bitmap };
        painter.clear_rect({ { 0, 0 }, corners_size }, Gfx::Color());
    } else {
        corner_bitmap = allocate_mask_bitmap();
        if (!corner_bitmap) {
            dbgln("Failed to allocate corner bitmap with size {}", corners_size);
            return nullptr;
        }
    }
    return corner_bitmap;
}

void paint_all_borders(PaintContext& context, CSSPixelRect const& bordered_rect, BorderRadiiData const& border_radii_data, BordersData const& borders_data)
{
    if (borders_data.top.width <= 0 && borders_data.right.width <= 0 && borders_data.left.width <= 0 && borders_data.bottom.width <= 0)
        return;

    auto border_rect = context.rounded_device_rect(bordered_rect);

    auto top_left = border_radii_data.top_left.as_corner(context);
    auto top_right = border_radii_data.top_right.as_corner(context);
    auto bottom_right = border_radii_data.bottom_right.as_corner(context);
    auto bottom_left = border_radii_data.bottom_left.as_corner(context);

    // Disable border radii if the corresponding borders don't exist:
    if (borders_data.bottom.width <= 0 && borders_data.left.width <= 0)
        bottom_left = { 0, 0 };
    if (borders_data.bottom.width <= 0 && borders_data.right.width <= 0)
        bottom_right = { 0, 0 };
    if (borders_data.top.width <= 0 && borders_data.left.width <= 0)
        top_left = { 0, 0 };
    if (borders_data.top.width <= 0 && borders_data.right.width <= 0)
        top_right = { 0, 0 };

    DevicePixelRect top_border_rect = {
        border_rect.x() + top_left.horizontal_radius,
        border_rect.y(),
        border_rect.width() - top_left.horizontal_radius - top_right.horizontal_radius,
        context.enclosing_device_pixels(borders_data.top.width)
    };
    DevicePixelRect right_border_rect = {
        border_rect.x() + (border_rect.width() - context.enclosing_device_pixels(borders_data.right.width)),
        border_rect.y() + top_right.vertical_radius,
        context.enclosing_device_pixels(borders_data.right.width),
        border_rect.height() - top_right.vertical_radius - bottom_right.vertical_radius
    };
    DevicePixelRect bottom_border_rect = {
        border_rect.x() + bottom_left.horizontal_radius,
        border_rect.y() + (border_rect.height() - context.enclosing_device_pixels(borders_data.bottom.width)),
        border_rect.width() - bottom_left.horizontal_radius - bottom_right.horizontal_radius,
        context.enclosing_device_pixels(borders_data.bottom.width)
    };
    DevicePixelRect left_border_rect = {
        border_rect.x(),
        border_rect.y() + top_left.vertical_radius,
        context.enclosing_device_pixels(borders_data.left.width),
        border_rect.height() - top_left.vertical_radius - bottom_left.vertical_radius
    };

    // Avoid overlapping pixels on the edges, in the easy 45 degree corners case:
    if (!top_left && top_border_rect.height() == left_border_rect.width())
        top_border_rect.shrink(0, 0, 0, 1);
    if (!top_right && top_border_rect.height() == right_border_rect.width())
        top_border_rect.shrink(0, 1, 0, 0);
    if (!bottom_left && bottom_border_rect.height() == left_border_rect.width())
        bottom_border_rect.shrink(0, 0, 0, 1);
    if (!bottom_right && bottom_border_rect.height() == right_border_rect.width())
        bottom_border_rect.shrink(0, 1, 0, 0);

    auto border_color_no_alpha = borders_data.top.color;
    border_color_no_alpha.set_alpha(255);

    // Paint the strait line part of the border:
    Painting::paint_border(context, Painting::BorderEdge::Top, top_border_rect, border_radii_data, borders_data);
    Painting::paint_border(context, Painting::BorderEdge::Right, right_border_rect, border_radii_data, borders_data);
    Painting::paint_border(context, Painting::BorderEdge::Bottom, bottom_border_rect, border_radii_data, borders_data);
    Painting::paint_border(context, Painting::BorderEdge::Left, left_border_rect, border_radii_data, borders_data);

    if (!top_left && !top_right && !bottom_left && !bottom_right)
        return;

    // Cache the smallest possible bitmap to render just the corners for the border.
    auto expand_width = abs(context.enclosing_device_pixels(borders_data.left.width) - context.enclosing_device_pixels(borders_data.right.width));
    auto expand_height = abs(context.enclosing_device_pixels(borders_data.top.width) - context.enclosing_device_pixels(borders_data.bottom.width));
    DevicePixelRect corner_mask_rect {
        0, 0,
        max(
            top_left.horizontal_radius + top_right.horizontal_radius + expand_width.value(),
            bottom_left.horizontal_radius + bottom_right.horizontal_radius + expand_height.value()),
        max(
            top_left.vertical_radius + bottom_left.vertical_radius + expand_width.value(),
            top_right.vertical_radius + bottom_right.vertical_radius + expand_height.value())
    };

    auto corner_bitmap = get_cached_corner_bitmap(corner_mask_rect.size());
    if (!corner_bitmap)
        return;
    Gfx::Painter painter { *corner_bitmap };

    Gfx::AntiAliasingPainter aa_painter { painter };

    // Paint a little tile sheet for the corners
    // TODO: Support various line styles on the corners (dotted, dashes, etc)

    // Paint the outer (minimal) corner rounded rectangle:
    aa_painter.fill_rect_with_rounded_corners(corner_mask_rect.to_type<int>(), border_color_no_alpha, top_left, top_right, bottom_right, bottom_left);

    // Subtract the inner corner rectangle:
    auto inner_corner_mask_rect = corner_mask_rect.shrunken(
        context.enclosing_device_pixels(borders_data.top.width),
        context.enclosing_device_pixels(borders_data.right.width),
        context.enclosing_device_pixels(borders_data.bottom.width),
        context.enclosing_device_pixels(borders_data.left.width));
    auto inner_top_left = top_left;
    auto inner_top_right = top_right;
    auto inner_bottom_right = bottom_right;
    auto inner_bottom_left = bottom_left;
    inner_top_left.horizontal_radius = max(0, inner_top_left.horizontal_radius - context.enclosing_device_pixels(borders_data.left.width).value());
    inner_top_left.vertical_radius = max(0, inner_top_left.vertical_radius - context.enclosing_device_pixels(borders_data.top.width).value());
    inner_top_right.horizontal_radius = max(0, inner_top_right.horizontal_radius - context.enclosing_device_pixels(borders_data.right.width).value());
    inner_top_right.vertical_radius = max(0, inner_top_right.vertical_radius - context.enclosing_device_pixels(borders_data.top.width).value());
    inner_bottom_right.horizontal_radius = max(0, inner_bottom_right.horizontal_radius - context.enclosing_device_pixels(borders_data.right.width).value());
    inner_bottom_right.vertical_radius = max(0, inner_bottom_right.vertical_radius - context.enclosing_device_pixels(borders_data.bottom.width).value());
    inner_bottom_left.horizontal_radius = max(0, inner_bottom_left.horizontal_radius - context.enclosing_device_pixels(borders_data.left.width).value());
    inner_bottom_left.vertical_radius = max(0, inner_bottom_left.vertical_radius - context.enclosing_device_pixels(borders_data.bottom.width).value());
    aa_painter.fill_rect_with_rounded_corners(inner_corner_mask_rect.to_type<int>(), border_color_no_alpha, inner_top_left, inner_top_right, inner_bottom_right, inner_bottom_left, Gfx::AntiAliasingPainter::BlendMode::AlphaSubtract);

    // TODO: Support dual color corners. Other browsers will render a rounded corner between two borders of
    // different colors using both colours, normally split at a 45 degree angle (though the exact angle is interpolated).
    auto blit_corner = [&](Gfx::IntPoint position, Gfx::IntRect const& src_rect, Color corner_color) {
        context.painter().blit_filtered(position, *corner_bitmap, src_rect, [&](auto const& corner_pixel) {
            return corner_color.with_alpha((corner_color.alpha() * corner_pixel.alpha()) / 255);
        });
    };

    // FIXME: Corners should actually split between the two colors, if both are provided (and differ)
    auto pick_corner_color = [](auto const& border, auto const& adjacent_border) {
        if (border.width > 0)
            return border.color;
        return adjacent_border.color;
    };

    // Blit the corners into to their corresponding locations:
    if (top_left)
        blit_corner(border_rect.top_left().to_type<int>(), top_left.as_rect(), pick_corner_color(borders_data.top, borders_data.left));

    if (top_right)
        blit_corner(border_rect.top_right().to_type<int>().translated(-top_right.horizontal_radius + 1, 0), top_right.as_rect().translated(corner_mask_rect.width().value() - top_right.horizontal_radius, 0), pick_corner_color(borders_data.top, borders_data.right));

    if (bottom_right)
        blit_corner(border_rect.bottom_right().to_type<int>().translated(-bottom_right.horizontal_radius + 1, -bottom_right.vertical_radius + 1), bottom_right.as_rect().translated(corner_mask_rect.width().value() - bottom_right.horizontal_radius, corner_mask_rect.height().value() - bottom_right.vertical_radius), pick_corner_color(borders_data.bottom, borders_data.right));

    if (bottom_left)
        blit_corner(border_rect.bottom_left().to_type<int>().translated(0, -bottom_left.vertical_radius + 1), bottom_left.as_rect().translated(0, corner_mask_rect.height().value() - bottom_left.vertical_radius), pick_corner_color(borders_data.bottom, borders_data.left));
}

}
