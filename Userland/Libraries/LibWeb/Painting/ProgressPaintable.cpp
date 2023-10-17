/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/StylePainter.h>
#include <LibWeb/Painting/ProgressPaintable.h>

namespace Web::Painting {

JS::NonnullGCPtr<ProgressPaintable> ProgressPaintable::create(Layout::Progress const& layout_box)
{
    return layout_box.heap().allocate_without_realm<ProgressPaintable>(layout_box);
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
        auto progress_rect = context.rounded_device_rect(absolute_rect());
        auto min_frame_thickness = context.rounded_device_pixels(3);
        auto frame_thickness = min(min(progress_rect.width(), progress_rect.height()) / 6, min_frame_thickness);

        context.painter().paint_progressbar(
            progress_rect.to_type<int>(),
            progress_rect.shrunken(frame_thickness, frame_thickness).to_type<int>(),
            context.palette(),
            0,
            round_to<int>(layout_box().dom_node().max()),
            round_to<int>(layout_box().dom_node().value()),
            ""sv);
    }
}

}
