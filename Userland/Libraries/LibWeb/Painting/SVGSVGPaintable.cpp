/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Layout/ImageBox.h>
#include <LibWeb/Painting/SVGSVGPaintable.h>

namespace Web::Painting {

JS::NonnullGCPtr<SVGSVGPaintable> SVGSVGPaintable::create(Layout::SVGSVGBox const& layout_box)
{
    return layout_box.heap().allocate_without_realm<SVGSVGPaintable>(layout_box);
}

SVGSVGPaintable::SVGSVGPaintable(Layout::SVGSVGBox const& layout_box)
    : PaintableBox(layout_box)
{
}

Layout::SVGSVGBox const& SVGSVGPaintable::layout_box() const
{
    return static_cast<Layout::SVGSVGBox const&>(layout_node());
}

void SVGSVGPaintable::before_children_paint(PaintContext& context, PaintPhase phase) const
{
    if (phase != PaintPhase::Foreground)
        return;

    if (!context.has_svg_context())
        context.set_svg_context(SVGContext(absolute_rect()));

    PaintableBox::before_children_paint(context, phase);
}

void SVGSVGPaintable::after_children_paint(PaintContext& context, PaintPhase phase) const
{
    PaintableBox::after_children_paint(context, phase);
    if (phase != PaintPhase::Foreground)
        return;
    context.clear_svg_context();
}

}
