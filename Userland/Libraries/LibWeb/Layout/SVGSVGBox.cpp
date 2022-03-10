/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Layout/SVGSVGBox.h>
#include <LibWeb/Painting/Paintable.h>

namespace Web::Layout {

SVGSVGBox::SVGSVGBox(DOM::Document& document, SVG::SVGSVGElement& element, NonnullRefPtr<CSS::StyleProperties> properties)
    : SVGGraphicsBox(document, element, properties)
{
}

}
