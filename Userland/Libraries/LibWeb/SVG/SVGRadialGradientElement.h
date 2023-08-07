/*
 * Copyright (c) 2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/SVG/AttributeParser.h>
#include <LibWeb/SVG/SVGAnimatedLength.h>
#include <LibWeb/SVG/SVGGradientElement.h>

namespace Web::SVG {

class SVGRadialGradientElement : public SVGGradientElement {
    WEB_PLATFORM_OBJECT(SVGRadialGradientElement, SVGGradientElement);

public:
    virtual ~SVGRadialGradientElement() override = default;

    virtual void attribute_changed(DeprecatedFlyString const& name, DeprecatedString const& value) override;

    virtual Optional<Gfx::PaintStyle const&> to_gfx_paint_style(SVGPaintContext const&) const override;

    JS::NonnullGCPtr<SVGAnimatedLength> cx() const;
    JS::NonnullGCPtr<SVGAnimatedLength> cy() const;
    JS::NonnullGCPtr<SVGAnimatedLength> fx() const;
    JS::NonnullGCPtr<SVGAnimatedLength> fy() const;
    JS::NonnullGCPtr<SVGAnimatedLength> fr() const;
    JS::NonnullGCPtr<SVGAnimatedLength> r() const;

protected:
    SVGRadialGradientElement(DOM::Document&, DOM::QualifiedName);

    virtual void initialize(JS::Realm&) override;

private:
    JS::GCPtr<SVGRadialGradientElement const> linked_radial_gradient() const
    {
        if (auto gradient = linked_gradient(); gradient && is<SVGRadialGradientElement>(*gradient))
            return &verify_cast<SVGRadialGradientElement>(*gradient);
        return {};
    }

    NumberPercentage start_circle_x() const;
    NumberPercentage start_circle_y() const;
    NumberPercentage start_circle_radius() const;
    NumberPercentage end_circle_x() const;
    NumberPercentage end_circle_y() const;
    NumberPercentage end_circle_radius() const;

    Optional<NumberPercentage> m_cx;
    Optional<NumberPercentage> m_cy;
    Optional<NumberPercentage> m_fx;
    Optional<NumberPercentage> m_fy;
    Optional<NumberPercentage> m_fr;
    Optional<NumberPercentage> m_r;

    mutable RefPtr<Gfx::SVGRadialGradientPaintStyle> m_paint_style;
};

}
