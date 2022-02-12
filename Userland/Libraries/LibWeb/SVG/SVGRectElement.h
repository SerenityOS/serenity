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
public:
    using WrapperType = Bindings::SVGRectElementWrapper;

    SVGRectElement(DOM::Document&, QualifiedName);
    virtual ~SVGRectElement() override = default;

    virtual void parse_attribute(FlyString const& name, String const& value) override;

    virtual Gfx::Path& get_path() override;

private:
    Gfx::FloatPoint calculate_used_corner_radius_values();

    Optional<Gfx::Path> m_path;

    Optional<float> m_x;
    Optional<float> m_y;
    Optional<float> m_width;
    Optional<float> m_height;
    Optional<float> m_radius_x;
    Optional<float> m_radius_y;
};

}
