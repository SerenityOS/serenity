/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Painter.h>
#include <LibWeb/Dump.h>
#include <LibWeb/Layout/InitialContainingBlockBox.h>
#include <LibWeb/Page/BrowsingContext.h>
#include <LibWeb/Painting/StackingContext.h>

namespace Web::Layout {

InitialContainingBlockBox::InitialContainingBlockBox(DOM::Document& document, NonnullRefPtr<CSS::StyleProperties> style)
    : BlockBox(document, &document, move(style))
{
}

InitialContainingBlockBox::~InitialContainingBlockBox()
{
}

void InitialContainingBlockBox::build_stacking_context_tree()
{
    if (stacking_context())
        return;

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

void InitialContainingBlockBox::paint_all_phases(PaintContext& context)
{
    context.painter().translate(-context.viewport_rect().location());
    stacking_context()->paint(context);
}

HitTestResult InitialContainingBlockBox::hit_test(const Gfx::IntPoint& position, HitTestType type) const
{
    return stacking_context()->hit_test(position, type);
}

void InitialContainingBlockBox::recompute_selection_states()
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

void InitialContainingBlockBox::set_selection(const LayoutRange& selection)
{
    m_selection = selection;
    recompute_selection_states();
}

void InitialContainingBlockBox::set_selection_end(const LayoutPosition& position)
{
    m_selection.set_end(position);
    recompute_selection_states();
}

}
