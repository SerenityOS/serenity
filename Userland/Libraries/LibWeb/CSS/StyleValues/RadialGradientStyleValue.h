/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022-2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibWeb/CSS/Enums.h>
#include <LibWeb/CSS/StyleValues/AbstractImageStyleValue.h>
#include <LibWeb/Painting/GradientPainting.h>

namespace Web::CSS {

class RadialGradientStyleValue final : public AbstractImageStyleValue {
public:
    enum class EndingShape {
        Circle,
        Ellipse
    };

    enum class Extent {
        ClosestCorner,
        ClosestSide,
        FarthestCorner,
        FarthestSide
    };

    struct CircleSize {
        Length radius;
        bool operator==(CircleSize const&) const = default;
    };

    struct EllipseSize {
        LengthPercentage radius_a;
        LengthPercentage radius_b;
        bool operator==(EllipseSize const&) const = default;
    };

    using Size = Variant<Extent, CircleSize, EllipseSize>;

    static ValueComparingNonnullRefPtr<RadialGradientStyleValue> create(EndingShape ending_shape, Size size, ValueComparingNonnullRefPtr<PositionStyleValue> position, Vector<LinearColorStopListElement> color_stop_list, GradientRepeating repeating)
    {
        VERIFY(color_stop_list.size() >= 2);
        return adopt_ref(*new (nothrow) RadialGradientStyleValue(ending_shape, size, move(position), move(color_stop_list), repeating));
    }

    virtual String to_string() const override;

    void paint(PaintContext&, DevicePixelRect const& dest_rect, CSS::ImageRendering, Vector<Gfx::Path> const& clip_paths = {}) const override;

    virtual bool equals(CSSStyleValue const& other) const override;

    Vector<LinearColorStopListElement> const& color_stop_list() const
    {
        return m_properties.color_stop_list;
    }

    bool is_paintable() const override { return true; }

    void resolve_for_size(Layout::NodeWithStyleAndBoxModelMetrics const&, CSSPixelSize) const override;

    CSSPixelSize resolve_size(Layout::Node const&, CSSPixelPoint, CSSPixelRect const&) const;

    bool is_repeating() const { return m_properties.repeating == GradientRepeating::Yes; }

    virtual ~RadialGradientStyleValue() override = default;

private:
    RadialGradientStyleValue(EndingShape ending_shape, Size size, ValueComparingNonnullRefPtr<PositionStyleValue> position, Vector<LinearColorStopListElement> color_stop_list, GradientRepeating repeating)
        : AbstractImageStyleValue(Type::RadialGradient)
        , m_properties { .ending_shape = ending_shape, .size = size, .position = move(position), .color_stop_list = move(color_stop_list), .repeating = repeating }
    {
    }

    struct Properties {
        EndingShape ending_shape;
        Size size;
        ValueComparingNonnullRefPtr<PositionStyleValue> position;
        Vector<LinearColorStopListElement> color_stop_list;
        GradientRepeating repeating;
        bool operator==(Properties const&) const = default;
    } m_properties;

    struct ResolvedData {
        Painting::RadialGradientData data;
        CSSPixelSize gradient_size;
        CSSPixelPoint center;
    };

    mutable Optional<ResolvedData> m_resolved;
};

}
