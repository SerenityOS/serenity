/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/SVG/SVGGraphicsElement.h>

namespace Web::SVG {

// https://svgwg.org/svg2-draft/text.html#InterfaceSVGTextContentElement
class SVGTextContentElement : public SVGGraphicsElement {
public:
    using WrapperType = Bindings::SVGTextContentElementWrapper;

    SVGTextContentElement(DOM::Document&, DOM::QualifiedName);

    int get_number_of_chars() const;
};

}
