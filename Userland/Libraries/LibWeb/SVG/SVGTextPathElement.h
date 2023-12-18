/*
 * Copyright (c) 2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/SVG/SVGGeometryElement.h>
#include <LibWeb/SVG/SVGTextContentElement.h>

namespace Web::SVG {

// https://svgwg.org/svg2-draft/text.html#TextPathElement
class SVGTextPathElement : public SVGTextContentElement {
    WEB_PLATFORM_OBJECT(SVGTextPathElement, SVGTextContentElement);
    JS_DECLARE_ALLOCATOR(SVGTextPathElement);

public:
    virtual JS::GCPtr<Layout::Node> create_layout_node(NonnullRefPtr<CSS::StyleProperties>) override;

    JS::GCPtr<SVGGeometryElement const> path_or_shape() const;

protected:
    SVGTextPathElement(DOM::Document&, DOM::QualifiedName);

    virtual void initialize(JS::Realm&) override;
};

}
