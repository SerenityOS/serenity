/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022-2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/CSS/Angle.h>
#include <LibWeb/CSS/Position.h>
#include <LibWeb/CSS/StyleValues/AbstractImageStyleValue.h>
#include <LibWeb/Painting/GradientPainting.h>

namespace Web::CSS {

class ConicGradientStyleValue final : public AbstractImageStyleValue {
public:
    static ValueComparingNonnullRefPtr<ConicGradientStyleValue> create(Angle from_angle, PositionValue position, Vector<AngularColorStopListElement> color_stop_list, GradientRepeating repeating)
    {
        VERIFY(color_stop_list.size() >= 2);
        return adopt_ref(*new (nothrow) ConicGradientStyleValue(from_angle, position, move(color_stop_list), repeating));
    }

    virtual ErrorOr<String> to_string() const override;

    void paint(PaintContext&, DevicePixelRect const& dest_rect, CSS::ImageRendering) const override;

    virtual bool equals(StyleValue const& other) const override;

    Vector<AngularColorStopListElement> const& color_stop_list() const
    {
        return m_properties.color_stop_list;
    }

    float angle_degrees() const;

    bool is_paintable() const override { return true; }

    void resolve_for_size(Layout::NodeWithStyleAndBoxModelMetrics const&, CSSPixelSize) const override;

    virtual ~ConicGradientStyleValue() override = default;

    bool is_repeating() const { return m_properties.repeating == GradientRepeating::Yes; }

private:
    ConicGradientStyleValue(Angle from_angle, PositionValue position, Vector<AngularColorStopListElement> color_stop_list, GradientRepeating repeating)
        : AbstractImageStyleValue(Type::ConicGradient)
        , m_properties { .from_angle = from_angle, .position = position, .color_stop_list = move(color_stop_list), .repeating = repeating }
    {
    }

    struct Properties {
        // FIXME: Support <color-interpolation-method>
        Angle from_angle;
        PositionValue position;
        Vector<AngularColorStopListElement> color_stop_list;
        GradientRepeating repeating;
        bool operator==(Properties const&) const = default;
    } m_properties;

    struct ResolvedData {
        Painting::ConicGradientData data;
        CSSPixelPoint position;
    };

    mutable Optional<ResolvedData> m_resolved;
};

}
