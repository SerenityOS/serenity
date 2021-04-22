/*
 * Copyright (c) 2020, Matthew Olsson <matthewcolsson@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/SVG/SVGGeometryElement.h>

namespace Web::SVG {

SVGGeometryElement::SVGGeometryElement(DOM::Document& document, QualifiedName qualified_name)
    : SVGGraphicsElement(document, move(qualified_name))
{
}

}
