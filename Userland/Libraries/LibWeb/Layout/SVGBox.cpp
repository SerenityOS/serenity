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
    : BlockContainer(document, &element, move(style))
{
}

void SVGBox::before_children_paint(PaintContext& context, Painting::PaintPhase phase)
{
    Node::before_children_paint(context, phase);
    if (phase != Painting::PaintPhase::Foreground)
        return;
    context.svg_context().save();
}

void SVGBox::after_children_paint(PaintContext& context, Painting::PaintPhase phase)
{
    Node::after_children_paint(context, phase);
    if (phase != Painting::PaintPhase::Foreground)
        return;
    context.svg_context().restore();
}

}
