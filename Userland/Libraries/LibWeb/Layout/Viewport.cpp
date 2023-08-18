/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Range.h>
#include <LibWeb/Dump.h>
#include <LibWeb/Layout/Viewport.h>
#include <LibWeb/Painting/PaintableBox.h>
#include <LibWeb/Painting/StackingContext.h>
#include <LibWeb/Painting/ViewportPaintable.h>

namespace Web::Layout {

Viewport::Viewport(DOM::Document& document, NonnullRefPtr<CSS::StyleProperties> style)
    : BlockContainer(document, &document, move(style))
{
}

Viewport::~Viewport() = default;

JS::GCPtr<Selection::Selection> Viewport::selection() const
{
    return const_cast<DOM::Document&>(document()).get_selection();
}

void Viewport::build_stacking_context_tree_if_needed()
{
    if (paintable_box()->stacking_context())
        return;
    build_stacking_context_tree();
}

void Viewport::build_stacking_context_tree()
{
    paintable_box()->set_stacking_context(make<Painting::StackingContext>(*this, nullptr, 0));

    size_t index_in_tree_order = 1;
    for_each_in_subtree_of_type<Box>([&](Box& box) {
        if (!box.paintable_box())
            return IterationDecision::Continue;
        box.paintable_box()->invalidate_stacking_context();
        if (!box.establishes_stacking_context()) {
            VERIFY(!box.paintable_box()->stacking_context());
            return IterationDecision::Continue;
        }
        auto* parent_context = box.paintable_box()->enclosing_stacking_context();
        VERIFY(parent_context);
        box.paintable_box()->set_stacking_context(make<Painting::StackingContext>(box, parent_context, index_in_tree_order++));
        return IterationDecision::Continue;
    });

    paintable_box()->stacking_context()->sort();
}

void Viewport::paint_all_phases(PaintContext& context)
{
    build_stacking_context_tree_if_needed();
    context.painter().translate(-context.device_viewport_rect().location().to_type<int>());
    paintable_box()->stacking_context()->paint(context);
}

void Viewport::recompute_selection_states()
{
    // 1. Start by resetting the selection state of all layout nodes to None.
    for_each_in_inclusive_subtree([&](auto& layout_node) {
        layout_node.set_selection_state(SelectionState::None);
        return IterationDecision::Continue;
    });

    // 2. If there is no active Selection or selected Range, return.
    auto selection = document().get_selection();
    if (!selection)
        return;
    auto range = selection->range();
    if (!range)
        return;

    auto* start_container = range->start_container();
    auto* end_container = range->end_container();

    // 3. If the selection starts and ends in the same node:
    if (start_container == end_container) {
        // 1. If the selection starts and ends at the same offset, return.
        if (range->start_offset() == range->end_offset()) {
            // NOTE: A zero-length selection should not be visible.
            return;
        }

        // 2. If it's a text node, mark it as StartAndEnd and return.
        if (is<DOM::Text>(*start_container)) {
            if (auto* layout_node = start_container->layout_node()) {
                layout_node->set_selection_state(SelectionState::StartAndEnd);
            }
            return;
        }
    }

    if (start_container == end_container && is<DOM::Text>(*start_container)) {
        if (auto* layout_node = start_container->layout_node()) {
            layout_node->set_selection_state(SelectionState::StartAndEnd);
        }
        return;
    }

    // 4. Mark the selection start node as Start (if text) or Full (if anything else).
    if (auto* layout_node = start_container->layout_node()) {
        if (is<DOM::Text>(*start_container))
            layout_node->set_selection_state(SelectionState::Start);
        else
            layout_node->set_selection_state(SelectionState::Full);
    }

    // 5. Mark the selection end node as End (if text) or Full (if anything else).
    if (auto* layout_node = end_container->layout_node()) {
        if (is<DOM::Text>(*end_container))
            layout_node->set_selection_state(SelectionState::End);
        else
            layout_node->set_selection_state(SelectionState::Full);
    }

    // 6. Mark the nodes between start node and end node (in tree order) as Full.
    for (auto* node = start_container->next_in_pre_order(); node && node != end_container; node = node->next_in_pre_order()) {
        if (auto* layout_node = node->layout_node())
            layout_node->set_selection_state(SelectionState::Full);
    }
}

JS::GCPtr<Painting::Paintable> Viewport::create_paintable() const
{
    return Painting::ViewportPaintable::create(*this);
}

}
