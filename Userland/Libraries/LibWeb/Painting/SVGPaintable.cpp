/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Layout/ImageBox.h>
#include <LibWeb/Layout/SVGSVGBox.h>
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

void SVGPaintable::before_children_paint(PaintContext& context, PaintPhase phase, ShouldClipOverflow should_clip_overflow) const
{
    PaintableBox::before_children_paint(context, phase, should_clip_overflow);
    if (phase != PaintPhase::Foreground)
        return;
    context.svg_context().save();
}

void SVGPaintable::after_children_paint(PaintContext& context, PaintPhase phase, ShouldClipOverflow should_clip_overflow) const
{
    PaintableBox::after_children_paint(context, phase, should_clip_overflow);
    if (phase != PaintPhase::Foreground)
        return;
    context.svg_context().restore();
}

Gfx::FloatRect SVGPaintable::compute_absolute_rect() const
{
    if (auto* svg_svg_box = layout_box().first_ancestor_of_type<Layout::SVGSVGBox>()) {
        Gfx::FloatRect rect { effective_offset(), content_size() };
        for (Layout::Box const* ancestor = svg_svg_box; ancestor && ancestor->paintable(); ancestor = ancestor->paintable()->containing_block())
            rect.translate_by(ancestor->paint_box()->effective_offset());
        return rect;
    }
    return PaintableBox::compute_absolute_rect();
}

}
