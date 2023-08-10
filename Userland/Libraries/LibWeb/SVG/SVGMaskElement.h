/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/SVG/SVGGraphicsElement.h>

namespace Web::SVG {

class SVGMaskElement final : public SVGGraphicsElement {
    WEB_PLATFORM_OBJECT(SVGMaskElement, SVGGraphicsElement);

public:
    virtual ~SVGMaskElement() override;

    virtual JS::GCPtr<Layout::Node> create_layout_node(NonnullRefPtr<CSS::StyleProperties>) override;

private:
    SVGMaskElement(DOM::Document&, DOM::QualifiedName);
    virtual void initialize(JS::Realm&) override;
};

}
