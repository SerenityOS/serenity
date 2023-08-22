/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022-2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FilterValueListStyleValue.h"
#include <LibWeb/CSS/Serialize.h>
#include <LibWeb/Layout/Node.h>

namespace Web::CSS {

float Filter::Blur::resolved_radius(Layout::Node const& node) const
{
    // Default value when omitted is 0px.
    auto sigma = 0;
    if (radius.has_value())
        sigma = radius->to_px(node).to_int();
    // Note: The radius/sigma of the blur needs to be doubled for LibGfx's blur functions.
    return sigma * 2;
}

Filter::DropShadow::Resolved Filter::DropShadow::resolved(Layout::Node const& node) const
{
    // The default value for omitted values is missing length values set to 0
    // and the missing used color is taken from the color property.
    return Resolved {
        offset_x.to_px(node).to_double(),
        offset_y.to_px(node).to_double(),
        radius.has_value() ? radius->to_px(node).to_double() : 0.0,
        color.has_value() ? *color : node.computed_values().color()
    };
}

float Filter::HueRotate::angle_degrees() const
{
    // Default value when omitted is 0deg.
    if (!angle.has_value())
        return 0.0f;
    return angle->visit([&](Angle const& a) { return a.to_degrees(); }, [&](auto) { return 0.0; });
}

float Filter::Color::resolved_amount() const
{
    if (amount.has_value()) {
        if (amount->is_percentage())
            return amount->percentage().as_fraction();
        return amount->number().value();
    }
    // All color filters (brightness, sepia, etc) have a default amount of 1.
    return 1.0f;
}

ErrorOr<String> FilterValueListStyleValue::to_string() const
{
    StringBuilder builder {};
    bool first = true;
    for (auto& filter_function : filter_value_list()) {
        if (!first)
            TRY(builder.try_append(' '));
        TRY(filter_function.visit(
            [&](Filter::Blur const& blur) -> ErrorOr<void> {
                TRY(builder.try_append("blur("sv));
                if (blur.radius.has_value())
                    TRY(builder.try_append(TRY(blur.radius->to_string())));
                return {};
            },
            [&](Filter::DropShadow const& drop_shadow) -> ErrorOr<void> {
                TRY(builder.try_appendff("drop-shadow({} {}"sv,
                    drop_shadow.offset_x, drop_shadow.offset_y));
                if (drop_shadow.radius.has_value())
                    TRY(builder.try_appendff(" {}", TRY(drop_shadow.radius->to_string())));
                if (drop_shadow.color.has_value()) {
                    TRY(builder.try_append(' '));
                    serialize_a_srgb_value(builder, *drop_shadow.color);
                }
                return {};
            },
            [&](Filter::HueRotate const& hue_rotate) -> ErrorOr<void> {
                TRY(builder.try_append("hue-rotate("sv));
                if (hue_rotate.angle.has_value()) {
                    TRY(hue_rotate.angle->visit(
                        [&](Angle const& angle) -> ErrorOr<void> {
                            return builder.try_append(TRY(angle.to_string()));
                        },
                        [&](auto&) -> ErrorOr<void> {
                            return builder.try_append('0');
                        }));
                }
                return {};
            },
            [&](Filter::Color const& color) -> ErrorOr<void> {
                TRY(builder.try_appendff("{}(",
                    [&] {
                        switch (color.operation) {
                        case Filter::Color::Operation::Brightness:
                            return "brightness"sv;
                        case Filter::Color::Operation::Contrast:
                            return "contrast"sv;
                        case Filter::Color::Operation::Grayscale:
                            return "grayscale"sv;
                        case Filter::Color::Operation::Invert:
                            return "invert"sv;
                        case Filter::Color::Operation::Opacity:
                            return "opacity"sv;
                        case Filter::Color::Operation::Saturate:
                            return "saturate"sv;
                        case Filter::Color::Operation::Sepia:
                            return "sepia"sv;
                        default:
                            VERIFY_NOT_REACHED();
                        }
                    }()));
                if (color.amount.has_value())
                    TRY(builder.try_append(TRY(color.amount->to_string())));
                return {};
            }));
        TRY(builder.try_append(')'));
        first = false;
    }
    return builder.to_string();
}

}
