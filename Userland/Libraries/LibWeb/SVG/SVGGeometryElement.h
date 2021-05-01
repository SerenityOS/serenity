/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/SVG/SVGGraphicsElement.h>

namespace Web::SVG {

class SVGGeometryElement : public SVGGraphicsElement {
public:
    using WrapperType = Bindings::SVGGeometryElementWrapper;

protected:
    SVGGeometryElement(DOM::Document& document, QualifiedName qualified_name);
};

}
