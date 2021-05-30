/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibWeb/Forward.h>
#include <LibWeb/Layout/FormattingContext.h>

namespace Web::Layout {

class BlockFormattingContext : public FormattingContext {
public:
    explicit BlockFormattingContext(Box&, FormattingContext* parent);
    ~BlockFormattingContext();

    virtual void run(Box&, LayoutMode) override;

    bool is_initial() const;

    const Vector<Box*>& left_floating_boxes() const { return m_left_floating_boxes; }
    const Vector<Box*>& right_floating_boxes() const { return m_right_floating_boxes; }

    static float compute_theoretical_height(const Box&);
    void compute_width(Box&);

protected:
    static void compute_height(Box&);
    void compute_position(Box&);

private:
    virtual bool is_block_formatting_context() const final { return true; }

    void compute_width_for_floating_box(Box&);

    void compute_width_for_block_level_replaced_element_in_normal_flow(ReplacedBox&);

    void layout_initial_containing_block(LayoutMode);

    void layout_block_level_children(Box&, LayoutMode);
    void layout_inline_children(Box&, LayoutMode);

    void place_block_level_replaced_element_in_normal_flow(Box& child, Box& container);
    void place_block_level_non_replaced_element_in_normal_flow(Box& child, Box& container);

    void layout_floating_child(Box&, Box& containing_block);

    Vector<Box*> m_left_floating_boxes;
    Vector<Box*> m_right_floating_boxes;
};

}
