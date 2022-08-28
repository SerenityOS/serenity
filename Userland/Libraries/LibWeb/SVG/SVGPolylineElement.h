/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/SVG/SVGGeometryElement.h>

namespace Web::SVG {

class SVGPolylineElement final : public SVGGeometryElement {
    WEB_PLATFORM_OBJECT(SVGPolylineElement, SVGGeometryElement);

public:
    virtual ~SVGPolylineElement() override = default;

    virtual void parse_attribute(FlyString const& name, String const& value) override;

    virtual Gfx::Path& get_path() override;

private:
    SVGPolylineElement(DOM::Document&, DOM::QualifiedName);

    Optional<Gfx::Path> m_path;

    Vector<Gfx::FloatPoint> m_points;
};

}

WRAPPER_HACK(SVGPolylineElement, Web::SVG)
