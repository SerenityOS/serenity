/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Layout/SVGGraphicsBox.h>
#include <LibWeb/Painting/StackingContext.h>

namespace Web::Layout {

SVGGraphicsBox::SVGGraphicsBox(DOM::Document& document, SVG::SVGGraphicsElement& element, NonnullRefPtr<CSS::StyleProperties> properties)
    : SVGBox(document, element, properties)
{
}

}
