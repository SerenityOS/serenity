/*
 * Copyright (c) 2024, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Layout/SVGClipBox.h>
#include <LibWeb/Painting/SVGClipPaintable.h>
#include <LibWeb/Painting/StackingContext.h>

namespace Web::Layout {

SVGClipBox::SVGClipBox(DOM::Document& document, SVG::SVGClipPathElement& element, NonnullRefPtr<CSS::StyleProperties> properties)
    : SVGBox(document, element, properties)
{
}

JS::GCPtr<Painting::Paintable> SVGClipBox::create_paintable() const
{
    return Painting::SVGClipPaintable::create(*this);
}

}
