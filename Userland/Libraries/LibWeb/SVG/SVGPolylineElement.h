/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/SVG/SVGGeometryElement.h>

namespace Web::SVG {

class SVGPolylineElement final : public SVGGeometryElement {
public:
    using WrapperType = Bindings::SVGPolylineElementWrapper;

    SVGPolylineElement(DOM::Document&, DOM::QualifiedName);
    virtual ~SVGPolylineElement() override = default;

    virtual void parse_attribute(FlyString const& name, String const& value) override;

    virtual Gfx::Path& get_path() override;

private:
    Optional<Gfx::Path> m_path;

    Vector<Gfx::FloatPoint> m_points;
};

}
