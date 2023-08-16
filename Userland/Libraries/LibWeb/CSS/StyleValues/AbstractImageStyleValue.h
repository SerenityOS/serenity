/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022-2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/CSS/Enums.h>
#include <LibWeb/CSS/PercentageOr.h>
#include <LibWeb/CSS/Serialize.h>
#include <LibWeb/CSS/StyleValue.h>

namespace Web::CSS {

class AbstractImageStyleValue : public StyleValue {
public:
    using StyleValue::StyleValue;

    virtual Optional<CSSPixels> natural_width() const { return {}; }
    virtual Optional<CSSPixels> natural_height() const { return {}; }

    virtual void load_any_resources(DOM::Document&) {};
    virtual void resolve_for_size(Layout::NodeWithStyleAndBoxModelMetrics const&, CSSPixelSize) const {};

    virtual bool is_paintable() const = 0;
    virtual void paint(PaintContext& context, DevicePixelRect const& dest_rect, ImageRendering) const = 0;
};

// And now, some gradient related things. Maybe these should live somewhere else.

enum class GradientRepeating {
    Yes,
    No
};

template<typename TPosition>
struct ColorStopListElement {
    using PositionType = TPosition;
    struct ColorHint {
        TPosition value;
        inline bool operator==(ColorHint const&) const = default;
    };

    Optional<ColorHint> transition_hint;
    struct ColorStop {
        RefPtr<StyleValue> color;
        Optional<TPosition> position;
        Optional<TPosition> second_position = {};
        inline bool operator==(ColorStop const&) const = default;
    } color_stop;

    inline bool operator==(ColorStopListElement const&) const = default;
};

using LinearColorStopListElement = ColorStopListElement<LengthPercentage>;
using AngularColorStopListElement = ColorStopListElement<AnglePercentage>;

static ErrorOr<void> serialize_color_stop_list(StringBuilder& builder, auto const& color_stop_list)
{
    bool first = true;
    for (auto const& element : color_stop_list) {
        if (!first)
            TRY(builder.try_append(", "sv));

        if (element.transition_hint.has_value())
            TRY(builder.try_appendff("{}, "sv, TRY(element.transition_hint->value.to_string())));

        TRY(builder.try_append(TRY(element.color_stop.color->to_string())));
        for (auto position : Array { &element.color_stop.position, &element.color_stop.second_position }) {
            if (position->has_value())
                TRY(builder.try_appendff(" {}"sv, TRY((*position)->to_string())));
        }
        first = false;
    }
    return {};
}

}
