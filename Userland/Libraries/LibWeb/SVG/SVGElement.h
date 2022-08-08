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
public:
    using WrapperType = Bindings::SVGElementWrapper;

    virtual bool requires_svg_container() const override { return true; }

    HTML::DOMStringMap* dataset() { return m_dataset.cell(); }
    HTML::DOMStringMap const* dataset() const { return m_dataset.cell(); }

protected:
    SVGElement(DOM::Document&, DOM::QualifiedName);

    JS::Handle<HTML::DOMStringMap> m_dataset;
};

}
