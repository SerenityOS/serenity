/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/PaintStyle.h>
#include <LibGfx/Path.h>
#include <LibWeb/DOM/Node.h>
#include <LibWeb/SVG/AttributeParser.h>
#include <LibWeb/SVG/SVGElement.h>
#include <LibWeb/SVG/SVGGradientElement.h>
#include <LibWeb/SVG/TagNames.h>

namespace Web::SVG {

class SVGGraphicsElement : public SVGElement {
    WEB_PLATFORM_OBJECT(SVGGraphicsElement, SVGElement);

public:
    virtual void apply_presentational_hints(CSS::StyleProperties&) const override;

    virtual void parse_attribute(DeprecatedFlyString const& name, DeprecatedString const& value) override;

    Optional<Gfx::Color> fill_color() const;
    Gfx::Painter::WindingRule fill_rule() const;
    Optional<Gfx::Color> stroke_color() const;
    Optional<float> stroke_width() const;

    float visible_stroke_width() const
    {
        if (auto color = stroke_color(); color.has_value() && color->alpha() > 0)
            return stroke_width().value_or(0);
        return 0;
    }

    Gfx::AffineTransform get_transform() const;

    Optional<Gfx::PaintStyle const&> fill_paint_style(SVGPaintContext const&) const;

protected:
    SVGGraphicsElement(DOM::Document&, DOM::QualifiedName);

    virtual JS::ThrowCompletionOr<void> initialize(JS::Realm&) override;

    Optional<float> m_fill_opacity = {};
    Gfx::AffineTransform m_transform = {};
};

Gfx::AffineTransform transform_from_transform_list(ReadonlySpan<Transform> transform_list);

}
