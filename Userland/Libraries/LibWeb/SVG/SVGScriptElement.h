/*
 * Copyright (c) 2023, Shannon Booth <shannon@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/SVG/SVGElement.h>

namespace Web::SVG {

// https://www.w3.org/TR/SVG/interact.html#InterfaceSVGScriptElement
class SVGScriptElement : public SVGElement {
    WEB_PLATFORM_OBJECT(SVGScriptElement, SVGElement);

public:
protected:
    SVGScriptElement(DOM::Document&, DOM::QualifiedName);

    virtual void initialize(JS::Realm&) override;

private:
    virtual bool is_svg_script_element() const final { return true; }
};

}

namespace Web::DOM {

template<>
inline bool Node::fast_is<SVG::SVGScriptElement>() const { return is_svg_script_element(); }

}
