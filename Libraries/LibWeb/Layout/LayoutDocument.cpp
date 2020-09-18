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
#include <LibWeb/Layout/LayoutDocument.h>
#include <LibWeb/Layout/LayoutImage.h>
#include <LibWeb/Layout/LayoutWidget.h>
#include <LibWeb/Page/Frame.h>
#include <LibWeb/Painting/StackingContext.h>

namespace Web {

LayoutDocument::LayoutDocument(DOM::Document& document, NonnullRefPtr<CSS::StyleProperties> style)
    : LayoutBlock(document, &document, move(style))
{
}

LayoutDocument::~LayoutDocument()
{
}

void LayoutDocument::build_stacking_context_tree()
{
    if (stacking_context())
        return;

    set_stacking_context(make<StackingContext>(*this, nullptr));

    for_each_in_subtree_of_type<LayoutBox>([&](LayoutBox& box) {
        if (&box == this)
            return IterationDecision::Continue;
        if (!box.establishes_stacking_context()) {
            ASSERT(!box.stacking_context());
            return IterationDecision::Continue;
        }
        auto* parent_context = box.enclosing_stacking_context();
        ASSERT(parent_context);
        box.set_stacking_context(make<StackingContext>(box, parent_context));
        return IterationDecision::Continue;
    });
}

void LayoutDocument::layout(LayoutMode layout_mode)
{
    build_stacking_context_tree();

    set_width(frame().size().width());

    LayoutNode::layout(layout_mode);

    ASSERT(!children_are_inline());

    float lowest_bottom = 0;
    for_each_child([&](auto& child) {
        ASSERT(is<LayoutBlock>(child));
        auto& child_block = downcast<LayoutBlock>(child);
        lowest_bottom = max(lowest_bottom, child_block.absolute_rect().bottom());
    });
    set_height(lowest_bottom);

    layout_absolutely_positioned_descendants();

    // FIXME: This is a total hack. Make sure any GUI::Widgets are moved into place after layout.
    //        We should stop embedding GUI::Widgets entirely, since that won't work out-of-process.
    for_each_in_subtree_of_type<LayoutWidget>([&](auto& widget) {
        widget.update_widget();
        return IterationDecision::Continue;
    });
}

void LayoutDocument::did_set_viewport_rect(Badge<Frame>, const Gfx::IntRect& a_viewport_rect)
{
    Gfx::FloatRect viewport_rect(a_viewport_rect.x(), a_viewport_rect.y(), a_viewport_rect.width(), a_viewport_rect.height());
    for_each_in_subtree_of_type<LayoutImage>([&](auto& layout_image) {
        const_cast<LayoutImage&>(layout_image).set_visible_in_viewport({}, viewport_rect.intersects(layout_image.absolute_rect()));
        return IterationDecision::Continue;
    });
}

void LayoutDocument::paint_all_phases(PaintContext& context)
{
    paint(context, PaintPhase::Background);
    paint(context, PaintPhase::Border);
    paint(context, PaintPhase::Foreground);
    if (context.has_focus())
        paint(context, PaintPhase::FocusOutline);
    paint(context, PaintPhase::Overlay);
}

void LayoutDocument::paint(PaintContext& context, PaintPhase phase)
{
    stacking_context()->paint(context, phase);
}

HitTestResult LayoutDocument::hit_test(const Gfx::IntPoint& position, HitTestType type) const
{
    return stacking_context()->hit_test(position, type);
}

void LayoutDocument::recompute_selection_states()
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

void LayoutDocument::set_selection(const LayoutRange& selection)
{
    m_selection = selection;
    recompute_selection_states();
}

void LayoutDocument::set_selection_end(const LayoutPosition& position)
{
    m_selection.set_end(position);
    recompute_selection_states();
}

}
