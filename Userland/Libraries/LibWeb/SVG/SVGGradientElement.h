/*
 * Copyright (c) 2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/IterationDecision.h>
#include <LibGfx/PaintStyle.h>
#include <LibWeb/SVG/AttributeParser.h>
#include <LibWeb/SVG/SVGElement.h>
#include <LibWeb/SVG/SVGStopElement.h>

namespace Web::SVG {

struct SVGPaintContext {
    Gfx::FloatRect viewport;
    Gfx::FloatRect path_bounding_box;
    Gfx::AffineTransform transform;
};

inline Gfx::SVGGradientPaintStyle::SpreadMethod to_gfx_spread_method(SpreadMethod spread_method)
{
    switch (spread_method) {
    case SpreadMethod::Pad:
        return Gfx::SVGGradientPaintStyle::SpreadMethod::Pad;
    case SpreadMethod::Reflect:
        return Gfx::SVGGradientPaintStyle::SpreadMethod::Reflect;
    case SpreadMethod::Repeat:
        return Gfx::SVGGradientPaintStyle::SpreadMethod::Repeat;
    default:
        VERIFY_NOT_REACHED();
    }
}

class SVGGradientElement : public SVGElement {
    WEB_PLATFORM_OBJECT(SVGGradientElement, SVGElement);

public:
    virtual ~SVGGradientElement() override = default;

    virtual void attribute_changed(DeprecatedFlyString const& name, DeprecatedString const& value) override;

    virtual Optional<Gfx::PaintStyle const&> to_gfx_paint_style(SVGPaintContext const&) const = 0;

    GradientUnits gradient_units() const;

    SpreadMethod spread_method() const;

    Optional<Gfx::AffineTransform> gradient_transform() const;

protected:
    SVGGradientElement(DOM::Document&, DOM::QualifiedName);

    virtual void initialize(JS::Realm&) override;

    JS::GCPtr<SVGGradientElement const> linked_gradient() const;

    Gfx::AffineTransform gradient_paint_transform(SVGPaintContext const&) const;

    template<VoidFunction<SVGStopElement> Callback>
    void for_each_color_stop(Callback const& callback) const
    {
        bool color_stops_found = false;
        for_each_child_of_type<SVG::SVGStopElement>([&](auto& stop) {
            color_stops_found = true;
            callback(stop);
        });
        if (!color_stops_found) {
            if (auto gradient = linked_gradient())
                gradient->for_each_color_stop(callback);
        }
    }

    void add_color_stops(Gfx::SVGGradientPaintStyle&) const;

private:
    Optional<GradientUnits> m_gradient_units = {};
    Optional<SpreadMethod> m_spread_method = {};
    Optional<Gfx::AffineTransform> m_gradient_transform = {};
};

}
