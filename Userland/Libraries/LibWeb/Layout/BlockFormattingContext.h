/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibWeb/Forward.h>
#include <LibWeb/Layout/BlockContainer.h>
#include <LibWeb/Layout/FormattingContext.h>

namespace Web::Layout {

class LineBuilder;

// https://www.w3.org/TR/css-display/#block-formatting-context
class BlockFormattingContext : public FormattingContext {
public:
    explicit BlockFormattingContext(LayoutState&, BlockContainer const&, FormattingContext* parent);
    ~BlockFormattingContext();

    virtual void run(Box const&, LayoutMode, AvailableSpace const&) override;
    virtual CSSPixels automatic_content_height() const override;

    bool is_initial() const;

    auto const& left_side_floats() const { return m_left_floats; }
    auto const& right_side_floats() const { return m_right_floats; }

    void compute_width(Box const&, AvailableSpace const&, LayoutMode = LayoutMode::Normal);

    // https://www.w3.org/TR/css-display/#block-formatting-context-root
    BlockContainer const& root() const { return static_cast<BlockContainer const&>(context_box()); }

    virtual void parent_context_did_dimension_child_root_box() override;

    void compute_height(Box const&, AvailableSpace const&);

    void add_absolutely_positioned_box(Box const& box) { m_absolutely_positioned_boxes.append(box); }

    SpaceUsedByFloats space_used_by_floats(CSSPixels y) const;

    virtual CSSPixels greatest_child_width(Box const&) override;

    void layout_floating_box(Box const& child, BlockContainer const& containing_block, LayoutMode, AvailableSpace const&, CSSPixels y, LineBuilder* = nullptr);

    void layout_block_level_box(Box const&, BlockContainer const&, LayoutMode, CSSPixels& bottom_of_lowest_margin_box, AvailableSpace const&, CSSPixels& current_y);

    virtual bool can_determine_size_of_child() const override { return true; }
    virtual void determine_width_of_child(Box const&, AvailableSpace const&) override;
    virtual void determine_height_of_child(Box const&, AvailableSpace const&) override;

private:
    CSSPixels compute_auto_height_for_block_level_element(Box const&, AvailableSpace const&);

    void compute_width_for_floating_box(Box const&, AvailableSpace const&);

    void compute_width_for_block_level_replaced_element_in_normal_flow(ReplacedBox const&, AvailableSpace const&);

    CSSPixels compute_width_for_table_wrapper(Box const&, AvailableSpace const&);

    void layout_initial_containing_block(LayoutMode, AvailableSpace const&);

    void layout_block_level_children(BlockContainer const&, LayoutMode, AvailableSpace const&);
    void layout_inline_children(BlockContainer const&, LayoutMode, AvailableSpace const&);

    static void resolve_vertical_box_model_metrics(Box const& box, LayoutState&);
    void place_block_level_element_in_normal_flow_horizontally(Box const& child_box, AvailableSpace const&);
    void place_block_level_element_in_normal_flow_vertically(Box const&, CSSPixels y);

    void layout_list_item_marker(ListItemBox const&);

    enum class FloatSide {
        Left,
        Right,
    };

    struct FloatingBox {
        Box const& box;
        // Offset from left/right edge to the left content edge of `box`.
        CSSPixels offset_from_edge { 0 };

        // Top margin edge of `box`.
        CSSPixels top_margin_edge { 0 };

        // Bottom margin edge of `box`.
        CSSPixels bottom_margin_edge { 0 };
    };

    struct FloatSideData {
        // Floating boxes currently accumulating on this side.
        Vector<FloatingBox&> current_boxes;

        // Combined width of boxes currently accumulating on this side.
        // This is the innermost margin of the innermost floating box.
        CSSPixels current_width { 0 };

        // Highest value of `m_current_width` we've seen.
        CSSPixels max_width { 0 };

        // All floating boxes encountered thus far within this BFC.
        Vector<NonnullOwnPtr<FloatingBox>> all_boxes;

        // Current Y offset from BFC root top.
        CSSPixels y_offset { 0 };

        void clear()
        {
            current_boxes.clear();
            current_width = 0;
        }
    };

    struct BlockMarginState {
        Vector<CSSPixels> current_collapsible_margins;
        Function<void(CSSPixels)> block_container_y_position_update_callback;
        bool box_last_in_flow_child_margin_bottom_collapsed { false };

        void add_margin(CSSPixels margin)
        {
            current_collapsible_margins.append(margin);
        }

        void register_block_container_y_position_update_callback(Function<void(CSSPixels)> callback)
        {
            block_container_y_position_update_callback = move(callback);
        }

        CSSPixels current_collapsed_margin() const;

        bool has_block_container_waiting_for_final_y_position() const
        {
            return static_cast<bool>(block_container_y_position_update_callback);
        }

        void update_block_waiting_for_final_y_position() const
        {
            if (block_container_y_position_update_callback) {
                CSSPixels collapsed_margin = current_collapsed_margin();
                block_container_y_position_update_callback(collapsed_margin);
            }
        }

        void reset()
        {
            block_container_y_position_update_callback = {};
            current_collapsible_margins.clear();
        }
    };

    BlockMarginState m_margin_state;

    FloatSideData m_left_floats;
    FloatSideData m_right_floats;

    Vector<Box const&> m_absolutely_positioned_boxes;

    bool m_was_notified_after_parent_dimensioned_my_root_box { false };
};

}
