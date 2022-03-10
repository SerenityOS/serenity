/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Layout/ImageBox.h>
#include <LibWeb/Painting/SVGPaintable.h>

namespace Web::Painting {

SVGPaintable::SVGPaintable(Layout::SVGBox const& layout_box)
    : Paintable(layout_box)
{
}

Layout::SVGBox const& SVGPaintable::layout_box() const
{
    return static_cast<Layout::SVGBox const&>(m_layout_box);
}

void SVGPaintable::before_children_paint(PaintContext& context, PaintPhase phase) const
{
    Paintable::before_children_paint(context, phase);
    if (phase != PaintPhase::Foreground)
        return;
    context.svg_context().save();
}

void SVGPaintable::after_children_paint(PaintContext& context, PaintPhase phase) const
{
    Paintable::after_children_paint(context, phase);
    if (phase != PaintPhase::Foreground)
        return;
    context.svg_context().restore();
}

}
