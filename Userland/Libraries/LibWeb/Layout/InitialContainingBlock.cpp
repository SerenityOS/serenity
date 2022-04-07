/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Dump.h>
#include <LibWeb/Layout/InitialContainingBlock.h>
#include <LibWeb/Painting/PaintableBox.h>
#include <LibWeb/Painting/StackingContext.h>

namespace Web::Layout {

InitialContainingBlock::InitialContainingBlock(DOM::Document& document, NonnullRefPtr<CSS::StyleProperties> style)
    : BlockContainer(document, &document, move(style))
{
}

InitialContainingBlock::~InitialContainingBlock() = default;

void InitialContainingBlock::build_stacking_context_tree_if_needed()
{
    if (paint_box()->stacking_context())
        return;
    build_stacking_context_tree();
}

void InitialContainingBlock::build_stacking_context_tree()
{
    const_cast<Painting::PaintableWithLines*>(paint_box())->set_stacking_context(make<Painting::StackingContext>(*this, nullptr));

    for_each_in_subtree_of_type<Box>([&](Box& box) {
        if (!box.paint_box())
            return IterationDecision::Continue;
        const_cast<Painting::PaintableBox*>(box.paint_box())->invalidate_stacking_context();
        if (!box.establishes_stacking_context()) {
            VERIFY(!box.paint_box()->stacking_context());
            return IterationDecision::Continue;
        }
        auto* parent_context = const_cast<Painting::PaintableBox*>(box.paint_box())->enclosing_stacking_context();
        VERIFY(parent_context);
        const_cast<Painting::PaintableBox*>(box.paint_box())->set_stacking_context(make<Painting::StackingContext>(box, parent_context));
        return IterationDecision::Continue;
    });

    const_cast<Painting::PaintableWithLines*>(paint_box())->stacking_context()->sort();
}

void InitialContainingBlock::paint_all_phases(PaintContext& context)
{
    build_stacking_context_tree_if_needed();
    context.painter().fill_rect(enclosing_int_rect(paint_box()->absolute_rect()), document().background_color(context.palette()));
    context.painter().translate(-context.viewport_rect().location());
    paint_box()->stacking_context()->paint(context);
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

void InitialContainingBlock::set_selection(LayoutRange const& selection)
{
    m_selection = selection;
    recompute_selection_states();
}

void InitialContainingBlock::set_selection_end(LayoutPosition const& position)
{
    m_selection.set_end(position);
    recompute_selection_states();
}

}
