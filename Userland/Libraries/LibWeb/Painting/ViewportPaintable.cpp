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
    set_stacking_context(make<StackingContext>(layout_box(), nullptr, 0));

    size_t index_in_tree_order = 1;
    for_each_in_subtree_of_type<PaintableBox>([&](PaintableBox const& paintable) {
        auto& paintable_box = const_cast<PaintableBox&>(paintable);
        paintable_box.invalidate_stacking_context();
        if (!paintable_box.layout_box().establishes_stacking_context()) {
            VERIFY(!paintable_box.stacking_context());
            return TraversalDecision::Continue;
        }
        auto* parent_context = paintable_box.enclosing_stacking_context();
        VERIFY(parent_context);
        paintable_box.set_stacking_context(make<Painting::StackingContext>(paintable_box.layout_box(), parent_context, index_in_tree_order++));
        return TraversalDecision::Continue;
    });

    stacking_context()->sort();
}

void ViewportPaintable::paint_all_phases(PaintContext& context)
{
    build_stacking_context_tree_if_needed();
    context.painter().translate(-context.device_viewport_rect().location().to_type<int>());
    stacking_context()->paint(context);
}

}
