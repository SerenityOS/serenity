/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
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

    SVGGraphicsElement(DOM::Document&, DOM::QualifiedName);

    virtual void apply_presentational_hints(CSS::StyleProperties&) const override;

    Optional<Gfx::Color> fill_color() const;
    Optional<Gfx::Color> stroke_color() const;
    Optional<float> stroke_width() const;
};

}
