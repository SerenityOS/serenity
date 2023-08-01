/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CircularQueue.h>
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

    bottom_left_radius_px.horizontal_radius = bottom_left_radius.horizontal_radius.to_px(node, rect.width());
    bottom_right_radius_px.horizontal_radius = bottom_right_radius.horizontal_radius.to_px(node, rect.width());
    top_left_radius_px.horizontal_radius = top_left_radius.horizontal_radius.to_px(node, rect.width());
    top_right_radius_px.horizontal_radius = top_right_radius.horizontal_radius.to_px(node, rect.width());

    bottom_left_radius_px.vertical_radius = bottom_left_radius.vertical_radius.to_px(node, rect.height());
    bottom_right_radius_px.vertical_radius = bottom_right_radius.vertical_radius.to_px(node, rect.height());
    top_left_radius_px.vertical_radius = top_left_radius.vertical_radius.to_px(node, rect.height());
    top_right_radius_px.vertical_radius = top_right_radius.vertical_radius.to_px(node, rect.height());

    // Scale overlapping curves according to https://www.w3.org/TR/css-backgrounds-3/#corner-overlap
    CSSPixels f = 1;
    if (rect.width() > 0) {
        f = max(f, (top_left_radius_px.horizontal_radius + top_right_radius_px.horizontal_radius) / rect.width());
        f = max(f, (bottom_left_radius_px.horizontal_radius + bottom_right_radius_px.horizontal_radius) / rect.width());
    }
    if (rect.height() > 0) {
        f = max(f, (top_right_radius_px.vertical_radius + bottom_right_radius_px.vertical_radius) / rect.height());
        f = max(f, (top_left_radius_px.vertical_radius + bottom_left_radius_px.vertical_radius) / rect.height());
    }

    top_left_radius_px.horizontal_radius /= f;
    top_left_radius_px.vertical_radius /= f;
    top_right_radius_px.horizontal_radius /= f;
    top_right_radius_px.vertical_radius /= f;
    bottom_right_radius_px.horizontal_radius /= f;
    bottom_right_radius_px.vertical_radius /= f;
    bottom_left_radius_px.horizontal_radius /= f;
    bottom_left_radius_px.vertical_radius /= f;

    return BorderRadiiData { top_left_radius_px, top_right_radius_px, bottom_right_radius_px, bottom_left_radius_px };
}

Gfx::Color border_color(BorderEdge edge, BordersData const& borders_data)
{
    auto const& border_data = [&] {
        switch (edge) {
        case BorderEdge::Top:
            return borders_data.top;
        case BorderEdge::Right:
            return borders_data.right;
        case BorderEdge::Bottom:
            return borders_data.bottom;
        case BorderEdge::Left:
            return borders_data.left;
        default:
            VERIFY_NOT_REACHED();
        }
    }();

    if (border_data.line_style == CSS::LineStyle::Inset) {
        auto top_left_color = Color::from_rgb(0x5a5a5a);
        auto bottom_right_color = Color::from_rgb(0x888888);
        return (edge == BorderEdge::Left || edge == BorderEdge::Top) ? top_left_color : bottom_right_color;
    } else if (border_data.line_style == CSS::LineStyle::Outset) {
        auto top_left_color = Color::from_rgb(0x888888);
        auto bottom_right_color = Color::from_rgb(0x5a5a5a);
        return (edge == BorderEdge::Left || edge == BorderEdge::Top) ? top_left_color : bottom_right_color;
    }

    return border_data.color;
}

