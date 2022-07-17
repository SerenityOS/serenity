/*
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Checked.h>
#include <AK/Math.h>
#include <LibGfx/Gamma.h>
#include <LibGfx/Line.h>
#include <LibWeb/Painting/GradientPainting.h>

namespace Web::Painting {

static float normalized_gradient_angle_radians(float gradient_angle)
{
    // Adjust angle so 0 degrees is bottom
    float real_angle = 90 - gradient_angle;
    return real_angle * (AK::Pi<float> / 180);
}

static float calulate_gradient_length(Gfx::IntRect const& gradient_rect, float sin_angle, float cos_angle)
{
    return AK::fabs(gradient_rect.height() * sin_angle) + AK::fabs(gradient_rect.width() * cos_angle);
}

static float calulate_gradient_length(Gfx::IntRect const& gradient_rect, float gradient_angle)
{
    float angle = normalized_gradient_angle_radians(gradient_angle);
    float sin_angle, cos_angle;
    AK::sincos(angle, sin_angle, cos_angle);
    return calulate_gradient_length(gradient_rect, sin_angle, cos_angle);
}

LinearGradientData resolve_linear_gradient_data(Layout::Node const& node, Gfx::FloatRect const& gradient_rect, CSS::LinearGradientStyleValue const& linear_gradient)
{
    auto& color_stop_list = linear_gradient.color_stop_list();

    VERIFY(color_stop_list.size() >= 2);
    ColorStopList resolved_color_stops;
    resolved_color_stops.ensure_capacity(color_stop_list.size());
    for (auto& stop : color_stop_list)
        resolved_color_stops.append(ColorStop { .color = stop.color_stop.color });

    auto gradient_angle = linear_gradient.angle_degrees(gradient_rect);
    auto gradient_length_px = calulate_gradient_length(gradient_rect.to_rounded<int>(), gradient_angle);
    auto gradient_length = CSS::Length::make_px(gradient_length_px);

    // 1. If the first color stop does not have a position, set its position to 0%.
    auto& first_stop = color_stop_list.first().color_stop;
    resolved_color_stops.first().position = first_stop.length.has_value()
        ? first_stop.length->resolved(node, gradient_length).to_px(node)
        : 0;
    //    If the last color stop does not have a position, set its position to 100%
    auto& last_stop = color_stop_list.last().color_stop;
    resolved_color_stops.last().position = last_stop.length.has_value()
        ? last_stop.length->resolved(node, gradient_length).to_px(node)
        : gradient_length_px;

    // FIXME: Handle transition hints
    // 2. If a color stop or transition hint has a position that is less than the
    //    specified position of any color stop or transition hint before it in the list,
    //    set its position to be equal to the largest specified position of any color stop
    //    or transition hint before it.
    auto max_previous_color_stop = resolved_color_stops[0].position;
    for (size_t i = 1; i < color_stop_list.size(); i++) {
        auto& stop = color_stop_list[i];
        if (stop.color_stop.length.has_value()) {
            float value = stop.color_stop.length->resolved(node, gradient_length).to_px(node);
            value = max(value, max_previous_color_stop);
            resolved_color_stops[i].position = value;
            max_previous_color_stop = value;
        }
    }

    // 3. If any color stop still does not have a position, then, for each run of adjacent color stops
    //    without positions, set their positions so that they are evenly spaced between the preceding
    //    and following color stops with positions.
    size_t i = 1;
    auto find_run_end = [&] {
        while (i < color_stop_list.size() - 1 && !color_stop_list[i].color_stop.length.has_value()) {
            i++;
        }
        return i;
    };
    while (i < color_stop_list.size() - 1) {
        auto& stop = color_stop_list[i];
        if (!stop.color_stop.length.has_value()) {
            auto run_start = i - 1;
            auto run_end = find_run_end();
            auto start_position = resolved_color_stops[run_start].position;
            auto end_position = resolved_color_stops[run_end].position;
            auto spacing = (end_position - start_position) / (run_end - run_start);
            for (auto j = run_start + 1; j < run_end; j++) {
                resolved_color_stops[j].position = start_position + (j - run_start) * spacing;
            }
        }
        i++;
    }

    return { gradient_angle, resolved_color_stops };
}

static float mix(float x, float y, float a)
{
    return x * (1 - a) + y * a;
}

// Note: Gfx::gamma_accurate_blend() is NOT correct for linear gradients!
static Gfx::Color color_mix(Gfx::Color x, Gfx::Color y, float a)
{
    if (x.alpha() == y.alpha() || x.with_alpha(0) == y.with_alpha(0)) {
        return Gfx::Color {
            round_to<u8>(mix(x.red(), y.red(), a)),
            round_to<u8>(mix(x.green(), y.green(), a)),
            round_to<u8>(mix(x.blue(), y.blue(), a)),
            round_to<u8>(mix(x.alpha(), y.alpha(), a)),
        };
    }
    // Use slower but more visually pleasing premultiplied alpha mixing if both the color and alpha differ.
    // https://drafts.csswg.org/css-images/#coloring-gradient-line
    auto mixed_alpha = mix(x.alpha(), y.alpha(), a);
    auto premultiplied_mix_channel = [&](float channel_x, float channel_y, float a) {
        return round_to<u8>(mix(channel_x * (x.alpha() / 255.0f), channel_y * (y.alpha() / 255.0f), a) / (mixed_alpha / 255.0f));
    };
    return Gfx::Color {
        premultiplied_mix_channel(x.red(), y.red(), a),
        premultiplied_mix_channel(x.green(), y.green(), a),
        premultiplied_mix_channel(x.blue(), y.blue(), a),
        round_to<u8>(mixed_alpha),
    };
}

void paint_linear_gradient(PaintContext& context, Gfx::IntRect const& gradient_rect, LinearGradientData const& data)
{
    float angle = normalized_gradient_angle_radians(data.gradient_angle);
    float sin_angle, cos_angle;
    AK::sincos(angle, sin_angle, cos_angle);

    auto length = calulate_gradient_length(gradient_rect, sin_angle, cos_angle);

    Gfx::FloatPoint offset { cos_angle * (length / 2), sin_angle * (length / 2) };

    auto center = gradient_rect.translated(-gradient_rect.location()).center();
    auto start_point = center.to_type<float>() - offset;

    // Rotate gradient line to be horizontal
    auto rotated_start_point_x = start_point.x() * cos_angle - start_point.y() * -sin_angle;

    // FIXME: Handle transition hint interpolation
    auto linear_step = [](float min, float max, float value) -> float {
        if (value < min)
            return 0.;
        if (value > max)
            return 1.;
        return (value - min) / (max - min);
    };

    Vector<Gfx::Color, 1024> gradient_line_colors;
    auto int_length = round_to<int>(length);
    gradient_line_colors.resize(int_length);
    auto& color_stops = data.color_stops;
    for (int loc = 0; loc < int_length; loc++) {
        Gfx::Color gradient_color = color_mix(
            color_stops[0].color,
            color_stops[1].color,
            linear_step(
                color_stops[0].position,
                color_stops[1].position,
                loc));
        for (size_t i = 1; i < color_stops.size() - 1; i++) {
            gradient_color = color_mix(
                gradient_color,
                color_stops[i + 1].color,
                linear_step(
                    color_stops[i].position,
                    color_stops[i + 1].position,
                    loc));
        }
        gradient_line_colors[loc] = gradient_color;
    }

    auto lookup_color = [&](int loc) {
        return gradient_line_colors[clamp(loc, 0, int_length - 1)];
    };

    for (int y = 0; y < gradient_rect.height(); y++) {
        for (int x = 0; x < gradient_rect.width(); x++) {
            auto loc = (x * cos_angle - (gradient_rect.height() - y) * -sin_angle) - rotated_start_point_x;
            // Blend between the two neighbouring colors (this fixes some nasty aliasing issues at small angles)
            auto blend = loc - static_cast<int>(loc);
            auto gradient_color = color_mix(lookup_color(loc - 1), lookup_color(loc), blend);
            context.painter().set_pixel(gradient_rect.x() + x, gradient_rect.y() + y, gradient_color, gradient_color.alpha() < 255);
        }
    }
}

}
