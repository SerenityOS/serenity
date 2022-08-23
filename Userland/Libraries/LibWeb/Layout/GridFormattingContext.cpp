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

    auto number_of_columns = (int)box.computed_values().grid_template_columns().size();
    struct GridRow {
        float height { 0 };
        Vector<Box&> boxes;
    };
    Vector<GridRow> grid_rows;

    auto current_column_count = 0;
    box.for_each_child_of_type<Box>([&](Box& child_box) {
        if (should_skip_is_anonymous_text_run(child_box))
            return IterationDecision::Continue;

        if (current_column_count == 0)
            grid_rows.append(GridRow());
        GridRow& grid_row = grid_rows.last();
        grid_row.boxes.append(child_box);

        auto& child_box_state = m_state.get_mutable(child_box);
        if (child_box_state.content_height() > grid_row.height)
            grid_row.height = child_box_state.content_height();
        (void)layout_inside(child_box, LayoutMode::Normal);
        if (child_box_state.content_height() > grid_row.height)
            grid_row.height = child_box_state.content_height();

        current_column_count++;
        if (current_column_count == number_of_columns)
            current_column_count = 0;
        return IterationDecision::Continue;
    });

    auto& box_state = m_state.get_mutable(box);
    float current_y_position = 0;
    current_column_count = 0;
    for (auto& grid_row : grid_rows) {
        for (auto& child_box : grid_row.boxes) {
            if (should_skip_is_anonymous_text_run(child_box))
                continue;
            auto& child_box_state = m_state.get_mutable(child_box);

            // FIXME: instead of dividing the parent width equally between the columns, should use
            // the values in the GridTrackSize objects.
            child_box_state.set_content_width(box_state.content_width() / number_of_columns);
            child_box_state.set_content_height(grid_row.height);
            child_box_state.offset = { current_column_count * (box_state.content_width() / number_of_columns), current_y_position };

            current_column_count++;
            if (current_column_count == number_of_columns) {
                current_y_position += grid_row.height;
                current_column_count = 0;
            }
            continue;
        }
    }
}

}
