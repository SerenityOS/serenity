/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/StylePainter.h>
#include <LibWeb/Painting/ProgressPaintable.h>

namespace Web::Painting {

NonnullOwnPtr<ProgressPaintable> ProgressPaintable::create(Layout::Progress const& layout_box)
{
    return adopt_own(*new ProgressPaintable(layout_box));
}

ProgressPaintable::ProgressPaintable(Layout::Progress const& layout_box)
    : Paintable(layout_box)
{
}

Layout::Progress const& ProgressPaintable::layout_box() const
{
    return static_cast<Layout::Progress const&>(m_layout_box);
}

void ProgressPaintable::paint(PaintContext& context, PaintPhase phase) const
{
    if (!is_visible())
        return;

    if (phase == PaintPhase::Foreground) {
        // FIXME: This does not support floating point value() and max()
        Gfx::StylePainter::paint_progressbar(context.painter(), enclosing_int_rect(absolute_rect()), context.palette(), 0, layout_box().dom_node().max(), layout_box().dom_node().value(), "");
    }
}

}