void paint_border(PaintContext& context, BorderEdge edge, DevicePixelRect const& rect, Gfx::AntiAliasingPainter::CornerRadius const& radius, Gfx::AntiAliasingPainter::CornerRadius const& opposite_radius, BordersData const& borders_data, Gfx::Path& path, bool last)
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

    auto color = border_color(edge, borders_data);
    auto border_style = border_data.line_style;
    auto device_pixel_width = context.rounded_device_pixels(width);

    struct Points {
        DevicePixelPoint p1;
        DevicePixelPoint p2;
    };

    auto points_for_edge = [](BorderEdge edge, DevicePixelRect const& rect) -> Points {
        switch (edge) {
        case BorderEdge::Top:
            return { rect.top_left(), rect.top_right().moved_left(1) };
        case BorderEdge::Right:
            return { rect.top_right().moved_left(1), rect.bottom_right().translated(-1) };
        case BorderEdge::Bottom:
            return { rect.bottom_left().moved_up(1), rect.bottom_right().translated(-1) };
        default: // Edge::Left
            return { rect.top_left(), rect.bottom_left().moved_up(1) };
        }
    };

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

    auto draw_border = [&](Vector<Gfx::FloatPoint> const& points, bool joined_corner_has_inner_corner, bool opposite_joined_corner_has_inner_corner, Gfx::FloatSize joined_inner_corner_offset, Gfx::FloatSize opposite_joined_inner_corner_offset, bool ready_to_draw) {
        int current = 0;
        path.move_to(points[current++]);
        path.elliptical_arc_to(
            points[current++],
            Gfx::FloatSize(radius.horizontal_radius, radius.vertical_radius),
            0,
            false,
            false);
        path.line_to(points[current++]);
        if (joined_corner_has_inner_corner) {
            path.elliptical_arc_to(
                points[current++],
                Gfx::FloatSize(radius.horizontal_radius - joined_inner_corner_offset.width(), radius.vertical_radius - joined_inner_corner_offset.height()),
                0,
                false,
                true);
        }
        path.line_to(points[current++]);
        if (opposite_joined_corner_has_inner_corner) {
            path.elliptical_arc_to(
                points[current++],
                Gfx::FloatSize(opposite_radius.horizontal_radius - opposite_joined_inner_corner_offset.width(), opposite_radius.vertical_radius - opposite_joined_inner_corner_offset.height()),
                0,
                false,
                true);
        }
        path.line_to(points[current++]);
        path.elliptical_arc_to(
            points[current++],
            Gfx::FloatSize(opposite_radius.horizontal_radius, opposite_radius.vertical_radius),
            0,
            false,
            false);

        // If joined borders have the same color, combine them to draw together.
        if (ready_to_draw) {
            Gfx::AntiAliasingPainter aa_painter { context.painter() };
            path.close_all_subpaths();
            aa_painter.fill_path(path, color, Gfx::Painter::WindingRule::EvenOdd);
            path.clear();
        }
    };

    auto compute_midpoint = [&](int horizontal_radius, int vertical_radius, int joined_border_width) {
        if (horizontal_radius == 0 && vertical_radius == 0) {
            return Gfx::FloatPoint(0, 0);
        }
        if (joined_border_width == 0) {
            switch (edge) {
            case BorderEdge::Top:
            case BorderEdge::Bottom:
                return Gfx::FloatPoint(horizontal_radius, 0);
            case BorderEdge::Right:
            case BorderEdge::Left:
                return Gfx::FloatPoint(0, vertical_radius);
            default:
                VERIFY_NOT_REACHED();
            }
        }
        // FIXME: this middle point rule seems not exacly the same as main browsers
        // compute the midpoint based on point whose tangent slope of 1
        // https://math.stackexchange.com/questions/3325134/find-the-points-on-the-ellipse-where-the-slope-of-the-tangent-line-is-1
        return Gfx::FloatPoint(
            (horizontal_radius * horizontal_radius) / AK::sqrt(1.0f * horizontal_radius * horizontal_radius + vertical_radius * vertical_radius),
            (vertical_radius * vertical_radius) / AK::sqrt(1.0f * horizontal_radius * horizontal_radius + vertical_radius * vertical_radius));
    };

    /**
     *   0 /-------------\ 7
     *    / /-----------\ \
     *   /-/ 3         4 \-\
     *  1  2             5  6
     * For each border edge, need to compute 8 points at most, then paint them as closed path.
     * 8 points are the most complicated case, it happens when the joined border width is not 0 and border radius larger than border width on both side.
     * If border radius is smaller than the border width, then the inner corner of the border corner is a right angle.
     */
    switch (edge) {
    case BorderEdge::Top: {
        auto joined_border_width = context.enclosing_device_pixels(borders_data.left.width);
        auto opposite_joined_border_width = context.enclosing_device_pixels(borders_data.right.width);
        bool joined_corner_has_inner_corner = device_pixel_width < radius.vertical_radius && joined_border_width < radius.horizontal_radius;
        bool opposite_joined_corner_has_inner_corner = device_pixel_width < opposite_radius.vertical_radius && opposite_joined_border_width < opposite_radius.horizontal_radius;

        Gfx::FloatPoint joined_corner_endpoint_offset;
        Gfx::FloatPoint opposite_joined_border_corner_offset;

        {
            auto midpoint = compute_midpoint(radius.horizontal_radius, radius.vertical_radius, joined_border_width.value());
            joined_corner_endpoint_offset = Gfx::FloatPoint(-midpoint.x(), radius.vertical_radius - midpoint.y());
        }

        {
            auto midpoint = compute_midpoint(opposite_radius.horizontal_radius, opposite_radius.vertical_radius, opposite_joined_border_width.value());
            opposite_joined_border_corner_offset = Gfx::FloatPoint(midpoint.x(), opposite_radius.vertical_radius - midpoint.y());
        }

        Vector<Gfx::FloatPoint, 8> points;
        points.append(Gfx::FloatPoint(rect.top_left().to_type<int>()));
        points.append(Gfx::FloatPoint(rect.top_left().to_type<int>()) + joined_corner_endpoint_offset);

        if (joined_corner_has_inner_corner) {
            Gfx::FloatPoint midpoint = compute_midpoint(
                radius.horizontal_radius - joined_border_width.value(),
                radius.vertical_radius - device_pixel_width.value(),
                joined_border_width.value());
            Gfx::FloatPoint inner_corner_endpoint_offset = Gfx::FloatPoint(
                -midpoint.x(),
                radius.vertical_radius - device_pixel_width.value() - midpoint.y());
            points.append(Gfx::FloatPoint(rect.bottom_left().to_type<int>()) + inner_corner_endpoint_offset);
            points.append(Gfx::FloatPoint(rect.bottom_left().to_type<int>()));
        } else {
            Gfx::FloatPoint inner_right_angle_offset = Gfx::FloatPoint(
                joined_border_width.value() - radius.horizontal_radius,
                0);
            points.append(Gfx::FloatPoint(rect.bottom_left().to_type<int>()) + inner_right_angle_offset);
        }

        if (opposite_joined_corner_has_inner_corner) {
            Gfx::FloatPoint midpoint = compute_midpoint(
                opposite_radius.horizontal_radius - opposite_joined_border_width.value(),
                opposite_radius.vertical_radius - device_pixel_width.value(),
                opposite_joined_border_width.value());
            Gfx::FloatPoint inner_corner_endpoint_offset = Gfx::FloatPoint(
                midpoint.x(),
                opposite_radius.vertical_radius - device_pixel_width.value() - midpoint.y());
            points.append(Gfx::FloatPoint(rect.bottom_right().to_type<int>()));
            points.append(Gfx::FloatPoint(rect.bottom_right().to_type<int>()) + inner_corner_endpoint_offset);
        } else {
            Gfx::FloatPoint inner_right_angle_offset = Gfx::FloatPoint(
                opposite_joined_border_width.value() - opposite_radius.horizontal_radius,
                0);
            points.append(Gfx::FloatPoint(rect.bottom_right().to_type<int>()) - inner_right_angle_offset);
        }

        points.append(Gfx::FloatPoint(rect.top_right().to_type<int>()) + opposite_joined_border_corner_offset);
        points.append(Gfx::FloatPoint(rect.top_right().to_type<int>()));

        draw_border(
            points,
            joined_corner_has_inner_corner,
            opposite_joined_corner_has_inner_corner,
            Gfx::FloatSize(joined_border_width.value(), device_pixel_width.value()),
            Gfx::FloatSize(opposite_joined_border_width.value(), device_pixel_width.value()),
            last || color != border_color(BorderEdge::Right, borders_data));
        break;
    }
    case BorderEdge::Right: {
        auto joined_border_width = context.enclosing_device_pixels(borders_data.top.width);
        auto opposite_joined_border_width = context.enclosing_device_pixels(borders_data.bottom.width);
        bool joined_corner_has_inner_corner = device_pixel_width < radius.horizontal_radius && joined_border_width < radius.vertical_radius;
        bool opposite_joined_corner_has_inner_corner = device_pixel_width < opposite_radius.horizontal_radius && opposite_joined_border_width < opposite_radius.vertical_radius;

        Gfx::FloatPoint joined_corner_endpoint_offset;
        Gfx::FloatPoint opposite_joined_border_corner_offset;

        {
            auto midpoint = compute_midpoint(radius.horizontal_radius, radius.vertical_radius, joined_border_width.value());
            joined_corner_endpoint_offset = Gfx::FloatPoint(midpoint.x() - radius.horizontal_radius, -midpoint.y());
        }

        {
            auto midpoint = compute_midpoint(opposite_radius.horizontal_radius, opposite_radius.vertical_radius, opposite_joined_border_width.value());
            opposite_joined_border_corner_offset = Gfx::FloatPoint(midpoint.x() - opposite_radius.horizontal_radius, midpoint.y());
        }

        Vector<Gfx::FloatPoint, 8> points;
        points.append(Gfx::FloatPoint(rect.top_right().to_type<int>()));
        points.append(Gfx::FloatPoint(rect.top_right().to_type<int>()) + joined_corner_endpoint_offset);

        if (joined_corner_has_inner_corner) {
            auto midpoint = compute_midpoint(
                radius.horizontal_radius - device_pixel_width.value(),
                radius.vertical_radius - joined_border_width.value(),
                joined_border_width.value());
            Gfx::FloatPoint inner_corner = Gfx::FloatPoint(
                -(radius.horizontal_radius - midpoint.x() - device_pixel_width.value()),
                -midpoint.y());
            points.append(Gfx::FloatPoint(rect.top_left().to_type<int>()) + inner_corner);
            points.append(Gfx::FloatPoint(rect.top_left().to_type<int>()));
        } else {
            Gfx::FloatPoint inner_right_angle_offset = Gfx::FloatPoint(0, joined_border_width.value() - radius.horizontal_radius);
            points.append(Gfx::FloatPoint(rect.top_left().to_type<int>()) + inner_right_angle_offset);
        }

        if (opposite_joined_corner_has_inner_corner) {
            auto midpoint = compute_midpoint(
                opposite_radius.horizontal_radius - device_pixel_width.value(),
                opposite_radius.vertical_radius - opposite_joined_border_width.value(),
                opposite_joined_border_width.value());
            Gfx::FloatPoint inner_corner = Gfx::FloatPoint(
                -(opposite_radius.horizontal_radius - midpoint.x() - device_pixel_width.value()),
                midpoint.y());
            points.append(Gfx::FloatPoint(rect.bottom_left().to_type<int>()));
            points.append(Gfx::FloatPoint(rect.bottom_left().to_type<int>()) + inner_corner);
        } else {
            Gfx::FloatPoint inner_right_angle_offset = Gfx::FloatPoint(0, opposite_joined_border_width.value() - opposite_radius.horizontal_radius);
            points.append(Gfx::FloatPoint(rect.bottom_left().to_type<int>()) - inner_right_angle_offset);
        }

        points.append(Gfx::FloatPoint(rect.bottom_right().to_type<int>()) + opposite_joined_border_corner_offset);
        points.append(Gfx::FloatPoint(rect.bottom_right().to_type<int>()));

        draw_border(
            points,
            joined_corner_has_inner_corner,
            opposite_joined_corner_has_inner_corner,
            Gfx::FloatSize(device_pixel_width.value(), joined_border_width.value()),
            Gfx::FloatSize(device_pixel_width.value(), opposite_joined_border_width.value()),
            last || color != border_color(BorderEdge::Bottom, borders_data));
        break;
    }
    case BorderEdge::Bottom: {
        auto joined_border_width = context.enclosing_device_pixels(borders_data.right.width);
        auto opposite_joined_border_width = context.enclosing_device_pixels(borders_data.left.width);
        bool joined_corner_has_inner_corner = device_pixel_width < radius.vertical_radius && joined_border_width < radius.horizontal_radius;
        bool opposite_joined_corner_has_inner_corner = device_pixel_width < opposite_radius.vertical_radius && opposite_joined_border_width < opposite_radius.horizontal_radius;

        Gfx::FloatPoint joined_corner_endpoint_offset;
        Gfx::FloatPoint opposite_joined_border_corner_offset;

        {
            auto midpoint = compute_midpoint(radius.horizontal_radius, radius.vertical_radius, joined_border_width.value());
            joined_corner_endpoint_offset = Gfx::FloatPoint(midpoint.x(), midpoint.y() - radius.vertical_radius);
        }

        {
            auto midpoint = compute_midpoint(opposite_radius.horizontal_radius, opposite_radius.vertical_radius, opposite_joined_border_width.value());
            opposite_joined_border_corner_offset = Gfx::FloatPoint(-midpoint.x(), midpoint.y() - opposite_radius.vertical_radius);
        }

        Vector<Gfx::FloatPoint, 8> points;
        points.append(Gfx::FloatPoint(rect.bottom_right().to_type<int>()));
        points.append(Gfx::FloatPoint(rect.bottom_right().to_type<int>()) + joined_corner_endpoint_offset);

        if (joined_corner_has_inner_corner) {
            auto midpoint = compute_midpoint(
                radius.horizontal_radius - joined_border_width.value(),
                radius.vertical_radius - device_pixel_width.value(),
                joined_border_width.value());
            Gfx::FloatPoint inner_corner = Gfx::FloatPoint(midpoint.x(), -(radius.vertical_radius - midpoint.y() - device_pixel_width.value()));
            points.append(Gfx::FloatPoint(rect.top_right().to_type<int>()) + inner_corner);
            points.append(Gfx::FloatPoint(rect.top_right().to_type<int>()));
        } else {
            Gfx::FloatPoint inner_right_angle_offset = Gfx::FloatPoint(joined_border_width.value() - radius.horizontal_radius, 0);
            points.append(Gfx::FloatPoint(rect.top_right().to_type<int>()) - inner_right_angle_offset);
        }

        if (opposite_joined_corner_has_inner_corner) {
            auto midpoint = compute_midpoint(
                opposite_radius.horizontal_radius - opposite_joined_border_width.value(),
                opposite_radius.vertical_radius - device_pixel_width.value(),
                opposite_joined_border_width.value());
            Gfx::FloatPoint inner_corner = Gfx::FloatPoint(
                -midpoint.x(),
                -(opposite_radius.vertical_radius - midpoint.y() - device_pixel_width.value()));
            points.append(Gfx::FloatPoint(rect.top_left().to_type<int>()));
            points.append(Gfx::FloatPoint(rect.top_left().to_type<int>()) + inner_corner);
        } else {
            Gfx::FloatPoint inner_right_angle_offset = Gfx::FloatPoint(opposite_joined_border_width.value() - opposite_radius.horizontal_radius, 0);
            points.append(Gfx::FloatPoint(rect.top_left().to_type<int>()) + inner_right_angle_offset);
        }

        points.append(Gfx::FloatPoint(rect.bottom_left().to_type<int>()) + opposite_joined_border_corner_offset);
        points.append(Gfx::FloatPoint(rect.bottom_left().to_type<int>()));
        draw_border(
            points,
            joined_corner_has_inner_corner,
            opposite_joined_corner_has_inner_corner,
            Gfx::FloatSize(joined_border_width.value(), device_pixel_width.value()),
            Gfx::FloatSize(opposite_joined_border_width.value(), device_pixel_width.value()),
            last || color != border_color(BorderEdge::Left, borders_data));
        break;
    }
    case BorderEdge::Left: {
        auto joined_border_width = context.enclosing_device_pixels(borders_data.bottom.width);
        auto opposite_joined_border_width = context.enclosing_device_pixels(borders_data.top.width);
        bool joined_corner_has_inner_corner = device_pixel_width < radius.horizontal_radius && joined_border_width < radius.vertical_radius;
        bool opposite_joined_corner_has_inner_corner = device_pixel_width < opposite_radius.horizontal_radius && opposite_joined_border_width < opposite_radius.vertical_radius;

        Gfx::FloatPoint joined_corner_endpoint_offset;
        Gfx::FloatPoint opposite_joined_border_corner_offset;

        {
            auto midpoint = compute_midpoint(radius.horizontal_radius, radius.vertical_radius, joined_border_width.value());
            joined_corner_endpoint_offset = Gfx::FloatPoint(radius.horizontal_radius - midpoint.x(), midpoint.y());
        }

        {
            auto midpoint = compute_midpoint(opposite_radius.horizontal_radius, opposite_radius.vertical_radius, opposite_joined_border_width.value());
            opposite_joined_border_corner_offset = Gfx::FloatPoint(opposite_radius.horizontal_radius - midpoint.x(), -midpoint.y());
        }

        Vector<Gfx::FloatPoint, 8> points;
        points.append(Gfx::FloatPoint(rect.bottom_left().to_type<int>()));
        points.append(Gfx::FloatPoint(rect.bottom_left().to_type<int>()) + joined_corner_endpoint_offset);

        if (joined_corner_has_inner_corner) {
            auto midpoint = compute_midpoint(
                radius.horizontal_radius - device_pixel_width.value(),
                radius.vertical_radius - joined_border_width.value(),
                joined_border_width.value());
            Gfx::FloatPoint inner_corner = Gfx::FloatPoint(radius.horizontal_radius - device_pixel_width.value() - midpoint.x(), midpoint.y());
            points.append(Gfx::FloatPoint(rect.bottom_right().to_type<int>()) + inner_corner);
            points.append(Gfx::FloatPoint(rect.bottom_right().to_type<int>()));
        } else {
            Gfx::FloatPoint inner_right_angle_offset = Gfx::FloatPoint(0, joined_border_width.value() - radius.vertical_radius);
            points.append(Gfx::FloatPoint(rect.bottom_right().to_type<int>()) - inner_right_angle_offset);
        }

        if (opposite_joined_corner_has_inner_corner) {
            auto midpoint = compute_midpoint(
                opposite_radius.horizontal_radius - device_pixel_width.value(),
                opposite_radius.vertical_radius - opposite_joined_border_width.value(),
                opposite_joined_border_width.value());
            Gfx::FloatPoint inner_corner = Gfx::FloatPoint(
                opposite_radius.horizontal_radius - device_pixel_width.value() - midpoint.x(),
                -midpoint.y());
            points.append(Gfx::FloatPoint(rect.top_right().to_type<int>()));
            points.append(Gfx::FloatPoint(rect.top_right().to_type<int>()) + inner_corner);
        } else {
            Gfx::FloatPoint inner_right_angle_offset = Gfx::FloatPoint(0, opposite_joined_border_width.value() - opposite_radius.vertical_radius);
            points.append(Gfx::FloatPoint(rect.top_right().to_type<int>()) + inner_right_angle_offset);
        }
        points.append(Gfx::FloatPoint(rect.top_left().to_type<int>()) + opposite_joined_border_corner_offset);
        points.append(Gfx::FloatPoint(rect.top_left().to_type<int>()));

        draw_border(
            points,
            joined_corner_has_inner_corner,
            opposite_joined_corner_has_inner_corner,
            Gfx::FloatSize(device_pixel_width.value(), joined_border_width.value()),
            Gfx::FloatSize(device_pixel_width.value(), opposite_joined_border_width.value()),
            last || color != border_color(BorderEdge::Top, borders_data));
        break;
    }
    }
}

