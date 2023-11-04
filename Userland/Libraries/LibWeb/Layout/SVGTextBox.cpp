/*
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Layout/SVGTextBox.h>
#include <LibWeb/Painting/SVGPathPaintable.h>
#include <LibWeb/SVG/SVGSVGElement.h>

namespace Web::Layout {

SVGTextBox::SVGTextBox(DOM::Document& document, SVG::SVGTextPositioningElement& element, NonnullRefPtr<CSS::StyleProperties> properties)
    : SVGGraphicsBox(document, element, properties)
{
}

JS::GCPtr<Painting::Paintable> SVGTextBox::create_paintable() const
{
    return Painting::SVGPathPaintable::create(*this);
}

}
