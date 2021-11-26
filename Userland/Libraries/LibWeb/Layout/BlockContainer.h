/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Layout/Box.h>
#include <LibWeb/Layout/LineBox.h>

namespace Web::Layout {

// https://www.w3.org/TR/css-display/#block-container
class BlockContainer : public Box {
public:
    BlockContainer(DOM::Document&, DOM::Node*, NonnullRefPtr<CSS::StyleProperties>);
    BlockContainer(DOM::Document&, DOM::Node*, CSS::ComputedValues);
    virtual ~BlockContainer() override;

    virtual void paint(PaintContext&, PaintPhase) override;

    virtual HitTestResult hit_test(const Gfx::IntPoint&, HitTestType) const override;

    BlockContainer* previous_sibling() { return verify_cast<BlockContainer>(Node::previous_sibling()); }
    const BlockContainer* previous_sibling() const { return verify_cast<BlockContainer>(Node::previous_sibling()); }
    BlockContainer* next_sibling() { return verify_cast<BlockContainer>(Node::next_sibling()); }
    const BlockContainer* next_sibling() const { return verify_cast<BlockContainer>(Node::next_sibling()); }

    template<typename Callback>
    void for_each_fragment(Callback);
    template<typename Callback>
    void for_each_fragment(Callback) const;

    virtual void split_into_lines(InlineFormattingContext&, LayoutMode) override;

    bool is_scrollable() const;
    const Gfx::FloatPoint& scroll_offset() const { return m_scroll_offset; }
    void set_scroll_offset(const Gfx::FloatPoint&);

    Vector<LineBox>& line_boxes() { return m_line_boxes; }
    const Vector<LineBox>& line_boxes() const { return m_line_boxes; }

    LineBox& ensure_last_line_box();
    LineBox& add_line_box();

protected:
    Vector<LineBox> m_line_boxes;

private:
    virtual bool is_block_container() const final { return true; }
    virtual bool wants_mouse_events() const override { return false; }
    virtual bool handle_mousewheel(Badge<EventHandler>, const Gfx::IntPoint&, unsigned buttons, unsigned modifiers, int wheel_delta) override;

    bool should_clip_overflow() const;

    Gfx::FloatPoint m_scroll_offset;
};

template<>
inline bool Node::fast_is<BlockContainer>() const { return is_block_container(); }

template<typename Callback>
void BlockContainer::for_each_fragment(Callback callback)
{
    for (auto& line_box : line_boxes()) {
        for (auto& fragment : line_box.fragments()) {
            if (callback(fragment) == IterationDecision::Break)
                return;
        }
    }
}

template<typename Callback>
void BlockContainer::for_each_fragment(Callback callback) const
{
    for (auto& line_box : line_boxes()) {
        for (auto& fragment : line_box.fragments()) {
            if (callback(fragment) == IterationDecision::Break)
                return;
        }
    }
}

}
