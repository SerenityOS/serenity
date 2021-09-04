/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Path.h>
#include <LibWeb/DOM/Node.h>
#include <LibWeb/SVG/SVGElement.h>
#include <LibWeb/SVG/TagNames.h>

namespace Web::SVG {

class SVGGraphicsElement : public SVGElement {
public:
    using WrapperType = Bindings::SVGGraphicsElementWrapper;

    SVGGraphicsElement(DOM::Document&, QualifiedName);

    virtual void parse_attribute(FlyString const& name, String const& value) override;

    const Optional<Gfx::Color>& fill_color() const { return m_fill_color; }
    const Optional<Gfx::Color>& stroke_color() const { return m_stroke_color; }
    const Optional<float>& stroke_width() const { return m_stroke_width; }

protected:
    Optional<Gfx::Color> m_fill_color;
    Optional<Gfx::Color> m_stroke_color;
    Optional<float> m_stroke_width;
};

}
