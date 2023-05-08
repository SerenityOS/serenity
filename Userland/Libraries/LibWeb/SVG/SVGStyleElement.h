/*
 * Copyright (c) 2023, Emil Militzer <emil.militzer@posteo.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/SVG/SVGElement.h>

namespace Web::SVG {

class SVGStyleElement final : public SVGElement {
    WEB_PLATFORM_OBJECT(SVGStyleElement, SVGElement);

public:
    virtual ~SVGStyleElement() override;

    virtual void children_changed() override;
    virtual void inserted() override;
    virtual void removed_from(Node*) override;

    void update_a_style_block();

    CSS::CSSStyleSheet* sheet();
    CSS::CSSStyleSheet const* sheet() const;

private:
    SVGStyleElement(DOM::Document&, DOM::QualifiedName);

    virtual JS::ThrowCompletionOr<void> initialize(JS::Realm&) override;
    virtual void visit_edges(Cell::Visitor&) override;

    // https://www.w3.org/TR/cssom/#associated-css-style-sheet
    JS::GCPtr<CSS::CSSStyleSheet> m_associated_css_style_sheet;
};

}
