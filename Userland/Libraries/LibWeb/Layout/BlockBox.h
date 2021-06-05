/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Layout/Box.h>
#include <LibWeb/Layout/LineBox.h>

namespace Web::Layout {

class BlockBox : public Box {
public:
    BlockBox(DOM::Document&, DOM::Node*, NonnullRefPtr<CSS::StyleProperties>);
    BlockBox(DOM::Document&, DOM::Node*, CSS::ComputedValues);
    virtual ~BlockBox() override;

    virtual void paint(PaintContext&, PaintPhase) override;

    virtual HitTestResult hit_test(const Gfx::IntPoint&, HitTestType) const override;

    BlockBox* previous_sibling() { return downcast<BlockBox>(Node::previous_sibling()); }
    const BlockBox* previous_sibling() const { return downcast<BlockBox>(Node::previous_sibling()); }
    BlockBox* next_sibling() { return downcast<BlockBox>(Node::next_sibling()); }
    const BlockBox* next_sibling() const { return downcast<BlockBox>(Node::next_sibling()); }

    template<typename Callback>
    void for_each_fragment(Callback);
    template<typename Callback>
    void for_each_fragment(Callback) const;

    virtual void split_into_lines(InlineFormattingContext&, LayoutMode) override;

    bool is_scrollable() const;
    const Gfx::FloatPoint& scroll_offset() const { return m_scroll_offset; }
    void set_scroll_offset(const Gfx::FloatPoint&);

private:
    virtual bool is_block_box() const final { return true; }
    virtual bool wants_mouse_events() const override { return false; }
    virtual bool handle_mousewheel(Badge<EventHandler>, const Gfx::IntPoint&, unsigned buttons, unsigned modifiers, int wheel_delta) override;

    bool should_clip_overflow() const;

    Gfx::FloatPoint m_scroll_offset;
};

template<>
inline bool Node::fast_is<BlockBox>() const { return is_block_box(); }

template<typename Callback>
void BlockBox::for_each_fragment(Callback callback)
{
    for (auto& line_box : line_boxes()) {
        for (auto& fragment : line_box.fragments()) {
            if (callback(fragment) == IterationDecision::Break)
                return;
        }
    }
}

template<typename Callback>
void BlockBox::for_each_fragment(Callback callback) const
{
    for (auto& line_box : line_boxes()) {
        for (auto& fragment : line_box.fragments()) {
            if (callback(fragment) == IterationDecision::Break)
                return;
        }
    }
}

}
