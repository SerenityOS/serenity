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

    NonnullRefPtr<HTML::DOMStringMap> dataset() const { return m_dataset; }

protected:
    SVGElement(DOM::Document&, DOM::QualifiedName);

    NonnullRefPtr<HTML::DOMStringMap> m_dataset;
};

}
