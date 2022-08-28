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
    WEB_PLATFORM_OBJECT(SVGTextContentElement, SVGGraphicsElement);

public:
    int get_number_of_chars() const;

protected:
    SVGTextContentElement(DOM::Document&, DOM::QualifiedName);
};

}

WRAPPER_HACK(SVGTextContentElement, Web::SVG)
