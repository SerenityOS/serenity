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

    Vector<Vector<bool>> occupation_grid;
    Vector<bool> occupation_grid_row;
    for (int column_index = 0; column_index < max((int)box.computed_values().grid_template_columns().size(), 1); column_index++)
        occupation_grid_row.append(false);
    for (int row_index = 0; row_index < max((int)box.computed_values().grid_template_rows().size(), 1); row_index++)
        occupation_grid.append(occupation_grid_row);
}
