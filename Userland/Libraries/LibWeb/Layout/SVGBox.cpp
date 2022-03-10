/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Painter.h>
#include <LibWeb/Layout/SVGBox.h>
#include <LibWeb/Painting/StackingContext.h>

namespace Web::Layout {

SVGBox::SVGBox(DOM::Document& document, SVG::SVGElement& element, NonnullRefPtr<CSS::StyleProperties> style)
    : Box(document, &element, move(style))
{
}

}
