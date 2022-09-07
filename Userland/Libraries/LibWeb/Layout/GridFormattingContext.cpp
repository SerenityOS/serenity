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

    auto maybe_add_column_to_occupation_grid = [](int needed_number_of_columns, Vector<Vector<bool>>& occupation_grid) -> void {
        int current_column_count = (int)occupation_grid[0].size();
        if (needed_number_of_columns <= current_column_count)
            return;
        for (auto& occupation_grid_row : occupation_grid)
            for (int idx = 0; idx < (needed_number_of_columns + 1) - current_column_count; idx++)
                occupation_grid_row.append(false);
    };

    auto maybe_add_row_to_occupation_grid = [](int needed_number_of_rows, Vector<Vector<bool>>& occupation_grid) -> void {
        if (needed_number_of_rows <= (int)occupation_grid.size())
            return;

        Vector<bool> new_occupation_grid_row;
        for (int idx = 0; idx < (int)occupation_grid[0].size(); idx++)
            new_occupation_grid_row.append(false);

        for (int idx = 0; idx < needed_number_of_rows - (int)occupation_grid.size(); idx++)
            occupation_grid.append(new_occupation_grid_row);
    };

    auto set_occupied_cells = [](int row_start, int row_end, int column_start, int column_end, Vector<Vector<bool>>& occupation_grid) -> void {
        for (int row_index = 0; row_index < (int)occupation_grid.size(); row_index++) {
            if (row_index >= row_start && row_index < row_end) {
                for (int column_index = 0; column_index < (int)occupation_grid[0].size(); column_index++) {
                    if (column_index >= column_start && column_index < column_end) {
                        occupation_grid[row_index][column_index] = true;
                    }
                }
            }
        }
    };

    // https://drafts.csswg.org/css-grid/#overview-placement
    // 2.2. Placing Items
    // The contents of the grid container are organized into individual grid items (analogous to
    // flex items), which are then assigned to predefined areas in the grid. They can be explicitly
    // placed using coordinates through the grid-placement properties or implicitly placed into
    // empty areas using auto-placement.
    struct PositionedBox {
        Box const& box;
        int row { 0 };
        int row_span { 1 };
        int column { 0 };
        int column_span { 1 };
        float computed_height { 0 };
    };
    Vector<PositionedBox> positioned_boxes;

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

    // FIXME: 0. Generate anonymous grid items

    // 1. Position anything that's not auto-positioned.
    for (size_t i = 0; i < boxes_to_place.size(); i++) {
        auto const& child_box = boxes_to_place[i];
        if (child_box.computed_values().grid_row_start().is_auto()
            || child_box.computed_values().grid_row_end().is_auto()
            || child_box.computed_values().grid_column_start().is_auto()
            || child_box.computed_values().grid_column_end().is_auto())
            continue;

        int row_start = child_box.computed_values().grid_row_start().position();
        int row_end = child_box.computed_values().grid_row_end().position();
        int column_start = child_box.computed_values().grid_column_start().position();
        int column_end = child_box.computed_values().grid_column_end().position();
        int row_span = 1;
        int column_span = 1;

        // https://drafts.csswg.org/css-grid/#grid-placement-int
        // [ <integer [−∞,−1]> | <integer [1,∞]> ] && <custom-ident>?
        // Contributes the Nth grid line to the grid item’s placement. If a negative integer is given, it
        // instead counts in reverse, starting from the end edge of the explicit grid.
        if (row_end < 0)
            row_end = static_cast<int>(occupation_grid.size()) + row_end + 2;
        if (column_end < 0)
            column_end = static_cast<int>(occupation_grid[0].size()) + column_end + 2;
        // FIXME: If a name is given as a <custom-ident>, only lines with that name are counted. If not enough
        // lines with that name exist, all implicit grid lines are assumed to have that name for the purpose
        // of finding this position.

        // FIXME: An <integer> value of zero makes the declaration invalid.

        // https://drafts.csswg.org/css-grid/#grid-placement-errors
        // 8.3.1. Grid Placement Conflict Handling
        // If the placement for a grid item contains two lines, and the start line is further end-ward than
        // the end line, swap the two lines. If the start line is equal to the end line, remove the end
        // line.
        if (row_start > row_end) {
            auto temp = row_end;
            row_end = row_start;
            row_start = temp;
        }
        if (column_start > column_end) {
            auto temp = column_end;
            column_end = column_start;
            column_start = temp;
        }
        if (row_start != row_end)
            row_span = row_end - row_start;
        if (column_start != column_end)
            column_span = column_end - column_start;
        // FIXME: If the placement contains two spans, remove the one contributed by the end grid-placement
        // property.

        // FIXME: If the placement contains only a span for a named line, replace it with a span of 1.

        row_start -= 1;
        column_start -= 1;
        positioned_boxes.append({ child_box, row_start, row_span, column_start, column_span });

        maybe_add_row_to_occupation_grid(row_start + row_span, occupation_grid);
        maybe_add_column_to_occupation_grid(column_start + column_span, occupation_grid);
        set_occupied_cells(row_start, row_start + row_span, column_start, column_start + column_span, occupation_grid);
        boxes_to_place.remove(i);
        i--;
    }

    // 2. Process the items locked to a given row.
    // FIXME: Do "dense" packing
    for (size_t i = 0; i < boxes_to_place.size(); i++) {
        auto const& child_box = boxes_to_place[i];
        if (child_box.computed_values().grid_row_start().is_auto()
            || child_box.computed_values().grid_row_end().is_auto())
            continue;

        int row_start = child_box.computed_values().grid_row_start().position();
        int row_end = child_box.computed_values().grid_row_end().position();
        int row_span = 1;

        // https://drafts.csswg.org/css-grid/#grid-placement-int
        // [ <integer [−∞,−1]> | <integer [1,∞]> ] && <custom-ident>?
        // Contributes the Nth grid line to the grid item’s placement. If a negative integer is given, it
        // instead counts in reverse, starting from the end edge of the explicit grid.
        if (row_end < 0)
            row_end = static_cast<int>(occupation_grid.size()) + row_end + 2;
        // FIXME: If a name is given as a <custom-ident>, only lines with that name are counted. If not enough
        // lines with that name exist, all implicit grid lines are assumed to have that name for the purpose
        // of finding this position.

        // FIXME: An <integer> value of zero makes the declaration invalid.

        // https://drafts.csswg.org/css-grid/#grid-placement-errors
        // 8.3.1. Grid Placement Conflict Handling
        // If the placement for a grid item contains two lines, and the start line is further end-ward than
        // the end line, swap the two lines. If the start line is equal to the end line, remove the end
        // line.
        if (row_start > row_end) {
            auto temp = row_end;
            row_end = row_start;
            row_start = temp;
        }
        if (row_start != row_end)
            row_span = row_end - row_start;
        // FIXME: If the placement contains two spans, remove the one contributed by the end grid-placement
        // property.

        // FIXME: If the placement contains only a span for a named line, replace it with a span of 1.

        row_start -= 1;
        maybe_add_row_to_occupation_grid(row_start + row_span, occupation_grid);

        int column_start = 0;
        int column_span = 1;
        bool found_available_column = false;
        for (int column_index = column_start; column_index < (int)occupation_grid[0].size(); column_index++) {
            if (!occupation_grid[0][column_index]) {
                found_available_column = true;
                column_start = column_index;
                break;
            }
        }
        if (!found_available_column) {
            column_start = occupation_grid[0].size();
            maybe_add_column_to_occupation_grid(column_start + column_span, occupation_grid);
        }
        set_occupied_cells(row_start, row_start + row_span, column_start, column_start + column_span, occupation_grid);

        positioned_boxes.append({ child_box, row_start, row_span, column_start, column_span });
        boxes_to_place.remove(i);
        i--;
    }

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
