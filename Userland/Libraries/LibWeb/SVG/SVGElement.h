/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/Element.h>

namespace Web::SVG {

class SVGElement : public DOM::Element {
public:
    using WrapperType = Bindings::SVGElementWrapper;

protected:
    SVGElement(DOM::Document&, QualifiedName);
};

}
