/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/SVG/SVGAnimatedLength.h>
#include <LibWeb/SVG/SVGGeometryElement.h>

namespace Web::SVG {

class SVGLineElement final : public SVGGeometryElement {
    WEB_PLATFORM_OBJECT(SVGLineElement, SVGGraphicsElement);

public:
    virtual ~SVGLineElement() override = default;

    virtual void parse_attribute(FlyString const& name, String const& value) override;

    virtual Gfx::Path& get_path() override;

    JS::NonnullGCPtr<SVGAnimatedLength> x1() const;
    JS::NonnullGCPtr<SVGAnimatedLength> y1() const;
    JS::NonnullGCPtr<SVGAnimatedLength> x2() const;
    JS::NonnullGCPtr<SVGAnimatedLength> y2() const;

private:
    SVGLineElement(DOM::Document&, DOM::QualifiedName);

    Optional<Gfx::Path> m_path;

    Optional<float> m_x1;
    Optional<float> m_y1;
    Optional<float> m_x2;
    Optional<float> m_y2;
};

}
