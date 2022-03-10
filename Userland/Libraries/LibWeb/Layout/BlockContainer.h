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

    virtual void paint(PaintContext&, Painting::PaintPhase) override;

    virtual HitTestResult hit_test(const Gfx::IntPoint&, HitTestType) const override;

    BlockContainer* previous_sibling() { return verify_cast<BlockContainer>(Node::previous_sibling()); }
    const BlockContainer* previous_sibling() const { return verify_cast<BlockContainer>(Node::previous_sibling()); }
    BlockContainer* next_sibling() { return verify_cast<BlockContainer>(Node::next_sibling()); }
    const BlockContainer* next_sibling() const { return verify_cast<BlockContainer>(Node::next_sibling()); }

    bool is_scrollable() const;
    const Gfx::FloatPoint& scroll_offset() const { return m_scroll_offset; }
    void set_scroll_offset(const Gfx::FloatPoint&);

    Painting::PaintableWithLines const* paint_box() const;

private:
    virtual bool is_block_container() const final { return true; }
    virtual bool wants_mouse_events() const override { return false; }
    virtual bool handle_mousewheel(Badge<EventHandler>, const Gfx::IntPoint&, unsigned buttons, unsigned modifiers, int wheel_delta_x, int wheel_delta_y) override;

    bool should_clip_overflow() const;

    Gfx::FloatPoint m_scroll_offset;
};

template<>
inline bool Node::fast_is<BlockContainer>() const { return is_block_container(); }

}
