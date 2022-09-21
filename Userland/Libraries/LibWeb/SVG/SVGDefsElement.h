/*
 * Copyright (c) 2022, Simon Danner <danner.simon@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/SVG/SVGGraphicsElement.h>

namespace Web::SVG {

class SVGDefsElement final : public SVGGraphicsElement {
    WEB_PLATFORM_OBJECT(SVGDefsElement, SVGGraphicsElement);

public:
    virtual ~SVGDefsElement();

    virtual RefPtr<Layout::Node> create_layout_node(NonnullRefPtr<CSS::StyleProperties>) override;

private:
    SVGDefsElement(DOM::Document&, DOM::QualifiedName);
};

}
