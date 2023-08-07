/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/SVG/SVGGeometryElement.h>

namespace Web::SVG {

// https://www.w3.org/TR/SVG11/shapes.html#RectElement
class SVGRectElement final : public SVGGeometryElement {
    WEB_PLATFORM_OBJECT(SVGRectElement, SVGGeometryElement);

public:
    virtual ~SVGRectElement() override = default;

    virtual void attribute_changed(DeprecatedFlyString const& name, DeprecatedString const& value) override;

    virtual Gfx::Path& get_path() override;

    JS::NonnullGCPtr<SVGAnimatedLength> x() const;
    JS::NonnullGCPtr<SVGAnimatedLength> y() const;
    JS::NonnullGCPtr<SVGAnimatedLength> width() const;
    JS::NonnullGCPtr<SVGAnimatedLength> height() const;
    JS::NonnullGCPtr<SVGAnimatedLength> rx() const;
    JS::NonnullGCPtr<SVGAnimatedLength> ry() const;

private:
    SVGRectElement(DOM::Document&, DOM::QualifiedName);

    virtual void initialize(JS::Realm&) override;

    Gfx::FloatSize calculate_used_corner_radius_values() const;

    Optional<Gfx::Path> m_path;

    Optional<float> m_x;
    Optional<float> m_y;
    Optional<float> m_width;
    Optional<float> m_height;
    Optional<float> m_radius_x;
    Optional<float> m_radius_y;
};

}
