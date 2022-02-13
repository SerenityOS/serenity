/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Painter.h>
#include <LibWeb/Dump.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/Layout/InitialContainingBlock.h>
#include <LibWeb/Painting/StackingContext.h>

namespace Web::Layout {

InitialContainingBlock::InitialContainingBlock(DOM::Document& document, NonnullRefPtr<CSS::StyleProperties> style)
    : BlockContainer(document, &document, move(style))
{
}

InitialContainingBlock::~InitialContainingBlock()
{
}

void InitialContainingBlock::build_stacking_context_tree()
{
    set_stacking_context(make<StackingContext>(*this, nullptr));

    for_each_in_inclusive_subtree_of_type<Box>([&](Box& box) {
        if (&box == this)
            return IterationDecision::Continue;
        if (!box.establishes_stacking_context()) {
            VERIFY(!box.stacking_context());
            return IterationDecision::Continue;
        }
        auto* parent_context = box.enclosing_stacking_context();
        VERIFY(parent_context);
        box.set_stacking_context(make<StackingContext>(box, parent_context));
        return IterationDecision::Continue;
    });
}

void InitialContainingBlock::paint_all_phases(PaintContext& context)
{
    context.painter().fill_rect(enclosing_int_rect(absolute_rect()), context.palette().base());
    context.painter().translate(-context.viewport_rect().location());
    stacking_context()->paint(context);
}

HitTestResult InitialContainingBlock::hit_test(const Gfx::IntPoint& position, HitTestType type) const
{
    return stacking_context()->hit_test(position, type);
}

void InitialContainingBlock::recompute_selection_states()
{
    SelectionState state = SelectionState::None;

    auto selection = this->selection().normalized();

    for_each_in_inclusive_subtree([&](auto& layout_node) {
        if (!selection.is_valid()) {
            // Everything gets SelectionState::None.
        } else if (&layout_node == selection.start().layout_node && &layout_node == selection.end().layout_node) {
            state = SelectionState::StartAndEnd;
        } else if (&layout_node == selection.start().layout_node) {
            state = SelectionState::Start;
        } else if (&layout_node == selection.end().layout_node) {
            state = SelectionState::End;
        } else {
            if (state == SelectionState::Start)
                state = SelectionState::Full;
            else if (state == SelectionState::End || state == SelectionState::StartAndEnd)
                state = SelectionState::None;
        }
        layout_node.set_selection_state(state);
        return IterationDecision::Continue;
    });
}

void InitialContainingBlock::set_selection(const LayoutRange& selection)
{
    m_selection = selection;
    recompute_selection_states();
}

void InitialContainingBlock::set_selection_end(const LayoutPosition& position)
{
    m_selection.set_end(position);
    recompute_selection_states();
}

}
