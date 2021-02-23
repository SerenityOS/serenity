/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <LibWeb/Dump.h>
#include <LibWeb/Layout/InitialContainingBlockBox.h>
#include <LibWeb/Page/Frame.h>
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

    for_each_in_subtree_of_type<Box>([&](Box& box) {
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
    paint(context, PaintPhase::Background);
    paint(context, PaintPhase::Border);
    paint(context, PaintPhase::Foreground);
    if (context.has_focus())
        paint(context, PaintPhase::FocusOutline);
    paint(context, PaintPhase::Overlay);
}

void InitialContainingBlockBox::paint(PaintContext& context, PaintPhase phase)
{
    stacking_context()->paint(context, phase);
}

HitTestResult InitialContainingBlockBox::hit_test(const Gfx::IntPoint& position, HitTestType type) const
{
    return stacking_context()->hit_test(position, type);
}

void InitialContainingBlockBox::recompute_selection_states()
{
    SelectionState state = SelectionState::None;

    auto selection = this->selection().normalized();

    for_each_in_subtree([&](auto& layout_node) {
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
