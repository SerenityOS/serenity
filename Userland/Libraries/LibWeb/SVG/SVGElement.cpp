/*
 * Copyright (c) 2020, Matthew Olsson <matthewcolsson@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/SVG/SVGElement.h>

namespace Web::SVG {

SVGElement::SVGElement(DOM::Document& document, QualifiedName qualified_name)
    : Element(document, move(qualified_name))
{
}

}
