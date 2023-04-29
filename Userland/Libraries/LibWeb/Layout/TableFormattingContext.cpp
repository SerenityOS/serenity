/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Node.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/Layout/Box.h>
#include <LibWeb/Layout/InlineFormattingContext.h>
#include <LibWeb/Layout/TableBox.h>
#include <LibWeb/Layout/TableCellBox.h>
#include <LibWeb/Layout/TableFormattingContext.h>
#include <LibWeb/Layout/TableRowBox.h>
#include <LibWeb/Layout/TableRowGroupBox.h>

struct GridPosition {
    size_t x;
    size_t y;
};

inline bool operator==(GridPosition const& a, GridPosition const& b)
{
    return a.x == b.x && a.y == b.y;
}

namespace AK {
template<>
struct Traits<GridPosition> : public GenericTraits<GridPosition> {
    static unsigned hash(GridPosition const& key)
    {
        return pair_int_hash(key.x, key.y);
    }
};
}

namespace Web::Layout {

TableFormattingContext::TableFormattingContext(LayoutState& state, TableBox const& root, FormattingContext* parent)
    : FormattingContext(Type::Table, state, root, parent)
{
}

TableFormattingContext::~TableFormattingContext() = default;

void TableFormattingContext::calculate_row_column_grid(Box const& box)
{
    // Implements https://html.spec.whatwg.org/multipage/tables.html#forming-a-table
    HashMap<GridPosition, bool> grid;

    size_t x_width = 0, y_height = 0;
    size_t x_current = 0, y_current = 0;

    auto process_row = [&](auto& row) {
        if (y_height == y_current)
            y_height++;

        x_current = 0;
        while (x_current < x_width && grid.contains(GridPosition { x_current, y_current }))
            x_current++;

        for (auto* child = row.first_child(); child; child = child->next_sibling()) {
            if (is<TableCellBox>(*child)) {
                Box* box = static_cast<Box*>(child);
                if (x_current == x_width)
                    x_width++;

                const size_t colspan = static_cast<TableCellBox*>(child)->colspan();
                const size_t rowspan = static_cast<TableCellBox*>(child)->rowspan();

                if (x_width < x_current + colspan)
                    x_width = x_current + colspan;
                if (y_height < y_current + rowspan)
                    y_height = y_current + rowspan;

                for (size_t y = y_current; y < y_current + rowspan; y++)
                    for (size_t x = x_current; x < x_current + colspan; x++)
                        grid.set(GridPosition { x, y }, true);
                m_cells.append(Cell { *box, x_current, y_current, colspan, rowspan });

                x_current += colspan;
            }
        }

        m_rows.append(Row { row });
        y_current++;
    };

    box.template for_each_child_of_type<TableRowGroupBox>([&](auto& row_group_box) {
        row_group_box.template for_each_child_of_type<TableRowBox>([&](auto& row) {
            process_row(row);
            return IterationDecision::Continue;
        });
    });

    box.template for_each_child_of_type<TableRowBox>([&](auto& row) {
        process_row(row);
        return IterationDecision::Continue;
    });

    m_columns.resize(x_width);
}

void TableFormattingContext::compute_table_measures()
{
    size_t max_cell_column_span = 1;

    for (auto& cell : m_cells) {
        auto width_of_containing_block = m_state.get(*table_wrapper().containing_block()).content_width();
        auto width_of_containing_block_as_length = CSS::Length::make_px(width_of_containing_block);
        auto& computed_values = cell.box->computed_values();
        CSSPixels padding_left = computed_values.padding().left().resolved(cell.box, width_of_containing_block_as_length).to_px(cell.box);
        CSSPixels padding_right = computed_values.padding().right().resolved(cell.box, width_of_containing_block_as_length).to_px(cell.box);

        auto is_left_most_cell = cell.column_index == 0;
        auto is_right_most_cell = cell.column_index == m_columns.size() - 1;
        auto should_hide_borders = cell.box->computed_values().border_collapse() == CSS::BorderCollapse::Collapse;
        CSSPixels border_left = should_hide_borders && !is_left_most_cell ? 0 : computed_values.border_left().width;
        CSSPixels border_right = should_hide_borders && !is_right_most_cell ? 0 : computed_values.border_right().width;

        CSSPixels width = computed_values.width().resolved(cell.box, width_of_containing_block_as_length).to_px(cell.box);
        auto cell_intrinsic_offsets = padding_left + padding_right + border_left + border_right;
        auto min_content_width = calculate_min_content_width(cell.box);
        auto max_content_width = calculate_max_content_width(cell.box);

        CSSPixels min_width = min_content_width;
        if (!computed_values.min_width().is_auto())
            min_width = max(min_width, computed_values.min_width().resolved(cell.box, width_of_containing_block_as_length).to_px(cell.box));

        CSSPixels max_width = computed_values.width().is_auto() ? max_content_width.value() : width;
        if (!computed_values.max_width().is_none())
            max_width = min(max_width, computed_values.max_width().resolved(cell.box, width_of_containing_block_as_length).to_px(cell.box));

        auto computed_width = computed_values.width();
        if (computed_width.is_percentage()) {
            m_columns[cell.column_index].type = ColumnType::Percent;
            m_columns[cell.column_index].percentage_width = max(m_columns[cell.column_index].percentage_width, computed_width.percentage().value());
        } else if (computed_width.is_length()) {
            m_columns[cell.column_index].type = ColumnType::Pixel;
        }

        cell.min_width = min_width + cell_intrinsic_offsets;
        cell.max_width = max(max(width, min_width), max_width) + cell_intrinsic_offsets;

        max_cell_column_span = max(max_cell_column_span, cell.column_span);
    }

    for (auto& cell : m_cells) {
        if (cell.column_span == 1) {
            m_columns[cell.column_index].min_width = max(m_columns[cell.column_index].min_width, cell.min_width);
            m_columns[cell.column_index].max_width = max(m_columns[cell.column_index].max_width, cell.max_width);
        }
    }

    for (size_t current_column_span = 2; current_column_span < max_cell_column_span; current_column_span++) {
        // https://www.w3.org/TR/css-tables-3/#min-content-width-of-a-column-based-on-cells-of-span-up-to-n-n--1
        Vector<Vector<CSSPixels>> cell_min_contributions_by_column_index;
        cell_min_contributions_by_column_index.resize(m_columns.size());
        // https://www.w3.org/TR/css-tables-3/#max-content-width-of-a-column-based-on-cells-of-span-up-to-n-n--1
        Vector<Vector<CSSPixels>> cell_max_contributions_by_column_index;
        cell_max_contributions_by_column_index.resize(m_columns.size());
        for (auto& cell : m_cells) {
            if (cell.column_span == current_column_span) {
                // Define the baseline max-content width as the sum of the max-content widths based on cells of span up to N-1 of all columns that the cell spans.
                CSSPixels baseline_max_content_width = 0;
                auto cell_start_column_index = cell.column_index;
                auto cell_end_column_index = cell.column_index + cell.column_span;
                for (auto column_index = cell_start_column_index; column_index < cell_end_column_index; column_index++) {
                    baseline_max_content_width += m_columns[column_index].max_width;
                }

                // The contribution of the cell is the sum of:
                // the min-content width of the column based on cells of span up to N-1
                auto cell_min_contribution = m_columns[cell.column_index].min_width;
                // and the product of:
                // - the ratio of the max-content width based on cells of span up to N-1 of the column to the baseline max-content width
                // - the outer min-content width of the cell minus the baseline max-content width and baseline border spacing, or 0 if this is negative
                cell_min_contribution += (m_columns[cell.column_index].max_width / baseline_max_content_width) * max(CSSPixels(0), cell.min_width - baseline_max_content_width);

                // The contribution of the cell is the sum of:
                // the max-content width of the column based on cells of span up to N-1
                auto cell_max_contribution = m_columns[cell.column_index].max_width;
                // and the product of:
                // - the ratio of the max-content width based on cells of span up to N-1 of the column to the baseline max-content width
                // - the outer max-content width of the cell minus the baseline max-content width and the baseline border spacing, or 0 if this is negative
                cell_max_contribution += (m_columns[cell.column_index].max_width / baseline_max_content_width) * max(CSSPixels(0), cell.max_width - baseline_max_content_width);

                cell_min_contributions_by_column_index[cell.column_index].append(cell_min_contribution);
                cell_max_contributions_by_column_index[cell.column_index].append(cell_max_contribution);
            }
        }

        for (size_t column_index = 0; column_index < m_columns.size(); column_index++) {
            // min-content width of a column based on cells of span up to N (N > 1) is
            // the largest of the min-content width of the column based on cells of span up to N-1 and the contributions of the cells in the column whose colSpan is N
            for (auto min_contribution : cell_min_contributions_by_column_index[column_index])
                m_columns[column_index].min_width = max(m_columns[column_index].min_width, min_contribution);

            // max-content width of a column based on cells of span up to N (N > 1) is
            // the largest of the max-content width based on cells of span up to N-1 and the contributions of the cells in the column whose colSpan is N
            for (auto max_contribution : cell_max_contributions_by_column_index[column_index])
                m_columns[column_index].max_width = max(m_columns[column_index].max_width, max_contribution);
        }
    }
}

void TableFormattingContext::compute_table_width()
{
    // https://drafts.csswg.org/css-tables-3/#computing-the-table-width

    auto& table_box_state = m_state.get_mutable(table_box());

    auto& computed_values = table_box().computed_values();

    // Percentages on 'width' and 'height' on the table are relative to the table wrapper box's containing block,
    // not the table wrapper box itself.
    CSSPixels width_of_table_containing_block = m_state.get(*table_wrapper().containing_block()).content_width();

    // The row/column-grid width minimum (GRIDMIN) width is the sum of the min-content width
    // of all the columns plus cell spacing or borders.
    CSSPixels grid_min = 0.0f;
    for (auto& column : m_columns) {
        grid_min += column.min_width;
    }

    // The row/column-grid width maximum (GRIDMAX) width is the sum of the max-content width
    // of all the columns plus cell spacing or borders.
    CSSPixels grid_max = 0.0f;
    for (auto& column : m_columns) {
        grid_max += column.max_width;
    }

    // The used min-width of a table is the greater of the resolved min-width, CAPMIN, and GRIDMIN.
    auto used_min_width = grid_min;
    if (!computed_values.min_width().is_auto()) {
        used_min_width = max(used_min_width, computed_values.min_width().resolved(table_box(), CSS::Length::make_px(width_of_table_containing_block)).to_px(table_box()));
    }

    CSSPixels used_width;
    if (computed_values.width().is_auto()) {
        // If the table-root has 'width: auto', the used width is the greater of
        // min(GRIDMAX, the table’s containing block width), the used min-width of the table.
        used_width = max(min(grid_max, width_of_table_containing_block), used_min_width);
    } else {
        // If the table-root’s width property has a computed value (resolving to
        // resolved-table-width) other than auto, the used width is the greater
        // of resolved-table-width, and the used min-width of the table.
        CSSPixels resolved_table_width = computed_values.width().resolved(table_box(), CSS::Length::make_px(width_of_table_containing_block)).to_px(table_box());
        used_width = max(resolved_table_width, used_min_width);
        if (!computed_values.max_width().is_none())
            used_width = min(used_width, computed_values.max_width().resolved(table_box(), CSS::Length::make_px(width_of_table_containing_block)).to_px(table_box()));
    }

    // The assignable table width is the used width of the table minus the total horizontal border spacing (if any).
    // This is the width that we will be able to allocate to the columns.
    table_box_state.set_content_width(used_width - table_box_state.border_left - table_box_state.border_right);
}

void TableFormattingContext::distribute_width_to_columns()
{
    // Implements https://www.w3.org/TR/css-tables-3/#width-distribution-algorithm

    CSSPixels available_width = m_state.get(table_box()).content_width();

    auto columns_total_used_width = [&]() {
        CSSPixels total_used_width = 0;
        for (auto& column : m_columns) {
            total_used_width += column.used_width;
        }
        return total_used_width;
    };

    auto column_preferred_width = [&](Column& column) {
        switch (column.type) {
        case ColumnType::Percent: {
            return max(column.min_width, column.percentage_width / 100 * available_width);
        }
        case ColumnType::Pixel:
        case ColumnType::Auto: {
            return column.max_width;
        }
        default: {
            VERIFY_NOT_REACHED();
        }
        }
    };

    auto expand_columns_to_fill_available_width = [&](ColumnType column_type) {
        CSSPixels remaining_available_width = available_width;
        CSSPixels total_preferred_width_increment = 0;
        for (auto& column : m_columns) {
            remaining_available_width -= column.used_width;
            if (column.type == column_type) {
                total_preferred_width_increment += column_preferred_width(column) - column.min_width;
            }
        }

        if (total_preferred_width_increment == 0)
            return;

        for (auto& column : m_columns) {
            if (column.type == column_type) {
                CSSPixels preferred_width_increment = column_preferred_width(column) - column.min_width;
                column.used_width += preferred_width_increment * remaining_available_width / total_preferred_width_increment;
            }
        }
    };

    auto shrink_columns_to_fit_available_width = [&](ColumnType column_type) {
        for (auto& column : m_columns) {
            if (column.type == column_type)
                column.used_width = column.min_width;
        }

        expand_columns_to_fill_available_width(column_type);
    };

    for (auto& column : m_columns) {
        column.used_width = column.min_width;
    }

    for (auto& column : m_columns) {
        if (column.type == ColumnType::Percent) {
            column.used_width = max(column.min_width, column.percentage_width / 100 * available_width);
        }
    }

    if (columns_total_used_width() > available_width) {
        shrink_columns_to_fit_available_width(ColumnType::Percent);
        return;
    }

    for (auto& column : m_columns) {
        if (column.type == ColumnType::Pixel) {
            column.used_width = column.max_width;
        }
    }

    if (columns_total_used_width() > available_width) {
        shrink_columns_to_fit_available_width(ColumnType::Pixel);
        return;
    }

    if (columns_total_used_width() < available_width) {
        expand_columns_to_fill_available_width(ColumnType::Auto);
    }

    if (columns_total_used_width() < available_width) {
        expand_columns_to_fill_available_width(ColumnType::Pixel);
    }

    if (columns_total_used_width() < available_width) {
        expand_columns_to_fill_available_width(ColumnType::Percent);
    }

    if (columns_total_used_width() < available_width) {
        // NOTE: if all columns got their max width and there is still width to distribute left
        // it should be assigned to columns proportionally to their max width
        CSSPixels grid_max = 0.0f;
        for (auto& column : m_columns) {
            grid_max += column.max_width;
        }

        auto width_to_distribute = available_width - columns_total_used_width();
        if (grid_max == 0) {
            // If total max width of columns is zero then divide distributable width equally among them
            auto column_width = width_to_distribute / m_columns.size();
            for (auto& column : m_columns) {
                column.used_width = column_width;
            }
        } else {
            // Distribute width to columns proportionally to their max width
            for (auto& column : m_columns) {
                column.used_width += width_to_distribute * column.max_width / grid_max;
            }
        }
    }
}

void TableFormattingContext::determine_intrisic_size_of_table_container(AvailableSpace const& available_space)
{
    auto& table_box_state = m_state.get_mutable(table_box());

    if (available_space.width.is_min_content()) {
        // The min-content width of a table is the width required to fit all of its columns min-content widths and its undistributable spaces.
        CSSPixels grid_min = 0.0f;
        for (auto& column : m_columns)
            grid_min += column.min_width;
        table_box_state.set_content_width(grid_min);
    }

    if (available_space.width.is_max_content()) {
        // The max-content width of a table is the width required to fit all of its columns max-content widths and its undistributable spaces.
        CSSPixels grid_max = 0.0f;
        for (auto& column : m_columns)
            grid_max += column.max_width;
        table_box_state.set_content_width(grid_max);
    }
}

void TableFormattingContext::compute_table_height(LayoutMode layout_mode)
{
    // First pass of row height calculation:
    for (auto& row : m_rows) {
        auto row_computed_height = row.box->computed_values().height();
        if (row_computed_height.is_length()) {
            auto height_of_containing_block = m_state.get(*row.box->containing_block()).content_height();
            auto height_of_containing_block_as_length = CSS::Length::make_px(height_of_containing_block);
            auto row_used_height = row_computed_height.resolved(row.box, height_of_containing_block_as_length).to_px(row.box);
            row.base_height = max(row.base_height, row_used_height);
        }
    }

    // First pass of cells layout:
    for (auto& cell : m_cells) {
        auto& row = m_rows[cell.row_index];
        auto& cell_state = m_state.get_mutable(cell.box);

        CSSPixels span_width = 0;
        for (size_t i = 0; i < cell.column_span; ++i)
            span_width += m_columns[cell.column_index + i].used_width;

        auto width_of_containing_block = m_state.get(*cell.box->containing_block()).content_width();
        auto width_of_containing_block_as_length = CSS::Length::make_px(width_of_containing_block);
        auto height_of_containing_block = m_state.get(*cell.box->containing_block()).content_height();
        auto height_of_containing_block_as_length = CSS::Length::make_px(height_of_containing_block);

        cell_state.padding_top = cell.box->computed_values().padding().top().resolved(cell.box, width_of_containing_block_as_length).to_px(cell.box);
        cell_state.padding_bottom = cell.box->computed_values().padding().bottom().resolved(cell.box, width_of_containing_block_as_length).to_px(cell.box);
        cell_state.padding_left = cell.box->computed_values().padding().left().resolved(cell.box, width_of_containing_block_as_length).to_px(cell.box);
        cell_state.padding_right = cell.box->computed_values().padding().right().resolved(cell.box, width_of_containing_block_as_length).to_px(cell.box);

        auto is_top_most_cell = cell.row_index == 0;
        auto is_left_most_cell = cell.column_index == 0;
        auto is_right_most_cell = cell.column_index == m_columns.size() - 1;
        auto is_bottom_most_cell = cell.row_index == m_rows.size() - 1;
        auto should_hide_borders = cell.box->computed_values().border_collapse() == CSS::BorderCollapse::Collapse;

        cell_state.border_top = (should_hide_borders && is_top_most_cell) ? 0 : cell.box->computed_values().border_top().width;
        cell_state.border_bottom = (should_hide_borders && is_bottom_most_cell) ? 0 : cell.box->computed_values().border_bottom().width;
        cell_state.border_left = (should_hide_borders && is_left_most_cell) ? 0 : cell.box->computed_values().border_left().width;
        cell_state.border_right = (should_hide_borders && is_right_most_cell) ? 0 : cell.box->computed_values().border_right().width;

        auto cell_computed_height = cell.box->computed_values().height();
        if (cell_computed_height.is_length()) {
            auto cell_used_height = cell_computed_height.resolved(cell.box, height_of_containing_block_as_length).to_px(cell.box);
            cell_state.set_content_height(cell_used_height - cell_state.border_box_top() - cell_state.border_box_bottom());

            row.base_height = max(row.base_height, cell_used_height);
        }

        cell_state.set_content_width((span_width - cell_state.border_box_left() - cell_state.border_box_right()));
        if (auto independent_formatting_context = layout_inside(cell.box, layout_mode, cell_state.available_inner_space_or_constraints_from(*m_available_space))) {
            cell_state.set_content_height(independent_formatting_context->automatic_content_height());
            independent_formatting_context->parent_context_did_dimension_child_root_box();
        }

        cell.baseline = box_baseline(m_state, cell.box);

        row.base_height = max(row.base_height, cell_state.border_box_height());
        row.baseline = max(row.baseline, cell.baseline);
    }

    CSSPixels sum_rows_height = 0;
    for (auto& row : m_rows) {
        sum_rows_height += row.base_height;
    }

    // The height of a table is the sum of the row heights plus any cell spacing or borders.
    m_table_height = sum_rows_height;

    if (!table_box().computed_values().height().is_auto()) {
        // If the table has a height property with a value other than auto, it is treated as a minimum height for the
        // table grid, and will eventually be distributed to the height of the rows if their collective minimum height
        // ends up smaller than this number.
        CSSPixels height_of_table_containing_block = m_state.get(*table_wrapper().containing_block()).content_height();
        auto specified_table_height = table_box().computed_values().height().resolved(table_box(), CSS::Length::make_px(height_of_table_containing_block)).to_px(table_box());
        if (m_table_height < specified_table_height) {
            m_table_height = specified_table_height;
        }
    }

    for (auto& row : m_rows) {
        // Reference size is the largest of
        // - its initial base height and
        // - its new base height (the one evaluated during the second layout pass, where percentages used in
        //   rowgroups/rows/cells' specified heights were resolved according to the table height, instead of
        //   being ignored as 0px).

        // Assign reference size to base size. Later reference size might change to largee value during
        // second pass of rows layout.
        row.reference_height = row.base_height;
    }

    // Second pass of rows height calculation:
    // At this point percentage row height can be resolved because final table height is calculated.
    for (auto& row : m_rows) {
        auto row_computed_height = row.box->computed_values().height();
        if (row_computed_height.is_percentage()) {
            auto row_used_height = row_computed_height.resolved(row.box, CSS::Length::make_px(m_table_height)).to_px(row.box);
            row.reference_height = max(row.reference_height, row_used_height);
        } else {
            continue;
        }
    }

    // Second pass cells layout:
    // At this point percantage cell height can be resolved because final table heigh is calculated.
    for (auto& cell : m_cells) {
        auto& row = m_rows[cell.row_index];
        auto& cell_state = m_state.get_mutable(cell.box);

        CSSPixels span_width = 0;
        for (size_t i = 0; i < cell.column_span; ++i)
            span_width += m_columns[cell.column_index + i].used_width;

        auto height_of_containing_block_as_length = CSS::Length::make_px(m_table_height);

        auto cell_computed_height = cell.box->computed_values().height();
        if (cell_computed_height.is_percentage()) {
            auto cell_used_height = cell_computed_height.resolved(cell.box, height_of_containing_block_as_length).to_px(cell.box);
            cell_state.set_content_height(cell_used_height - cell_state.border_box_top() - cell_state.border_box_bottom());

            row.reference_height = max(row.reference_height, cell_used_height);
        } else {
            continue;
        }

        cell_state.set_content_width((span_width - cell_state.border_box_left() - cell_state.border_box_right()));
        if (auto independent_formatting_context = layout_inside(cell.box, layout_mode, cell_state.available_inner_space_or_constraints_from(*m_available_space))) {
            independent_formatting_context->parent_context_did_dimension_child_root_box();
        }

        cell.baseline = box_baseline(m_state, cell.box);

        row.reference_height = max(row.reference_height, cell_state.border_box_height());
        row.baseline = max(row.baseline, cell.baseline);
    }
}

void TableFormattingContext::distribute_height_to_rows()
{
    CSSPixels sum_reference_height = 0;
    for (auto& row : m_rows) {
        sum_reference_height += row.reference_height;
    }

    if (sum_reference_height == 0)
        return;

    Vector<Row&> rows_with_auto_height;
    for (auto& row : m_rows) {
        if (row.box->computed_values().height().is_auto()) {
            rows_with_auto_height.append(row);
        }
    }

    if (m_table_height <= sum_reference_height) {
        // If the table height is equal or smaller than sum of reference sizes, the final height assigned to each row
        // will be the weighted mean of the base and the reference size that yields the correct total height.

        for (auto& row : m_rows) {
            auto weight = row.reference_height / sum_reference_height;
            auto final_height = m_table_height * weight;
            row.final_height = final_height;
        }
    } else if (rows_with_auto_height.size() > 0) {
        // Else, if the table owns any “auto-height” row (a row whose size is only determined by its content size and
        // none of the specified heights), each non-auto-height row receives its reference height and auto-height rows
        // receive their reference size plus some increment which is equal to the height missing to amount to the
        // specified table height divided by the amount of such rows.

        for (auto& row : m_rows) {
            row.final_height = row.reference_height;
        }

        auto auto_height_rows_increment = (m_table_height - sum_reference_height) / rows_with_auto_height.size();
        for (auto& row : rows_with_auto_height) {
            row.final_height += auto_height_rows_increment;
        }
    } else {
        // Else, all rows receive their reference size plus some increment which is equal to the height missing to
        // amount to the specified table height divided by the amount of rows.

        auto increment = (m_table_height - sum_reference_height) / m_rows.size();
        for (auto& row : m_rows) {
            row.final_height = row.reference_height + increment;
        }
    }
}

void TableFormattingContext::position_row_boxes(CSSPixels& total_content_height)
{
    auto const& table_state = m_state.get(table_box());

    CSSPixels row_top_offset = table_state.border_top + table_state.padding_top;
    CSSPixels row_left_offset = table_state.border_left + table_state.padding_left;
    for (size_t y = 0; y < m_rows.size(); y++) {
        auto& row = m_rows[y];
        auto& row_state = m_state.get_mutable(row.box);
        CSSPixels row_width = 0.0f;
        for (auto& column : m_columns) {
            row_width += column.used_width;
        }

        row_state.set_content_height(row.final_height);
        row_state.set_content_width(row_width);
        row_state.set_content_x(row_left_offset);
        row_state.set_content_y(row_top_offset);
        row_top_offset += row_state.content_height();
    }

    CSSPixels row_group_top_offset = table_state.border_top + table_state.padding_top;
    CSSPixels row_group_left_offset = table_state.border_left + table_state.padding_left;
    table_box().for_each_child_of_type<TableRowGroupBox>([&](auto& row_group_box) {
        CSSPixels row_group_height = 0.0f;
        CSSPixels row_group_width = 0.0f;

        auto& row_group_box_state = m_state.get_mutable(row_group_box);
        row_group_box_state.set_content_x(row_group_left_offset);
        row_group_box_state.set_content_y(row_group_top_offset);

        row_group_box.template for_each_child_of_type<TableRowBox>([&](auto& row) {
            auto const& row_state = m_state.get(row);
            row_group_height += row_state.border_box_height();
            row_group_width = max(row_group_width, row_state.border_box_width());
        });

        row_group_box_state.set_content_height(row_group_height);
        row_group_box_state.set_content_width(row_group_width);

        row_group_top_offset += row_group_height;
    });

    total_content_height = max(row_top_offset, row_group_top_offset) - table_state.border_top - table_state.padding_top;
}

void TableFormattingContext::position_cell_boxes()
{
    CSSPixels left_column_offset = 0;
    for (auto& column : m_columns) {
        column.left_offset = left_column_offset;
        left_column_offset += column.used_width;
    }

    for (auto& cell : m_cells) {
        auto& cell_state = m_state.get_mutable(cell.box);
        auto& row_state = m_state.get(m_rows[cell.row_index].box);
        CSSPixels const cell_border_box_height = cell_state.content_height() + cell_state.border_box_top() + cell_state.border_box_bottom();
        CSSPixels const row_content_height = row_state.content_height();
        auto const& vertical_align = cell.box->computed_values().vertical_align();
        if (vertical_align.has<CSS::VerticalAlign>()) {
            switch (vertical_align.get<CSS::VerticalAlign>()) {
            case CSS::VerticalAlign::Middle: {
                cell_state.padding_top += (row_content_height - cell_border_box_height) / 2;
                cell_state.padding_bottom += (row_content_height - cell_border_box_height) / 2;
                break;
            }
            // FIXME: implement actual 'top' and 'bottom' support instead of fall back to 'baseline'
            case CSS::VerticalAlign::Top:
            case CSS::VerticalAlign::Bottom:
            case CSS::VerticalAlign::Baseline: {
                cell_state.padding_top += m_rows[cell.row_index].baseline - cell.baseline;
                cell_state.padding_bottom += row_content_height - cell_border_box_height;
                break;
            }
            default:
                VERIFY_NOT_REACHED();
            }
        }

        cell_state.offset = row_state.offset.translated(cell_state.border_box_left() + m_columns[cell.column_index].left_offset, cell_state.border_box_top());
    }
}

void TableFormattingContext::run(Box const& box, LayoutMode layout_mode, AvailableSpace const& available_space)
{
    m_available_space = available_space;

    CSSPixels total_content_height = 0;

    // Determine the number of rows/columns the table requires.
    calculate_row_column_grid(box);

    // Compute the minimum width of each column.
    compute_table_measures();

    if (available_space.width.is_intrinsic_sizing_constraint() && !available_space.height.is_intrinsic_sizing_constraint()) {
        determine_intrisic_size_of_table_container(available_space);
        return;
    }

    // Compute the width of the table.
    compute_table_width();

    // Distribute the width of the table among columns.
    distribute_width_to_columns();

    compute_table_height(layout_mode);

    distribute_height_to_rows();

    position_row_boxes(total_content_height);
    position_cell_boxes();

    m_state.get_mutable(table_box()).set_content_height(total_content_height);

    // FIXME: This is a total hack, we should respect the 'height' property.
    m_automatic_content_height = total_content_height;
}

CSSPixels TableFormattingContext::automatic_content_width() const
{
    return greatest_child_width(context_box());
}

CSSPixels TableFormattingContext::automatic_content_height() const
{
    return m_automatic_content_height;
}

}
