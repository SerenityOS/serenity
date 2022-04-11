/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/SVG/SVGElement.h>

namespace Web::SVG {

class SVGClipPathElement final : public SVGElement {
public:
    using WrapperType = Bindings::SVGClipPathElementWrapper;

    SVGClipPathElement(DOM::Document&, DOM::QualifiedName);
    virtual ~SVGClipPathElement();

    virtual RefPtr<Layout::Node> create_layout_node(NonnullRefPtr<CSS::StyleProperties>) override;
};

}
