/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <LibWeb/HTML/BrowsingContextContainer.h>
#include <LibWeb/Layout/FrameBox.h>
#include <LibWeb/Layout/InitialContainingBlock.h>
#include <LibWeb/Painting/NestedBrowsingContextPaintable.h>

namespace Web::Painting {

NonnullRefPtr<NestedBrowsingContextPaintable> NestedBrowsingContextPaintable::create(Layout::FrameBox const& layout_box)
{
    return adopt_ref(*new NestedBrowsingContextPaintable(layout_box));
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
    PaintableBox::paint(context, phase);

    if (phase == PaintPhase::Foreground) {
        auto* hosted_document = layout_box().dom_node().content_document_without_origin_check();
        if (!hosted_document)
            return;
        auto* hosted_layout_tree = hosted_document->layout_node();
        if (!hosted_layout_tree)
            return;

        context.painter().save();
        auto old_viewport_rect = context.viewport_rect();

        context.painter().add_clip_rect(enclosing_int_rect(absolute_rect()));
        context.painter().translate(absolute_x(), absolute_y());

        context.set_viewport_rect({ {}, layout_box().dom_node().nested_browsing_context()->size() });
        const_cast<Layout::InitialContainingBlock*>(hosted_layout_tree)->paint_all_phases(context);

        context.set_viewport_rect(old_viewport_rect);
        context.painter().restore();

        if constexpr (HIGHLIGHT_FOCUSED_FRAME_DEBUG) {
            if (layout_box().dom_node().nested_browsing_context()->is_focused_context()) {
                context.painter().draw_rect(absolute_rect().to_type<int>(), Color::Cyan);
            }
        }
    }
}

}
