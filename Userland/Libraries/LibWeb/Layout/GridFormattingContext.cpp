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
    // NOTE: "implicit grid" here is the same as the occupation_grid

    // 3.1. Start with the columns from the explicit grid.
    // NOTE: Done in step 1.

    // 3.2. Among all the items with a definite column position (explicitly positioned items, items
    // positioned in the previous step, and items not yet positioned but with a definite column) add
    // columns to the beginning and end of the implicit grid as necessary to accommodate those items.
    // NOTE: "Explicitly positioned items" and "items positioned in the previous step" done in step 1
    // and 2, respectively. Adding columns for "items not yet positioned but with a definite column"
    // will be done in step 4.

    // 3.3. If the largest column span among all the items without a definite column position is larger
    // than the width of the implicit grid, add columns to the end of the implicit grid to accommodate
    // that column span.
    // NOTE: Done in step 1, 2, and will be done in step 4.

    // 4. Position the remaining grid items.
    // For each grid item that hasn't been positioned by the previous steps, in order-modified document
    // order:
    auto auto_placement_cursor_x = 0;
    auto auto_placement_cursor_y = 0;
    for (size_t i = 0; i < boxes_to_place.size(); i++) {
        auto const& child_box = boxes_to_place[i];
        // 4.1. For sparse packing:
        // FIXME: no distinction made. See #4.2

        // 4.1.1. If the item has a definite column position:
        if (!child_box.computed_values().grid_column_start().is_auto()) {
            int column_start = child_box.computed_values().grid_column_start().position();
            int column_end = child_box.computed_values().grid_column_end().position();
            int column_span = 1;

            // https://drafts.csswg.org/css-grid/#grid-placement-int
            // [ <integer [−∞,−1]> | <integer [1,∞]> ] && <custom-ident>?
            // Contributes the Nth grid line to the grid item’s placement. If a negative integer is given, it
            // instead counts in reverse, starting from the end edge of the explicit grid.
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
            if (!child_box.computed_values().grid_column_end().is_auto()) {
                if (column_start > column_end) {
                    auto temp = column_end;
                    column_end = column_start;
                    column_start = temp;
                }
                if (column_start != column_end)
                    column_span = column_end - column_start;
            }
            // FIXME: If the placement contains two spans, remove the one contributed by the end grid-placement
            // property.

            // FIXME: If the placement contains only a span for a named line, replace it with a span of 1.

            column_start -= 1;

            // 4.1.1.1. Set the column position of the cursor to the grid item's column-start line. If this is
            // less than the previous column position of the cursor, increment the row position by 1.
            auto_placement_cursor_x = column_start;
            if (column_start < auto_placement_cursor_x)
                auto_placement_cursor_y++;

            maybe_add_column_to_occupation_grid(auto_placement_cursor_x + 1, occupation_grid);
            maybe_add_row_to_occupation_grid(auto_placement_cursor_y + 1, occupation_grid);

            // 4.1.1.2. Increment the cursor's row position until a value is found where the grid item does not
            // overlap any occupied grid cells (creating new rows in the implicit grid as necessary).
            while (true) {
                if (!occupation_grid[auto_placement_cursor_y][column_start]) {
                    break;
                }
                auto_placement_cursor_y++;
                maybe_add_row_to_occupation_grid(auto_placement_cursor_y + 1, occupation_grid);
            }
            // 4.1.1.3. Set the item's row-start line to the cursor's row position, and set the item's row-end
            // line according to its span from that position.
            set_occupied_cells(auto_placement_cursor_y, auto_placement_cursor_y + 1, column_start, column_start + column_span, occupation_grid);

            positioned_boxes.append({ child_box, auto_placement_cursor_y, 1, column_start, column_span });
        }
        // 4.1.2. If the item has an automatic grid position in both axes:
        else {
            // 4.1.2.1. Increment the column position of the auto-placement cursor until either this item's grid
            // area does not overlap any occupied grid cells, or the cursor's column position, plus the item's
            // column span, overflow the number of columns in the implicit grid, as determined earlier in this
            // algorithm.
            auto column_start = 0;
            auto column_span = 1;
            auto row_start = 0;
            auto row_span = 1;
            auto found_unoccupied_cell = false;
            for (int row_index = auto_placement_cursor_y; row_index < (int)occupation_grid.size(); row_index++) {
                for (int column_index = auto_placement_cursor_x; column_index < (int)occupation_grid[0].size(); column_index++) {
                    if (!occupation_grid[row_index][column_index]) {
                        found_unoccupied_cell = true;
                        column_start = column_index;
                        row_start = row_index;
                        goto finish;
                    }
                    auto_placement_cursor_x = 0;
                }
                auto_placement_cursor_x = 0;
                auto_placement_cursor_y++;
            }
        finish:

            // 4.1.2.2. If a non-overlapping position was found in the previous step, set the item's row-start
            // and column-start lines to the cursor's position. Otherwise, increment the auto-placement cursor's
            // row position (creating new rows in the implicit grid as necessary), set its column position to the
            // start-most column line in the implicit grid, and return to the previous step.
            if (!found_unoccupied_cell) {
                row_start = (int)occupation_grid.size();
                maybe_add_row_to_occupation_grid((int)occupation_grid.size() + 1, occupation_grid);
            }

            set_occupied_cells(row_start, row_start + row_span, column_start, column_start + column_span, occupation_grid);
            positioned_boxes.append({ child_box, row_start, row_span, column_start, column_span });
        }
        boxes_to_place.remove(i);
        i--;

        // FIXME: 4.2. For dense packing:
    }

    // https://drafts.csswg.org/css-grid/#overview-sizing
    // 2.3. Sizing the Grid
    // Once the grid items have been placed, the sizes of the grid tracks (rows and columns) are
    // calculated, accounting for the sizes of their contents and/or available space as specified in
    // the grid definition.

    // https://drafts.csswg.org/css-grid/#layout-algorithm
    // 12. Grid Sizing
    // This section defines the grid sizing algorithm, which determines the size of all grid tracks and,
    // by extension, the entire grid.

    // Each track has specified minimum and maximum sizing functions (which may be the same). Each
    // sizing function is either:

    // - A fixed sizing function (<length> or resolvable <percentage>).
    // - An intrinsic sizing function (min-content, max-content, auto, fit-content()).
    // - A flexible sizing function (<flex>).

    // The grid sizing algorithm defines how to resolve these sizing constraints into used track sizes.

    struct GridTrack {
        CSS::GridTrackSize min_track_sizing_function;
        CSS::GridTrackSize max_track_sizing_function;
        float base_size { 0 };
        float growth_limit { 0 };
    };
    Vector<GridTrack> grid_rows;
    Vector<GridTrack> grid_columns;

    for (auto& column_size : box.computed_values().grid_template_columns())
        grid_columns.append({ column_size, column_size });
    for (auto& row_size : box.computed_values().grid_template_rows())
        grid_rows.append({ row_size, row_size });

    for (int column_index = grid_columns.size(); column_index < static_cast<int>(occupation_grid[0].size()); column_index++)
        grid_columns.append({ CSS::GridTrackSize::make_auto(), CSS::GridTrackSize::make_auto() });
    for (int row_index = grid_rows.size(); row_index < static_cast<int>(occupation_grid.size()); row_index++)
        grid_rows.append({ CSS::GridTrackSize::make_auto(), CSS::GridTrackSize::make_auto() });

    // https://drafts.csswg.org/css-grid/#algo-overview
    // 12.1. Grid Sizing Algorithm
    // FIXME: Deals with subgrids, min-content, and justify-content.. not implemented yet

    // https://drafts.csswg.org/css-grid/#algo-track-sizing
    // 12.3. Track Sizing Algorithm

    // The remainder of this section is the track sizing algorithm, which calculates from the min and
    // max track sizing functions the used track size. Each track has a base size, a <length> which
    // grows throughout the algorithm and which will eventually be the track’s final size, and a growth
    // limit, a <length> which provides a desired maximum size for the base size. There are 5 steps:

    // 1. Initialize Track Sizes
    // 2. Resolve Intrinsic Track Sizes
    // 3. Maximize Tracks
    // 4. Expand Flexible Tracks
    // 5. [[#algo-stretch|Expand Stretched auto Tracks]]

    // https://drafts.csswg.org/css-grid/#algo-init
    // 12.4. Initialize Track Sizes

    // Initialize each track’s base size and growth limit.
    for (auto& grid_column : grid_columns) {
        // For each track, if the track’s min track sizing function is:
        switch (grid_column.min_track_sizing_function.type()) {
        // - A fixed sizing function
        // Resolve to an absolute length and use that size as the track’s initial base size.
        // Indefinite lengths cannot occur, as they’re treated as auto.
        case CSS::GridTrackSize::Type::Length:
            if (!grid_column.min_track_sizing_function.length().is_auto())
                grid_column.base_size = grid_column.min_track_sizing_function.length().to_px(box);
            break;
        case CSS::GridTrackSize::Type::Percentage:
            grid_column.base_size = grid_column.min_track_sizing_function.percentage().as_fraction() * box_state.content_width();
            break;
        // - An intrinsic sizing function
        // Use an initial base size of zero.
        case CSS::GridTrackSize::Type::FlexibleLength:
            break;
        default:
            VERIFY_NOT_REACHED();
        }

        // For each track, if the track’s max track sizing function is:
        switch (grid_column.max_track_sizing_function.type()) {
        // - A fixed sizing function
        // Resolve to an absolute length and use that size as the track’s initial growth limit.
        case CSS::GridTrackSize::Type::Length:
            if (!grid_column.max_track_sizing_function.length().is_auto())
                grid_column.growth_limit = grid_column.max_track_sizing_function.length().to_px(box);
            else
                // - An intrinsic sizing function
                // Use an initial growth limit of infinity.
                grid_column.growth_limit = -1;
            break;
        case CSS::GridTrackSize::Type::Percentage:
            grid_column.growth_limit = grid_column.max_track_sizing_function.percentage().as_fraction() * box_state.content_width();
            break;
        // - A flexible sizing function
        // Use an initial growth limit of infinity.
        case CSS::GridTrackSize::Type::FlexibleLength:
            grid_column.growth_limit = -1;
            break;
        default:
            VERIFY_NOT_REACHED();
        }
    }

    // Initialize each track’s base size and growth limit.
    for (auto& grid_row : grid_rows) {
        // For each track, if the track’s min track sizing function is:
        switch (grid_row.min_track_sizing_function.type()) {
        // - A fixed sizing function
        // Resolve to an absolute length and use that size as the track’s initial base size.
        // Indefinite lengths cannot occur, as they’re treated as auto.
        case CSS::GridTrackSize::Type::Length:
            if (!grid_row.min_track_sizing_function.length().is_auto())
                grid_row.base_size = grid_row.min_track_sizing_function.length().to_px(box);
            break;
        case CSS::GridTrackSize::Type::Percentage:
            grid_row.base_size = grid_row.min_track_sizing_function.percentage().as_fraction() * box_state.content_height();
            break;
        // - An intrinsic sizing function
        // Use an initial base size of zero.
        case CSS::GridTrackSize::Type::FlexibleLength:
            break;
        default:
            VERIFY_NOT_REACHED();
        }

        // For each track, if the track’s max track sizing function is:
        switch (grid_row.max_track_sizing_function.type()) {
        // - A fixed sizing function
        // Resolve to an absolute length and use that size as the track’s initial growth limit.
        case CSS::GridTrackSize::Type::Length:
            if (!grid_row.max_track_sizing_function.length().is_auto())
                grid_row.growth_limit = grid_row.max_track_sizing_function.length().to_px(box);
            else
                // - An intrinsic sizing function
                // Use an initial growth limit of infinity.
                grid_row.growth_limit = -1;
            break;
        case CSS::GridTrackSize::Type::Percentage:
            grid_row.growth_limit = grid_row.max_track_sizing_function.percentage().as_fraction() * box_state.content_height();
            break;
        // - A flexible sizing function
        // Use an initial growth limit of infinity.
        case CSS::GridTrackSize::Type::FlexibleLength:
            grid_row.growth_limit = -1;
            break;
        default:
            VERIFY_NOT_REACHED();
        }
    }

    // FIXME: In all cases, if the growth limit is less than the base size, increase the growth limit to match
    // the base size.
}

}
