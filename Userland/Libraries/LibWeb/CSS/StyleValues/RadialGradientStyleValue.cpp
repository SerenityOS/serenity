/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022-2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "RadialGradientStyleValue.h"
#include <LibWeb/Layout/Node.h>

namespace Web::CSS {

ErrorOr<String> RadialGradientStyleValue::to_string() const
{
    StringBuilder builder;
    if (is_repeating())
        TRY(builder.try_append("repeating-"sv));
    TRY(builder.try_appendff("radial-gradient({} "sv,
        m_properties.ending_shape == EndingShape::Circle ? "circle"sv : "ellipse"sv));

    TRY(m_properties.size.visit(
        [&](Extent extent) -> ErrorOr<void> {
            return builder.try_append([&] {
                switch (extent) {
                case Extent::ClosestCorner:
                    return "closest-corner"sv;
                case Extent::ClosestSide:
                    return "closest-side"sv;
                case Extent::FarthestCorner:
                    return "farthest-corner"sv;
                case Extent::FarthestSide:
                    return "farthest-side"sv;
                default:
                    VERIFY_NOT_REACHED();
                }
            }());
        },
        [&](CircleSize const& circle_size) -> ErrorOr<void> {
            return builder.try_append(TRY(circle_size.radius.to_string()));
        },
        [&](EllipseSize const& ellipse_size) -> ErrorOr<void> {
            return builder.try_appendff("{} {}", TRY(ellipse_size.radius_a.to_string()), TRY(ellipse_size.radius_b.to_string()));
        }));

    if (m_properties.position != PositionValue::center()) {
        TRY(builder.try_appendff(" at "sv));
        TRY(m_properties.position.serialize(builder));
    }

    TRY(builder.try_append(", "sv));
    TRY(serialize_color_stop_list(builder, m_properties.color_stop_list));
    TRY(builder.try_append(')'));
    return builder.to_string();
}

Gfx::FloatSize RadialGradientStyleValue::resolve_size(Layout::Node const& node, Gfx::FloatPoint center, Gfx::FloatRect const& size) const
{
    auto const side_shape = [&](auto distance_function) {
        auto const distance_from = [&](float v, float a, float b, auto distance_function) {
            return distance_function(fabs(a - v), fabs(b - v));
        };
        auto x_dist = distance_from(center.x(), size.left(), size.right(), distance_function);
        auto y_dist = distance_from(center.y(), size.top(), size.bottom(), distance_function);
        if (m_properties.ending_shape == EndingShape::Circle) {
            auto dist = distance_function(x_dist, y_dist);
            return Gfx::FloatSize { dist, dist };
        } else {
            return Gfx::FloatSize { x_dist, y_dist };
        }
    };

    auto const closest_side_shape = [&] {
        return side_shape(AK::min<float>);
    };

    auto const farthest_side_shape = [&] {
        return side_shape(AK::max<float>);
    };

    auto const corner_distance = [&](auto distance_compare, Gfx::FloatPoint& corner) {
        auto top_left_distance = size.top_left().distance_from(center);
        auto top_right_distance = size.top_right().distance_from(center);
        auto bottom_right_distance = size.bottom_right().distance_from(center);
        auto bottom_left_distance = size.bottom_left().distance_from(center);
        auto distance = top_left_distance;
        if (distance_compare(top_right_distance, distance)) {
            corner = size.top_right();
            distance = top_right_distance;
        }
        if (distance_compare(bottom_right_distance, distance)) {
            corner = size.top_right();
            distance = bottom_right_distance;
        }
        if (distance_compare(bottom_left_distance, distance)) {
            corner = size.top_right();
            distance = bottom_left_distance;
        }
        return distance;
    };

    auto const closest_corner_distance = [&](Gfx::FloatPoint& corner) {
        return corner_distance([](float a, float b) { return a < b; }, corner);
    };

    auto const farthest_corner_distance = [&](Gfx::FloatPoint& corner) {
        return corner_distance([](float a, float b) { return a > b; }, corner);
    };

    auto const corner_shape = [&](auto corner_distance, auto get_shape) {
        Gfx::FloatPoint corner {};
        auto distance = corner_distance(corner);
        if (m_properties.ending_shape == EndingShape::Ellipse) {
            auto shape = get_shape();
            auto aspect_ratio = shape.width() / shape.height();
            auto p = corner - center;
            auto radius_a = AK::sqrt(p.y() * p.y() * aspect_ratio * aspect_ratio + p.x() * p.x());
            auto radius_b = radius_a / aspect_ratio;
            return Gfx::FloatSize { radius_a, radius_b };
        }
        return Gfx::FloatSize { distance, distance };
    };

    // https://w3c.github.io/csswg-drafts/css-images/#radial-gradient-syntax
    auto resolved_size = m_properties.size.visit(
        [&](Extent extent) {
            switch (extent) {
            case Extent::ClosestSide:
                // The ending shape is sized so that it exactly meets the side of the gradient box closest to the gradient’s center.
                // If the shape is an ellipse, it exactly meets the closest side in each dimension.
                return closest_side_shape();
            case Extent::ClosestCorner:
                // The ending shape is sized so that it passes through the corner of the gradient box closest to the gradient’s center.
                // If the shape is an ellipse, the ending shape is given the same aspect-ratio it would have if closest-side were specified
                return corner_shape(closest_corner_distance, closest_side_shape);
            case Extent::FarthestCorner:
                // Same as closest-corner, except the ending shape is sized based on the farthest corner.
                // If the shape is an ellipse, the ending shape is given the same aspect ratio it would have if farthest-side were specified.
                return corner_shape(farthest_corner_distance, farthest_side_shape);
            case Extent::FarthestSide:
                // Same as closest-side, except the ending shape is sized based on the farthest side(s).
                return farthest_side_shape();
            default:
                VERIFY_NOT_REACHED();
            }
        },
        [&](CircleSize const& circle_size) {
            auto radius = circle_size.radius.to_px(node);
            return Gfx::FloatSize { radius.to_float(), radius.to_float() };
        },
        [&](EllipseSize const& ellipse_size) {
            auto radius_a = ellipse_size.radius_a.resolved(node, CSS::Length::make_px(size.width())).to_px(node);
            auto radius_b = ellipse_size.radius_b.resolved(node, CSS::Length::make_px(size.height())).to_px(node);
            return Gfx::FloatSize { radius_a.to_float(), radius_b.to_float() };
        });

    // Handle degenerate cases
    // https://w3c.github.io/csswg-drafts/css-images/#degenerate-radials

    constexpr auto arbitrary_small_number = 1e-10;
    constexpr auto arbitrary_large_number = 1e10;

    // If the ending shape is a circle with zero radius:
    if (m_properties.ending_shape == EndingShape::Circle && resolved_size.is_empty()) {
        // Render as if the ending shape was a circle whose radius was an arbitrary very small number greater than zero.
        // This will make the gradient continue to look like a circle.
        return Gfx::FloatSize { arbitrary_small_number, arbitrary_small_number };
    }
    // If the ending shape has zero width (regardless of the height):
    if (resolved_size.width() <= 0) {
        // Render as if the ending shape was an ellipse whose height was an arbitrary very large number
        // and whose width was an arbitrary very small number greater than zero.
        // This will make the gradient look similar to a horizontal linear gradient that is mirrored across the center of the ellipse.
        // It also means that all color-stop positions specified with a percentage resolve to 0px.
        return Gfx::FloatSize { arbitrary_small_number, arbitrary_large_number };
    }
    // Otherwise, if the ending shape has zero height:
    if (resolved_size.height() <= 0) {
        // Render as if the ending shape was an ellipse whose width was an arbitrary very large number and whose height
        // was an arbitrary very small number greater than zero. This will make the gradient look like a solid-color image equal
        // to the color of the last color-stop, or equal to the average color of the gradient if it’s repeating.
        return Gfx::FloatSize { arbitrary_large_number, arbitrary_small_number };
    }
    return resolved_size;
}

void RadialGradientStyleValue::resolve_for_size(Layout::NodeWithStyleAndBoxModelMetrics const& node, CSSPixelSize paint_size) const
{
    CSSPixelRect gradient_box { { 0, 0 }, paint_size };
    auto center = m_properties.position.resolved(node, gradient_box).to_type<float>();
    auto gradient_size = resolve_size(node, center, gradient_box.to_type<float>());
    if (m_resolved.has_value() && m_resolved->gradient_size == gradient_size)
        return;
    m_resolved = ResolvedData {
        Painting::resolve_radial_gradient_data(node, gradient_size.to_type<CSSPixels>(), *this),
        gradient_size,
        center,
    };
}

bool RadialGradientStyleValue::equals(StyleValue const& other) const
{
    if (type() != other.type())
        return false;
    auto& other_gradient = other.as_radial_gradient();
    return m_properties == other_gradient.m_properties;
}

void RadialGradientStyleValue::paint(PaintContext& context, DevicePixelRect const& dest_rect, CSS::ImageRendering) const
{
    VERIFY(m_resolved.has_value());
    Painting::paint_radial_gradient(context, dest_rect, m_resolved->data,
        context.rounded_device_point(m_resolved->center.to_type<CSSPixels>()),
        context.rounded_device_size(m_resolved->gradient_size.to_type<CSSPixels>()));
}

}
