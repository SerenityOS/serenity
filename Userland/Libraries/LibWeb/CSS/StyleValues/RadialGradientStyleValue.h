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
#include <LibWeb/CSS/Position.h>
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

    static ErrorOr<ValueComparingNonnullRefPtr<RadialGradientStyleValue>> create(EndingShape ending_shape, Size size, PositionValue position, Vector<LinearColorStopListElement> color_stop_list, GradientRepeating repeating)
    {
        VERIFY(color_stop_list.size() >= 2);
        return adopt_nonnull_ref_or_enomem(new (nothrow) RadialGradientStyleValue(ending_shape, size, position, move(color_stop_list), repeating));
    }

    virtual ErrorOr<String> to_string() const override;

    void paint(PaintContext&, DevicePixelRect const& dest_rect, CSS::ImageRendering) const override;

    virtual bool equals(StyleValue const& other) const override;

    Vector<LinearColorStopListElement> const& color_stop_list() const
    {
        return m_properties.color_stop_list;
    }

    bool is_paintable() const override { return true; }

    void resolve_for_size(Layout::Node const&, CSSPixelSize) const override;

    Gfx::FloatSize resolve_size(Layout::Node const&, Gfx::FloatPoint, Gfx::FloatRect const&) const;

    bool is_repeating() const { return m_properties.repeating == GradientRepeating::Yes; }

    virtual ~RadialGradientStyleValue() override = default;

private:
    RadialGradientStyleValue(EndingShape ending_shape, Size size, PositionValue position, Vector<LinearColorStopListElement> color_stop_list, GradientRepeating repeating)
        : AbstractImageStyleValue(Type::RadialGradient)
        , m_properties { .ending_shape = ending_shape, .size = size, .position = position, .color_stop_list = move(color_stop_list), .repeating = repeating }
    {
    }

    struct Properties {
        EndingShape ending_shape;
        Size size;
        PositionValue position;
        Vector<LinearColorStopListElement> color_stop_list;
        GradientRepeating repeating;
        bool operator==(Properties const&) const = default;
    } m_properties;

    struct ResolvedData {
        Painting::RadialGradientData data;
        Gfx::FloatSize gradient_size;
        Gfx::FloatPoint center;
    };

    mutable Optional<ResolvedData> m_resolved;
};

}
