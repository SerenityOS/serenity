/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/SVG/SVGElement.h>

namespace Web::SVG {

class SVGClipPathElement final : public SVGElement {
    WEB_PLATFORM_OBJECT(SVGClipPathElement, SVGElement);

public:
    virtual ~SVGClipPathElement();

    virtual JS::GCPtr<Layout::Node> create_layout_node(NonnullRefPtr<CSS::StyleProperties>) override;

private:
    SVGClipPathElement(DOM::Document&, DOM::QualifiedName);

    virtual void initialize(JS::Realm&) override;
};

}
