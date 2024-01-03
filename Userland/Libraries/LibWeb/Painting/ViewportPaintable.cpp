/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Layout/Viewport.h>
#include <LibWeb/Painting/StackingContext.h>
#include <LibWeb/Painting/ViewportPaintable.h>

namespace Web::Painting {

JS::NonnullGCPtr<ViewportPaintable> ViewportPaintable::create(Layout::Viewport const& layout_viewport)
{
    return layout_viewport.heap().allocate_without_realm<ViewportPaintable>(layout_viewport);
}

ViewportPaintable::ViewportPaintable(Layout::Viewport const& layout_viewport)
    : PaintableWithLines(layout_viewport)
{
}

ViewportPaintable::~ViewportPaintable() = default;

void ViewportPaintable::build_stacking_context_tree_if_needed()
{
    if (stacking_context())
        return;
    build_stacking_context_tree();
}

void ViewportPaintable::build_stacking_context_tree()
{
    set_stacking_context(make<StackingContext>(*this, nullptr, 0));

    size_t index_in_tree_order = 1;
    for_each_in_subtree([&](Paintable const& paintable) {
        const_cast<Paintable&>(paintable).invalidate_stacking_context();
        if (!paintable.layout_node().establishes_stacking_context()) {
            VERIFY(!paintable.stacking_context());
            return TraversalDecision::Continue;
        }
        auto* parent_context = const_cast<Paintable&>(paintable).enclosing_stacking_context();
        VERIFY(parent_context);
        const_cast<Paintable&>(paintable).set_stacking_context(make<Painting::StackingContext>(const_cast<Paintable&>(paintable), parent_context, index_in_tree_order++));
        return TraversalDecision::Continue;
    });

    stacking_context()->sort();
}

void ViewportPaintable::paint_all_phases(PaintContext& context)
{
    build_stacking_context_tree_if_needed();
    context.recording_painter().translate(-context.device_viewport_rect().location().to_type<int>());
    stacking_context()->paint(context);
}

void ViewportPaintable::collect_scroll_frames(PaintContext& context) const
{
    i32 next_id = 0;
    for_each_in_subtree_of_type<PaintableBox>([&](auto const& paintable_box) {
        if (paintable_box.has_scrollable_overflow()) {
            auto offset = paintable_box.scroll_offset();
            auto ancestor = paintable_box.parent();
            while (ancestor) {
                if (ancestor->is_paintable_box() && static_cast<PaintableBox const*>(ancestor)->has_scrollable_overflow())
                    offset.translate_by(static_cast<PaintableBox const*>(ancestor)->scroll_offset());
                ancestor = ancestor->parent();
            }
            context.scroll_frames().set(&paintable_box, { .id = next_id++, .offset = -offset });
        }
        return TraversalDecision::Continue;
    });
}

}
