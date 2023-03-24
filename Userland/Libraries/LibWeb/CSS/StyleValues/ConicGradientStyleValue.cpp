/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022-2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ConicGradientStyleValue.h"
#include <LibWeb/CSS/Serialize.h>

namespace Web::CSS {

// FIXME: Temporary until AbstractImageStyleValue.h exists. (And the Serialize.h include above.)
static ErrorOr<void> serialize_color_stop_list(StringBuilder& builder, auto const& color_stop_list)
{
    bool first = true;
    for (auto const& element : color_stop_list) {
        if (!first)
            TRY(builder.try_append(", "sv));

        if (element.transition_hint.has_value())
            TRY(builder.try_appendff("{}, "sv, TRY(element.transition_hint->value.to_string())));

        TRY(serialize_a_srgb_value(builder, element.color_stop.color));
        for (auto position : Array { &element.color_stop.position, &element.color_stop.second_position }) {
            if (position->has_value())
                TRY(builder.try_appendff(" {}"sv, TRY((*position)->to_string())));
        }
        first = false;
    }
    return {};
}

ErrorOr<String> ConicGradientStyleValue::to_string() const
{
    StringBuilder builder;
    if (is_repeating())
        TRY(builder.try_append("repeating-"sv));
    TRY(builder.try_append("conic-gradient("sv));
    bool has_from_angle = false;
    bool has_at_position = false;
    if ((has_from_angle = m_properties.from_angle.to_degrees() != 0))
        TRY(builder.try_appendff("from {}", TRY(m_properties.from_angle.to_string())));
    if ((has_at_position = m_properties.position != PositionValue::center())) {
        if (has_from_angle)
            TRY(builder.try_append(' '));
        TRY(builder.try_appendff("at "sv));
        TRY(m_properties.position.serialize(builder));
    }
    if (has_from_angle || has_at_position)
        TRY(builder.try_append(", "sv));
    TRY(serialize_color_stop_list(builder, m_properties.color_stop_list));
    TRY(builder.try_append(')'));
    return builder.to_string();
}

void ConicGradientStyleValue::resolve_for_size(Layout::Node const& node, CSSPixelSize size) const
{
    if (!m_resolved.has_value())
        m_resolved = ResolvedData { Painting::resolve_conic_gradient_data(node, *this), {} };
    m_resolved->position = m_properties.position.resolved(node, CSSPixelRect { { 0, 0 }, size });
}

void ConicGradientStyleValue::paint(PaintContext& context, DevicePixelRect const& dest_rect, CSS::ImageRendering) const
{
    VERIFY(m_resolved.has_value());
    Painting::paint_conic_gradient(context, dest_rect, m_resolved->data, context.rounded_device_point(m_resolved->position));
}

bool ConicGradientStyleValue::equals(StyleValue const& other) const
{
    if (type() != other.type())
        return false;
    auto& other_gradient = other.as_conic_gradient();
    return m_properties == other_gradient.m_properties;
}

float ConicGradientStyleValue::angle_degrees() const
{
    return m_properties.from_angle.to_degrees();
}

}
