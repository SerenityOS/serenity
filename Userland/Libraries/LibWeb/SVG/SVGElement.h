/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/Element.h>
#include <LibWeb/HTML/DOMStringMap.h>

namespace Web::SVG {

class SVGElement : public DOM::Element {
    WEB_PLATFORM_OBJECT(SVGElement, DOM::Element);

public:
    virtual bool requires_svg_container() const override { return true; }

    HTML::DOMStringMap* dataset() { return m_dataset.ptr(); }
    HTML::DOMStringMap const* dataset() const { return m_dataset.ptr(); }

protected:
    SVGElement(DOM::Document&, DOM::QualifiedName);

    virtual void visit_edges(Cell::Visitor&) override;

    JS::NonnullGCPtr<HTML::DOMStringMap> m_dataset;
};

}
