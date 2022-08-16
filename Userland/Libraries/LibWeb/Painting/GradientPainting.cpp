/*
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Checked.h>
#include <AK/Math.h>
#include <LibGfx/Gamma.h>
#include <LibGfx/Line.h>
#include <LibWeb/CSS/StyleValue.h>
#include <LibWeb/Painting/GradientPainting.h>

namespace Web::Painting {

static float normalized_gradient_angle_radians(float gradient_angle)
{
    // Adjust angle so 0 degrees is bottom
    float real_angle = 90 - gradient_angle;
    return real_angle * (AK::Pi<float> / 180);
}

static float calulate_gradient_length(Gfx::IntSize const& gradient_size, float sin_angle, float cos_angle)
{
    return AK::fabs(gradient_size.height() * sin_angle) + AK::fabs(gradient_size.width() * cos_angle);
}

static float calulate_gradient_length(Gfx::IntSize const& gradient_size, float gradient_angle)
{
    float angle = normalized_gradient_angle_radians(gradient_angle);
    float sin_angle, cos_angle;
    AK::sincos(angle, sin_angle, cos_angle);
    return calulate_gradient_length(gradient_size, sin_angle, cos_angle);
}

LinearGradientData resolve_linear_gradient_data(Layout::Node const& node, Gfx::FloatSize const& gradient_size, CSS::LinearGradientStyleValue const& linear_gradient)
{
    auto& color_stop_list = linear_gradient.color_stop_list();

    VERIFY(color_stop_list.size() >= 2);
    ColorStopList resolved_color_stops;
    resolved_color_stops.ensure_capacity(color_stop_list.size());
    for (auto& stop : color_stop_list)
        resolved_color_stops.append(ColorStop { .color = stop.color_stop.color });

    auto gradient_angle = linear_gradient.angle_degrees(gradient_size);
    auto gradient_length_px = calulate_gradient_length(gradient_size.to_rounded<int>(), gradient_angle);
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

    // 2. If a color stop or transition hint has a position that is less than the
    //    specified position of any color stop or transition hint before it in the list,
    //    set its position to be equal to the largest specified position of any color stop
    //    or transition hint before it.
    auto max_previous_color_stop_or_hint = resolved_color_stops[0].position;
    for (size_t i = 1; i < color_stop_list.size(); i++) {
        auto& stop = color_stop_list[i];
        if (stop.transition_hint.has_value()) {
            float value = stop.transition_hint->value.resolved(node, gradient_length).to_px(node);
            value = max(value, max_previous_color_stop_or_hint);
            resolved_color_stops[i].transition_hint = value;
            max_previous_color_stop_or_hint = value;
        }
        if (stop.color_stop.length.has_value()) {
            float value = stop.color_stop.length->resolved(node, gradient_length).to_px(node);
            value = max(value, max_previous_color_stop_or_hint);
            resolved_color_stops[i].position = value;
            max_previous_color_stop_or_hint = value;
        }
    }

    // 3. If any color stop still does not have a position, then, for each run of adjacent color stops
    //    without positions, set their positions so that they are evenly spaced between the preceding
    //    and following color stops with positions.
    // Note: Though not mentioned anywhere in the specification transition hints are counted as "color stops with positions".
    size_t i = 1;
    auto find_run_end = [&] {
        auto color_stop_has_position = [](auto& color_stop) {
            return color_stop.transition_hint.has_value() || color_stop.color_stop.length.has_value();
        };
        while (i < color_stop_list.size() - 1 && !color_stop_has_position(color_stop_list[i])) {
            i++;
        }
        return i;
    };
    while (i < color_stop_list.size() - 1) {
        auto& stop = color_stop_list[i];
        if (!stop.color_stop.length.has_value()) {
            auto run_start = i - 1;
            auto start_position = resolved_color_stops[i++].transition_hint.value_or(resolved_color_stops[run_start].position);
            auto run_end = find_run_end();
            auto end_position = resolved_color_stops[run_end].transition_hint.value_or(resolved_color_stops[run_end].position);
            auto spacing = (end_position - start_position) / (run_end - run_start);
            for (auto j = run_start + 1; j < run_end; j++) {
                resolved_color_stops[j].position = start_position + (j - run_start) * spacing;
            }
        }
        i++;
    }

    // Determine the location of the transition hint as a percentage of the distance between the two color stops,
    // denoted as a number between 0 and 1, where 0 indicates the hint is placed right on the first color stop,
    // and 1 indicates the hint is placed right on the second color stop.
    for (size_t i = 1; i < resolved_color_stops.size(); i++) {
        auto& color_stop = resolved_color_stops[i];
        auto& previous_color_stop = resolved_color_stops[i - 1];
        if (color_stop.transition_hint.has_value()) {
            auto stop_length = color_stop.position - previous_color_stop.position;
            color_stop.transition_hint = stop_length > 0 ? (*color_stop.transition_hint - previous_color_stop.position) / stop_length : 0;
        }
    }

    Optional<float> repeat_length = {};
    if (linear_gradient.is_repeating())
        repeat_length = resolved_color_stops.last().position - resolved_color_stops.first().position;

    return { gradient_angle, resolved_color_stops, repeat_length };
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

    // Full length of the gradient
    auto length = calulate_gradient_length(gradient_rect.size(), sin_angle, cos_angle);

    Gfx::FloatPoint offset { cos_angle * (length / 2), sin_angle * (length / 2) };

    auto center = gradient_rect.translated(-gradient_rect.location()).center();
    auto start_point = center.to_type<float>() - offset;

    // Rotate gradient line to be horizontal
    auto rotated_start_point_x = start_point.x() * cos_angle - start_point.y() * -sin_angle;

    auto color_stop_step = [&](auto& previous_stop, auto& next_stop, float position) -> float {
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
    };

    Vector<Gfx::Color, 1024> gradient_line_colors;
    auto gradient_color_count = round_to<int>(data.repeat_length.value_or(length));
    gradient_line_colors.resize(gradient_color_count);
    auto& color_stops = data.color_stops;
    auto start_offset = data.repeat_length.has_value() ? color_stops.first().position : 0.0f;
    auto start_offset_int = round_to<int>(start_offset);
    for (int loc = 0; loc < gradient_color_count; loc++) {
        Gfx::Color gradient_color = color_mix(
            color_stops[0].color,
            color_stops[1].color,
            color_stop_step(
                color_stops[0],
                color_stops[1],
                loc + start_offset_int));
        for (size_t i = 1; i < color_stops.size() - 1; i++) {
            gradient_color = color_mix(
                gradient_color,
                color_stops[i + 1].color,
                color_stop_step(
                    color_stops[i],
                    color_stops[i + 1],
                    loc + start_offset_int));
        }
        gradient_line_colors[loc] = gradient_color;
    }

    auto lookup_color = [&](int loc) {
        return gradient_line_colors[clamp(loc, 0, gradient_color_count - 1)];
    };

    for (int y = 0; y < gradient_rect.height(); y++) {
        for (int x = 0; x < gradient_rect.width(); x++) {
            auto loc = (x * cos_angle - (gradient_rect.height() - y) * -sin_angle) - rotated_start_point_x - start_offset;
            if (data.repeat_length.has_value()) {
                loc = AK::fmod(loc, *data.repeat_length);
                if (loc < 0)
                    loc = *data.repeat_length + loc;
            }
            // Blend between the two neighbouring colors (this fixes some nasty aliasing issues at small angles)
            auto blend = loc - static_cast<int>(loc);
            auto gradient_color = color_mix(lookup_color(loc - 1), lookup_color(loc), blend);
            context.painter().set_pixel(gradient_rect.x() + x, gradient_rect.y() + y, gradient_color, gradient_color.alpha() < 255);
        }
    }
}

}
