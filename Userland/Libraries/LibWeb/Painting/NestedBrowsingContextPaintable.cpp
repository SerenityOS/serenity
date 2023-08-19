/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <LibWeb/HTML/NavigableContainer.h>
#include <LibWeb/Layout/FrameBox.h>
#include <LibWeb/Layout/Viewport.h>
#include <LibWeb/Painting/BorderRadiusCornerClipper.h>
#include <LibWeb/Painting/NestedBrowsingContextPaintable.h>
#include <LibWeb/Painting/ViewportPaintable.h>

namespace Web::Painting {

JS::NonnullGCPtr<NestedBrowsingContextPaintable> NestedBrowsingContextPaintable::create(Layout::FrameBox const& layout_box)
{
    return layout_box.heap().allocate_without_realm<NestedBrowsingContextPaintable>(layout_box);
}

NestedBrowsingContextPaintable::NestedBrowsingContextPaintable(Layout::FrameBox const& layout_box)
    : PaintableBox(layout_box)
{
}

Layout::FrameBox const& NestedBrowsingContextPaintable::layout_box() const
{
    return static_cast<Layout::FrameBox const&>(layout_node());
}

void NestedBrowsingContextPaintable::paint(PaintContext& context, PaintPhase phase) const
{
    if (!layout_box().is_visible())
        return;

    PaintableBox::paint(context, phase);

    if (phase == PaintPhase::Foreground) {
        auto absolute_rect = this->absolute_rect();
        auto clip_rect = context.rounded_device_rect(absolute_rect);
        ScopedCornerRadiusClip corner_clip { context, context.painter(), clip_rect, normalized_border_radii_data(ShrinkRadiiForBorders::Yes) };

        auto* hosted_document = layout_box().dom_node().content_document_without_origin_check();
        if (!hosted_document)
            return;
        auto* hosted_paint_tree = hosted_document->paintable();
        if (!hosted_paint_tree)
            return;

        context.painter().save();
        auto old_viewport_rect = context.device_viewport_rect();

        context.painter().add_clip_rect(clip_rect.to_type<int>());

        auto absolute_device_rect = context.enclosing_device_rect(absolute_rect);
        context.painter().translate(absolute_device_rect.x().value(), absolute_device_rect.y().value());

        context.set_device_viewport_rect({ {}, context.enclosing_device_size(layout_box().dom_node().nested_browsing_context()->size()) });
        const_cast<ViewportPaintable*>(hosted_paint_tree)->paint_all_phases(context);

        context.set_device_viewport_rect(old_viewport_rect);
        context.painter().restore();

        if constexpr (HIGHLIGHT_FOCUSED_FRAME_DEBUG) {
            if (layout_box().dom_node().nested_browsing_context()->is_focused_context()) {
                context.painter().draw_rect(clip_rect.to_type<int>(), Color::Cyan);
            }
        }
    }
}

}
