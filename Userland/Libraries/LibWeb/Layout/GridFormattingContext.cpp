/*
 * Copyright (c) 2022, Martin Falisse <mfalisse@outlook.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Node.h>
#include <LibWeb/Layout/Box.h>
#include <LibWeb/Layout/GridFormattingContext.h>

namespace Web::Layout {

GridFormattingContext::GridFormattingContext(LayoutState& state, BlockContainer const& block_container, FormattingContext* parent)
    : BlockFormattingContext(state, block_container, parent)
{
}

GridFormattingContext::~GridFormattingContext() = default;

void GridFormattingContext::run(Box const& box, LayoutMode)
{
    auto should_skip_is_anonymous_text_run = [&](Box& child_box) -> bool {
        if (child_box.is_anonymous() && !child_box.first_child_of_type<BlockContainer>()) {
            bool contains_only_white_space = true;
            child_box.for_each_in_subtree([&](auto const& node) {
                if (!is<TextNode>(node) || !static_cast<TextNode const&>(node).dom_node().data().is_whitespace()) {
                    contains_only_white_space = false;
                    return IterationDecision::Break;
                }
                return IterationDecision::Continue;
            });
            if (contains_only_white_space)
                return true;
        }
        return false;
    };

    // https://drafts.csswg.org/css-grid/#overview-placement
    // 2.2. Placing Items
    // The contents of the grid container are organized into individual grid items (analogous to
    // flex items), which are then assigned to predefined areas in the grid. They can be explicitly
    // placed using coordinates through the grid-placement properties or implicitly placed into
    // empty areas using auto-placement.

    Vector<Vector<bool>> occupation_grid;
    Vector<bool> occupation_grid_row;
    for (int column_index = 0; column_index < max((int)box.computed_values().grid_template_columns().size(), 1); column_index++)
        occupation_grid_row.append(false);
    for (int row_index = 0; row_index < max((int)box.computed_values().grid_template_rows().size(), 1); row_index++)
        occupation_grid.append(occupation_grid_row);

    Vector<Box const&> boxes_to_place;
    box.for_each_child_of_type<Box>([&](Box& child_box) {
        if (should_skip_is_anonymous_text_run(child_box))
            return IterationDecision::Continue;
        boxes_to_place.append(child_box);
        return IterationDecision::Continue;
    });

    // https://drafts.csswg.org/css-grid/#auto-placement-algo
    // 8.5. Grid Item Placement Algorithm

    // 0. Generate anonymous grid items

    // 1. Position anything that's not auto-positioned.

    // 2. Process the items locked to a given row.

    // 3. Determine the columns in the implicit grid.

    // 3.1. Start with the columns from the explicit grid.

    // 3.2. Among all the items with a definite column position (explicitly positioned items, items
    // positioned in the previous step, and items not yet positioned but with a definite column) add
    // columns to the beginning and end of the implicit grid as necessary to accommodate those items.
    // NOTE: "Explicitly positioned items" and "items positioned in the previous step" done in step 1
    // and 2, respectively. Adding columns for "items not yet positioned but with a definite column"
    // will be done in step 4.

    // 3.3. If the largest column span among all the items without a definite column position is larger
    // than the width of the implicit grid, add columns to the end of the implicit grid to accommodate
    // that column span.

    // 4. Position the remaining grid items.
    // For each grid item that hasn't been positioned by the previous steps, in order-modified document
    // order:
}
