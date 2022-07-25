/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/StylePainter.h>
#include <LibWeb/Painting/ProgressPaintable.h>

namespace Web::Painting {

NonnullRefPtr<ProgressPaintable> ProgressPaintable::create(Layout::Progress const& layout_box)
{
    return adopt_ref(*new ProgressPaintable(layout_box));
}

ProgressPaintable::ProgressPaintable(Layout::Progress const& layout_box)
    : PaintableBox(layout_box)
{
}

Layout::Progress const& ProgressPaintable::layout_box() const
{
    return static_cast<Layout::Progress const&>(layout_node());
}

void ProgressPaintable::paint(PaintContext& context, PaintPhase phase) const
{
    if (!is_visible())
        return;

    if (phase == PaintPhase::Foreground) {
        auto progress_rect = absolute_rect().to_rounded<int>();
        auto frame_thickness = min(min(progress_rect.width(), progress_rect.height()) / 6, 3);
        Gfx::StylePainter::paint_progressbar(context.painter(), progress_rect.shrunken(frame_thickness, frame_thickness), context.palette(), 0, round_to<int>(layout_box().dom_node().max()), round_to<int>(layout_box().dom_node().value()), ""sv);
        Gfx::StylePainter::paint_frame(context.painter(), progress_rect, context.palette(), Gfx::FrameShape::Box, Gfx::FrameShadow::Raised, frame_thickness);
    }
}

}
