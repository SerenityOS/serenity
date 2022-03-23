/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Painting/CanvasPaintable.h>

namespace Web::Painting {

NonnullRefPtr<CanvasPaintable> CanvasPaintable::create(Layout::CanvasBox const& layout_box)
{
    return adopt_ref(*new CanvasPaintable(layout_box));
}

CanvasPaintable::CanvasPaintable(Layout::CanvasBox const& layout_box)
    : PaintableBox(layout_box)
{
}

Layout::CanvasBox const& CanvasPaintable::layout_box() const
{
    return static_cast<Layout::CanvasBox const&>(layout_node());
}

void CanvasPaintable::paint(PaintContext& context, PaintPhase phase) const
{
    if (!layout_box().is_visible())
        return;

    PaintableBox::paint(context, phase);

    if (phase == PaintPhase::Foreground) {
        // FIXME: This should be done at a different level. Also rect() does not include padding etc!
        if (!context.viewport_rect().intersects(enclosing_int_rect(absolute_rect())))
            return;

        if (layout_box().dom_node().bitmap())
            context.painter().draw_scaled_bitmap(absolute_rect().to_rounded<int>(), *layout_box().dom_node().bitmap(), layout_box().dom_node().bitmap()->rect(), 1.0f, to_gfx_scaling_mode(computed_values().image_rendering()));
    }
}

}
