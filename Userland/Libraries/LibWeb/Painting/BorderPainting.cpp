/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CircularQueue.h>
#include <LibGfx/AntiAliasingPainter.h>
#include <LibGfx/Painter.h>
#include <LibGfx/Path.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/Layout/Node.h>
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
    // Let f = min(Li/Si), where i ∈ {top, right, bottom, left},
    // Si is the sum of the two corresponding radii of the corners on side i,
    // and Ltop = Lbottom = the width of the box, and Lleft = Lright = the height of the box.
    auto l_top = rect.width();
    auto l_bottom = l_top;
    auto l_left = rect.height();
    auto l_right = l_left;
    auto s_top = (top_left_radius_px.horizontal_radius + top_right_radius_px.horizontal_radius);
    auto s_right = (top_right_radius_px.vertical_radius + bottom_right_radius_px.vertical_radius);
    auto s_bottom = (bottom_left_radius_px.horizontal_radius + bottom_right_radius_px.horizontal_radius);
    auto s_left = (top_left_radius_px.vertical_radius + bottom_left_radius_px.vertical_radius);
    CSSPixelFraction f = 1;
    f = min(f, l_top / s_top);
    f = min(f, l_right / s_right);
    f = min(f, l_bottom / s_bottom);
    f = min(f, l_left / s_left);

    // If f < 1, then all corner radii are reduced by multiplying them by f.
    if (f < 1) {
        top_left_radius_px.horizontal_radius *= f;
        top_left_radius_px.vertical_radius *= f;
        top_right_radius_px.horizontal_radius *= f;
        top_right_radius_px.vertical_radius *= f;
        bottom_right_radius_px.horizontal_radius *= f;
        bottom_right_radius_px.vertical_radius *= f;
        bottom_left_radius_px.horizontal_radius *= f;
        bottom_left_radius_px.vertical_radius *= f;
    }

    return BorderRadiiData { top_left_radius_px, top_right_radius_px, bottom_right_radius_px, bottom_left_radius_px };
}

static constexpr double dark_light_absolute_value_difference = 1. / 3;

static Color light_color_for_inset_and_outset(Color const& color)
{
    auto hsv = color.to_hsv();
    if (hsv.value >= dark_light_absolute_value_difference)
        return Color::from_hsv(hsv);
    return Color::from_hsv({ hsv.hue, hsv.saturation, hsv.value + dark_light_absolute_value_difference });
}

static Color dark_color_for_inset_and_outset(Color const& color)
{
    auto hsv = color.to_hsv();
    if (hsv.value < dark_light_absolute_value_difference)
        return Color::from_hsv(hsv);
    return Color::from_hsv({ hsv.hue, hsv.saturation, hsv.value - dark_light_absolute_value_difference });
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
        auto top_left_color = dark_color_for_inset_and_outset(border_data.color);
        auto bottom_right_color = light_color_for_inset_and_outset(border_data.color);
        return (edge == BorderEdge::Left || edge == BorderEdge::Top) ? top_left_color : bottom_right_color;
    } else if (border_data.line_style == CSS::LineStyle::Outset) {
        auto top_left_color = light_color_for_inset_and_outset(border_data.color);
        auto bottom_right_color = dark_color_for_inset_and_outset(border_data.color);
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
    switch (border_style) {
    case CSS::LineStyle::None:
    case CSS::LineStyle::Hidden:
        return;
    case CSS::LineStyle::Dotted:
        gfx_line_style = Gfx::Painter::LineStyle::Dotted;
        break;
    case CSS::LineStyle::Dashed:
        gfx_line_style = Gfx::Painter::LineStyle::Dashed;
        break;
    case CSS::LineStyle::Solid:
        gfx_line_style = Gfx::Painter::LineStyle::Solid;
        break;
    case CSS::LineStyle::Double:
    case CSS::LineStyle::Groove:
    case CSS::LineStyle::Ridge:
    case CSS::LineStyle::Inset:
    case CSS::LineStyle::Outset:
        // FIXME: Implement these
        break;
    }

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
            context.painter().draw_line(p1.to_type<int>(), p2.to_type<int>(), color, device_pixel_width.value(), gfx_line_style);
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
            path.close_all_subpaths();
            context.painter().fill_path({ .path = path, .color = color, .winding_rule = Gfx::Painter::WindingRule::EvenOdd });
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

void paint_all_borders(PaintContext& context, DevicePixelRect const& border_rect, BorderRadiiData const& border_radii_data, BordersData const& borders_data)
{
    if (borders_data.top.width <= 0 && borders_data.right.width <= 0 && borders_data.left.width <= 0 && borders_data.bottom.width <= 0)
        return;

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

Optional<BordersData> borders_data_for_outline(Layout::Node const& layout_node, Color outline_color, CSS::OutlineStyle outline_style, CSSPixels outline_width)
{
    CSS::LineStyle line_style;
    if (outline_style == CSS::OutlineStyle::Auto) {
        // `auto` lets us do whatever we want for the outline. 2px of the link colour seems reasonable.
        line_style = CSS::LineStyle::Dotted;
        outline_color = layout_node.document().link_color();
        outline_width = 2;
    } else {
        line_style = CSS::value_id_to_line_style(CSS::to_value_id(outline_style)).value_or(CSS::LineStyle::None);
    }

    if (outline_color.alpha() == 0 || line_style == CSS::LineStyle::None || outline_width == 0)
        return {};

    CSS::BorderData border_data {
        .color = outline_color,
        .line_style = line_style,
        .width = outline_width,
    };
    return BordersData { border_data, border_data, border_data, border_data };
}

}
