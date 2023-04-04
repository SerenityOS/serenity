/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/HTMLVideoElement.h>
#include <LibWeb/Layout/VideoBox.h>
#include <LibWeb/Painting/BorderRadiusCornerClipper.h>
#include <LibWeb/Painting/VideoPaintable.h>

namespace Web::Painting {

JS::NonnullGCPtr<VideoPaintable> VideoPaintable::create(Layout::VideoBox const& layout_box)
{
    return layout_box.heap().allocate_without_realm<VideoPaintable>(layout_box);
}

VideoPaintable::VideoPaintable(Layout::VideoBox const& layout_box)
    : PaintableBox(layout_box)
{
}

Layout::VideoBox const& VideoPaintable::layout_box() const
{
    return static_cast<Layout::VideoBox const&>(layout_node());
}

void VideoPaintable::paint(PaintContext& context, PaintPhase phase) const
{
    if (!is_visible())
        return;

    // FIXME: This should be done at a different level.
    if (is_out_of_view(context))
        return;

    PaintableBox::paint(context, phase);

    if (phase != PaintPhase::Foreground)
        return;

    if (auto const& bitmap = layout_box().dom_node().current_frame()) {
        auto image_rect = context.rounded_device_rect(absolute_rect());
        ScopedCornerRadiusClip corner_clip { context, context.painter(), image_rect, normalized_border_radii_data(ShrinkRadiiForBorders::Yes) };
        context.painter().draw_scaled_bitmap(image_rect.to_type<int>(), *bitmap, bitmap->rect(), 1.0f, to_gfx_scaling_mode(computed_values().image_rendering()));
    }
}

}
