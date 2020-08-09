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

#pragma once

#include <LibWeb/Layout/LayoutBox.h>
#include <LibWeb/Layout/LineBox.h>

namespace Web {

class LayoutBlock : public LayoutBox {
public:
    LayoutBlock(DOM::Document&, DOM::Node*, NonnullRefPtr<CSS::StyleProperties>);
    virtual ~LayoutBlock() override;

    virtual const char* class_name() const override { return "LayoutBlock"; }

    virtual void layout(LayoutMode = LayoutMode::Default) override;
    virtual void paint(PaintContext&, PaintPhase) override;

    virtual LayoutNode& inline_wrapper() override;

    Vector<LineBox>& line_boxes() { return m_line_boxes; }
    const Vector<LineBox>& line_boxes() const { return m_line_boxes; }

    LineBox& ensure_last_line_box();
    LineBox& add_line_box();

    virtual HitTestResult hit_test(const Gfx::IntPoint&, HitTestType) const override;

    LayoutBlock* previous_sibling() { return downcast<LayoutBlock>(LayoutNode::previous_sibling()); }
    const LayoutBlock* previous_sibling() const { return downcast<LayoutBlock>(LayoutNode::previous_sibling()); }
    LayoutBlock* next_sibling() { return downcast<LayoutBlock>(LayoutNode::next_sibling()); }
    const LayoutBlock* next_sibling() const { return downcast<LayoutBlock>(LayoutNode::next_sibling()); }

    template<typename Callback>
    void for_each_fragment(Callback);
    template<typename Callback>
    void for_each_fragment(Callback) const;

    virtual void split_into_lines(LayoutBlock& container, LayoutMode) override;

    void layout_inside(LayoutMode);

protected:
    void compute_width();
    void compute_height();
    void layout_absolutely_positioned_descendants();

    virtual float width_of_logical_containing_block() const;

private:
    virtual bool is_block() const override { return true; }

    struct ShrinkToFitResult {
        float preferred_width { 0 };
        float preferred_minimum_width { 0 };
    };
    ShrinkToFitResult calculate_shrink_to_fit_width();

    void compute_width_for_absolutely_positioned_block();

    void place_block_level_non_replaced_element_in_normal_flow(LayoutBlock&);
    void place_block_level_replaced_element_in_normal_flow(LayoutReplaced&);
    void layout_absolutely_positioned_descendant(LayoutBox&);

    NonnullRefPtr<CSS::StyleProperties> style_for_anonymous_block() const;

    void layout_inline_children(LayoutMode);
    void layout_contained_boxes(LayoutMode);

    Vector<LineBox> m_line_boxes;
};

template<typename Callback>
void LayoutBlock::for_each_fragment(Callback callback)
{
    for (auto& line_box : line_boxes()) {
        for (auto& fragment : line_box.fragments()) {
            if (callback(fragment) == IterationDecision::Break)
                return;
        }
    }
}

template<typename Callback>
void LayoutBlock::for_each_fragment(Callback callback) const
{
    for (auto& line_box : line_boxes()) {
        for (auto& fragment : line_box.fragments()) {
            if (callback(fragment) == IterationDecision::Break)
                return;
        }
    }
}

}

AK_BEGIN_TYPE_TRAITS(Web::LayoutBlock)
static bool is_type(const Web::LayoutNode& layout_node) { return layout_node.is_block(); }
AK_END_TYPE_TRAITS()