RefPtr<Gfx::Bitmap> get_cached_corner_bitmap(DevicePixelSize corners_size)
{
    auto allocate_mask_bitmap = [&]() -> RefPtr<Gfx::Bitmap> {
        auto bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, corners_size.to_type<int>());
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

    AK::CircularQueue<BorderEdge, 4> borders;
    borders.enqueue(BorderEdge::Top);
    borders.enqueue(BorderEdge::Right);
    borders.enqueue(BorderEdge::Bottom);
    borders.enqueue(BorderEdge::Left);

    // Try to find the first border that has a different color than the previous one,
    // then start painting from that border.
    for (size_t i = 0; i < borders.size(); i++) {
        if (border_color(borders.at(0), borders_data) != border_color(borders.at(1), borders_data)) {
            borders.enqueue(borders.dequeue());
            break;
        }

        borders.enqueue(borders.dequeue());
    }

    Gfx::Path path;
    for (BorderEdge edge : borders) {
        switch (edge) {
        case BorderEdge::Top:
            paint_border(context, BorderEdge::Top, top_border_rect, top_left, top_right, borders_data, path, edge == borders.last());
            break;
        case BorderEdge::Right:
            paint_border(context, BorderEdge::Right, right_border_rect, top_right, bottom_right, borders_data, path, edge == borders.last());
            break;
        case BorderEdge::Bottom:
            paint_border(context, BorderEdge::Bottom, bottom_border_rect, bottom_right, bottom_left, borders_data, path, edge == borders.last());
            break;
        case BorderEdge::Left:
            paint_border(context, BorderEdge::Left, left_border_rect, bottom_left, top_left, borders_data, path, edge == borders.last());
            break;
        default:
            VERIFY_NOT_REACHED();
        }
    }
}

}
