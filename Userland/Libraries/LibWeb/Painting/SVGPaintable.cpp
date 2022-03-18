/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Layout/ImageBox.h>
#include <LibWeb/Painting/SVGPaintable.h>

namespace Web::Painting {

SVGPaintable::SVGPaintable(Layout::SVGBox const& layout_box)
    : PaintableBox(layout_box)
{
}

Layout::SVGBox const& SVGPaintable::layout_box() const
{
    return static_cast<Layout::SVGBox const&>(layout_node());
}

void SVGPaintable::before_children_paint(PaintContext& context, PaintPhase phase) const
{
    PaintableBox::before_children_paint(context, phase);
    if (phase != PaintPhase::Foreground)
        return;
    context.svg_context().save();
}

void SVGPaintable::after_children_paint(PaintContext& context, PaintPhase phase) const
{
    PaintableBox::after_children_paint(context, phase);
    if (phase != PaintPhase::Foreground)
        return;
    context.svg_context().restore();
}

}
