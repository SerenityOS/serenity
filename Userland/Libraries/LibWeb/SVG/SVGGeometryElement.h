/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/SVG/SVGGraphicsElement.h>

namespace Web::SVG {

// https://svgwg.org/svg2-draft/types.html#InterfaceSVGGeometryElement
class SVGGeometryElement : public SVGGraphicsElement {
public:
    using WrapperType = Bindings::SVGGeometryElementWrapper;

    virtual RefPtr<Layout::Node> create_layout_node(NonnullRefPtr<CSS::StyleProperties>) override;

    virtual Gfx::Path& get_path() = 0;

protected:
    SVGGeometryElement(DOM::Document& document, DOM::QualifiedName qualified_name);
};

}
