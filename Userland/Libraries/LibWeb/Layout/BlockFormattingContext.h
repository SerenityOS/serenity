/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibWeb/Forward.h>
#include <LibWeb/Layout/BlockContainer.h>
#include <LibWeb/Layout/FormattingContext.h>

namespace Web::Layout {

// https://www.w3.org/TR/css-display/#block-formatting-context
class BlockFormattingContext : public FormattingContext {
public:
    explicit BlockFormattingContext(BlockContainer&, FormattingContext* parent);
    ~BlockFormattingContext();

    virtual void run(Box&, LayoutMode) override;

    bool is_initial() const;

    auto const& left_side_floats() const { return m_left_floats; }
    auto const& right_side_floats() const { return m_right_floats; }

    static float compute_theoretical_height(Box const&);
    void compute_width(Box&);

    // https://www.w3.org/TR/css-display/#block-formatting-context-root
    BlockContainer& root() { return static_cast<BlockContainer&>(context_box()); }
    BlockContainer const& root() const { return static_cast<BlockContainer const&>(context_box()); }

protected:
    static void compute_height(Box&);
    void compute_position(Box&);

private:
    virtual bool is_block_formatting_context() const final { return true; }

    void compute_width_for_floating_box(Box&);

    void compute_width_for_block_level_replaced_element_in_normal_flow(ReplacedBox&);

    void layout_initial_containing_block(LayoutMode);

    void layout_block_level_children(BlockContainer&, LayoutMode);
    void layout_inline_children(BlockContainer&, LayoutMode);

    void compute_vertical_box_model_metrics(Box& child_box, BlockContainer const& containing_block);
    void place_block_level_element_in_normal_flow_horizontally(Box& child_box, BlockContainer const&);
    void place_block_level_element_in_normal_flow_vertically(Box& child_box, BlockContainer const&);

    void layout_floating_child(Box& child, BlockContainer const& containing_block);

    void apply_transformations_to_children(Box&);

    enum class FloatSide {
        Left,
        Right,
    };

    struct FloatSideData {
        Vector<Box&> boxes;
        float y_offset { 0 };
    };

    FloatSideData m_left_floats;
    FloatSideData m_right_floats;
};

}
