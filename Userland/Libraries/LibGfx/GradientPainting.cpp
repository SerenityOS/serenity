/*
 * Copyright (c) 2022-2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Math.h>
#include <LibGfx/Gradients.h>
#include <LibGfx/Painter.h>

#if defined(AK_COMPILER_GCC)
#    pragma GCC optimize("O3")
#endif
namespace Gfx {

// Note: This file implements the CSS gradients for LibWeb according to the spec.
// Please do not make ad-hoc changes that may break spec compliance!

static float color_stop_step(ColorStop const& previous_stop, ColorStop const& next_stop, float position)
{
    if (position < previous_stop.position)
        return 0;
    if (position > next_stop.position)
        return 1;
    // For any given point between the two color stops,
    // determine the point’s location as a percentage of the distance between the two color stops.
    // Let this percentage be P.
    auto stop_length = next_stop.position - previous_stop.position;
    // FIXME: Avoids NaNs... Still not quite correct?
    if (stop_length <= 0)
        return 1;
    auto p = (position - previous_stop.position) / stop_length;
    if (!next_stop.transition_hint.has_value())
        return p;
    if (*next_stop.transition_hint >= 1)
        return 0;
    if (*next_stop.transition_hint <= 0)
        return 1;
    // Let C, the color weighting at that point, be equal to P^(logH(.5)).
    auto c = AK::pow(p, AK::log<float>(0.5) / AK::log(*next_stop.transition_hint));
    // The color at that point is then a linear blend between the colors of the two color stops,
    // blending (1 - C) of the first stop and C of the second stop.
    return c;
}

class GradientLine {
public:
    GradientLine(int gradient_length, Span<ColorStop const> color_stops, Optional<float> repeat_length)
        : m_repeating { repeat_length.has_value() }
        , m_start_offset { round_to<int>((m_repeating ? color_stops.first().position : 0.0f) * gradient_length) }
    {
        // Avoid generating excessive amounts of colors when the not enough shades to fill that length.
        auto necessary_length = min<int>((color_stops.size() - 1) * 255, gradient_length);
        m_sample_scale = float(necessary_length) / gradient_length;
        // Note: color_count will be < gradient_length for repeating gradients.
        auto color_count = round_to<int>(repeat_length.value_or(1.0f) * necessary_length);
        m_gradient_line_colors.resize(color_count);
        // Note: color.mixed_with() performs premultiplied alpha mixing when necessary as defined in:
        // https://drafts.csswg.org/css-images/#coloring-gradient-line
        for (int loc = 0; loc < color_count; loc++) {
            auto relative_loc = float(loc + m_start_offset) / necessary_length;
            Color gradient_color = color_stops[0].color.mixed_with(
                color_stops[1].color,
                color_stop_step(color_stops[0], color_stops[1], relative_loc));
            for (size_t i = 1; i < color_stops.size() - 1; i++) {
                gradient_color = gradient_color.mixed_with(
                    color_stops[i + 1].color,
                    color_stop_step(color_stops[i], color_stops[i + 1], relative_loc));
            }
            m_gradient_line_colors[loc] = gradient_color;
            if (gradient_color.alpha() < 255)
                m_requires_blending = true;
        }
    }

    Color get_color(i64 index) const
    {
        return m_gradient_line_colors[clamp(index, 0, m_gradient_line_colors.size() - 1)];
    }

    Color sample_color(float loc) const
    {
        if (m_sample_scale != 1.0f)
            loc *= m_sample_scale;
        auto repeat_wrap_if_required = [&](i64 loc) {
            if (m_repeating)
                return (loc + m_start_offset) % static_cast<i64>(m_gradient_line_colors.size());
            return loc;
        };
        auto int_loc = static_cast<i64>(floor(loc));
        auto blend = loc - int_loc;
        auto color = get_color(repeat_wrap_if_required(int_loc));
        // Blend between the two neighbouring colors (this fixes some nasty aliasing issues at small angles)
        if (blend >= 0.004f)
            color = color.mixed_with(get_color(repeat_wrap_if_required(int_loc + 1)), blend);
        return color;
    }

    void paint_into_physical_rect(Painter& painter, IntRect rect, auto location_transform)
    {
        auto clipped_rect = rect.intersected(painter.clip_rect() * painter.scale());
        auto start_offset = clipped_rect.location() - rect.location();
        for (int y = 0; y < clipped_rect.height(); y++) {
            for (int x = 0; x < clipped_rect.width(); x++) {
                auto pixel = sample_color(location_transform(x + start_offset.x(), y + start_offset.y()));
                painter.set_physical_pixel(clipped_rect.location().translated(x, y), pixel, m_requires_blending);
            }
        }
    }

private:
    bool m_repeating;
    int m_start_offset;
    float m_sample_scale { 1 };
    Vector<Color, 1024> m_gradient_line_colors;

    bool m_requires_blending = false;
};

void Painter::fill_rect_with_linear_gradient(IntRect const& rect, Span<ColorStop const> const& color_stops, float angle, Optional<float> repeat_length)
{
    auto a_rect = to_physical(rect);
    if (a_rect.intersected(clip_rect() * scale()).is_empty())
        return;

    float normalized_angle = normalized_gradient_angle_radians(angle);
    float sin_angle, cos_angle;
    AK::sincos(normalized_angle, sin_angle, cos_angle);

    // Full length of the gradient
    auto gradient_length = calculate_gradient_length(a_rect.size(), sin_angle, cos_angle);
    IntPoint offset { cos_angle * (gradient_length / 2), sin_angle * (gradient_length / 2) };
    auto center = a_rect.translated(-a_rect.location()).center();
    auto start_point = center - offset;
    // Rotate gradient line to be horizontal
    auto rotated_start_point_x = start_point.x() * cos_angle - start_point.y() * -sin_angle;

    GradientLine gradient_line(gradient_length, color_stops, repeat_length);
    gradient_line.paint_into_physical_rect(*this, a_rect, [&](int x, int y) {
        return (x * cos_angle - (a_rect.height() - y) * -sin_angle) - rotated_start_point_x;
    });
}

void Painter::fill_rect_with_conic_gradient(IntRect const& rect, Span<ColorStop const> const& color_stops, IntPoint center, float start_angle, Optional<float> repeat_length)
{
    auto a_rect = to_physical(rect);
    if (a_rect.intersected(clip_rect() * scale()).is_empty())
        return;

    // FIXME: Do we need/want sub-degree accuracy for the gradient line?
    GradientLine gradient_line(360, color_stops, repeat_length);
    float normalized_start_angle = (360.0f - start_angle) + 90.0f;
    // Translate position/center to the center of the pixel (avoids some funky painting)
    auto center_point = FloatPoint { center * scale() }.translated(0.5, 0.5);
    // The flooring can make gradients that want soft edges look worse, so only floor if we have hard edges.
    // Which makes sure the hard edge stay hard edges :^)
    bool should_floor_angles = false;
    for (size_t i = 0; i < color_stops.size() - 1; i++) {
        if (color_stops[i + 1].position - color_stops[i].position <= 0.01f) {
            should_floor_angles = true;
            break;
        }
    }
    gradient_line.paint_into_physical_rect(*this, a_rect, [&](int x, int y) {
        auto point = FloatPoint { x, y } - center_point;
        // FIXME: We could probably get away with some approximation here:
        auto loc = fmod((AK::atan2(point.y(), point.x()) * 180.0f / AK::Pi<float> + 360.0f + normalized_start_angle), 360.0f);
        return should_floor_angles ? floor(loc) : loc;
    });
}

void Painter::fill_rect_with_radial_gradient(IntRect const& rect, Span<ColorStop const> const& color_stops, IntPoint center, IntSize size, Optional<float> repeat_length)
{
    auto a_rect = to_physical(rect);
    if (a_rect.intersected(clip_rect() * scale()).is_empty())
        return;

    // A conservative guesstimate on how many colors we need to generate:
    auto max_dimension = max(a_rect.width(), a_rect.height());
    auto max_visible_gradient = max(max_dimension / 2, min(size.width(), max_dimension));
    GradientLine gradient_line(max_visible_gradient, color_stops, repeat_length);
    auto center_point = FloatPoint { center * scale() }.translated(0.5, 0.5);
    gradient_line.paint_into_physical_rect(*this, a_rect, [&](int x, int y) {
        // FIXME: See if there's a more efficient calculation we do there :^)
        auto point = FloatPoint(x, y) - center_point;
        auto gradient_x = point.x() / size.width();
        auto gradient_y = point.y() / size.height();
        return AK::sqrt(gradient_x * gradient_x + gradient_y * gradient_y) * max_visible_gradient;
    });
}

}
