/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/SVG/SVGGeometryElement.h>

namespace Web::SVG {

class SVGEllipseElement final : public SVGGeometryElement {
public:
    using WrapperType = Bindings::SVGEllipseElementWrapper;

    SVGEllipseElement(DOM::Document&, DOM::QualifiedName);
    virtual ~SVGEllipseElement() override = default;

    virtual void parse_attribute(FlyString const& name, String const& value) override;

    virtual Gfx::Path& get_path() override;

private:
    Optional<Gfx::Path> m_path;

    Optional<float> m_center_x;
    Optional<float> m_center_y;
    Optional<float> m_radius_x;
    Optional<float> m_radius_y;
};

}
