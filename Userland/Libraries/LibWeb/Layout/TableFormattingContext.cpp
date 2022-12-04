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

TableFormattingContext::TableFormattingContext(LayoutState& state, BlockContainer const& block_container, FormattingContext* parent)
    : BlockFormattingContext(state, block_container, parent)
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

    m_columns.resize(x_width);
}

void TableFormattingContext::compute_table_measures()
{
    for (auto& cell : m_cells) {
        auto width_of_containing_block = m_state.get(*cell.box.containing_block()).content_width();
        auto width_of_containing_block_as_length = CSS::Length::make_px(width_of_containing_block);
        float padding_left = cell.box.computed_values().padding().left().resolved(cell.box, width_of_containing_block_as_length).to_px(cell.box);
        float padding_right = cell.box.computed_values().padding().right().resolved(cell.box, width_of_containing_block_as_length).to_px(cell.box);
        float border_left = cell.box.computed_values().border_left().width;
        float border_right = cell.box.computed_values().border_right().width;

        auto min_width = calculate_min_content_width(cell.box) + padding_left + padding_right + border_left + border_right;
        auto max_width = calculate_max_content_width(cell.box) + padding_left + padding_right + border_left + border_right;
        m_columns[cell.column_index].min_width = max(m_columns[cell.column_index].min_width, min_width);
        m_columns[cell.column_index].max_width = max(m_columns[cell.column_index].max_width, max_width);
    }

    for (auto& column : m_columns) {
        column.used_width = column.min_width;
    }
}

void TableFormattingContext::compute_table_width(float& extra_width)
{
    auto const& table_box = context_box();
    auto& table_box_state = m_state.get_mutable(table_box);

    auto& computed_values = table_box.computed_values();

    float width_of_table_containing_block = m_state.get(*table_box.containing_block()).content_width();

    // The row/column-grid width minimum (GRIDMIN) width is the sum of the min-content width
    // of all the columns plus cell spacing or borders.
    float grid_min = 0.0f;
    for (auto& column : m_columns) {
        grid_min += column.min_width;
    }

    // The row/column-grid width maximum (GRIDMAX) width is the sum of the max-content width
    // of all the columns plus cell spacing or borders.
    float grid_max = 0.0f;
    for (auto& column : m_columns) {
        grid_max += column.max_width;
    }

    // The used min-width of a table is the greater of the resolved min-width, CAPMIN, and GRIDMIN.
    float used_min_width = grid_min;
    if (!computed_values.min_width().is_auto()) {
        used_min_width = max(used_min_width, computed_values.min_width().resolved(table_box, CSS::Length::make_px(width_of_table_containing_block)).to_px(table_box));
    }

    float used_width;
    if (computed_values.width().is_auto()) {
        // If the table-root has 'width: auto', the used width is the greater of
        // min(GRIDMAX, the table’s containing block width), the used min-width of the table.
        used_width = max(min(grid_max, width_of_table_containing_block), used_min_width);
        table_box_state.set_content_width(used_width);
    } else {
        // If the table-root’s width property has a computed value (resolving to
        // resolved-table-width) other than auto, the used width is the greater
        // of resolved-table-width, and the used min-width of the table.
        float resolved_table_width = computed_values.width().resolved(table_box, CSS::Length::make_px(width_of_table_containing_block)).to_px(table_box);
        used_width = max(resolved_table_width, used_min_width);
        table_box_state.set_content_width(used_width);
    }

    if (used_width > grid_min) {
        extra_width = used_width - grid_min;
    }
}

void TableFormattingContext::distribute_width_to_columns(float extra_width)
{
    float grid_max = 0.0f;
    for (auto& column : m_columns)
        grid_max += column.max_width - column.min_width;

    for (auto& column : m_columns)
        column.used_width += ((column.max_width - column.min_width) / grid_max) * extra_width;
}

void TableFormattingContext::determine_intrisic_size_of_table_container(AvailableSpace const& available_space)
{
    auto const& table_box = context_box();
    auto& table_box_state = m_state.get_mutable(table_box);

    if (available_space.width.is_min_content()) {
        // The min-content width of a table is the width required to fit all of its columns min-content widths and its undistributable spaces.
        float grid_min = 0.0f;
        for (auto& column : m_columns)
            grid_min += column.min_width;
        table_box_state.set_content_width(grid_min);
    }

    if (available_space.width.is_max_content()) {
        // The max-content width of a table is the width required to fit all of its columns max-content widths and its undistributable spaces.
        float grid_max = 0.0f;
        for (auto& column : m_columns)
            grid_max += column.max_width;
        table_box_state.set_content_width(grid_max);
    }
}

void TableFormattingContext::run(Box const& box, LayoutMode, AvailableSpace const& available_space)
{
    float total_content_height = 0;

    // Determine the number of rows/columns the table requires.
    calculate_row_column_grid(box);

    // Compute the minimum width of each column.
    compute_table_measures();

    if (available_space.width.is_intrinsic_sizing_constraint()) {
        determine_intrisic_size_of_table_container(available_space);
        return;
    }

    // Compute the width of the table.
    float extra_width = 0;
    compute_table_width(extra_width);

    // Distribute the width of the table among columns.
    distribute_width_to_columns(extra_width);

    float left_column_offset = 0;
    for (auto& column : m_columns) {
        column.left_offset = left_column_offset;
        left_column_offset += column.used_width;
    }

    for (auto& cell : m_cells) {
        auto& cell_state = m_state.get_mutable(cell.box);

        float span_width = 0;
        for (size_t i = 0; i < cell.column_span; ++i)
            span_width += m_columns[cell.column_index + i].used_width;

        auto width_of_containing_block = m_state.get(*cell.box.containing_block()).content_width();
        auto width_of_containing_block_as_length = CSS::Length::make_px(width_of_containing_block);

        cell_state.padding_top = cell.box.computed_values().padding().top().resolved(cell.box, width_of_containing_block_as_length).to_px(cell.box);
        cell_state.padding_bottom = cell.box.computed_values().padding().bottom().resolved(cell.box, width_of_containing_block_as_length).to_px(cell.box);
        cell_state.padding_left = cell.box.computed_values().padding().left().resolved(cell.box, width_of_containing_block_as_length).to_px(cell.box);
        cell_state.padding_right = cell.box.computed_values().padding().right().resolved(cell.box, width_of_containing_block_as_length).to_px(cell.box);
        cell_state.border_top = cell.box.computed_values().border_top().width;
        cell_state.border_bottom = cell.box.computed_values().border_bottom().width;
        cell_state.border_left = cell.box.computed_values().border_left().width;
        cell_state.border_right = cell.box.computed_values().border_right().width;

        cell_state.set_content_width(span_width - cell_state.border_box_left() - cell_state.border_box_right());
        if (auto independent_formatting_context = layout_inside(cell.box, LayoutMode::Normal, cell_state.available_inner_space_or_constraints_from(available_space)))
            independent_formatting_context->parent_context_did_dimension_child_root_box();

        BlockFormattingContext::compute_height(cell.box, AvailableSpace(AvailableSize::make_indefinite(), AvailableSize::make_indefinite()));

        Row& row = m_rows[cell.row_index];
        row.used_width = max(row.used_width, cell_state.border_box_height());
    }

    for (size_t y = 0; y < m_rows.size(); y++) {
        auto& row = m_rows[y];
        auto& row_state = m_state.get_mutable(row.box);
        float row_width = 0.0f;
        for (auto& column : m_columns) {
            row_width += column.used_width;
        }

        row_state.set_content_height(row.used_width);
        row_state.set_content_width(row_width);
    }

    float row_group_top_offset = 0.0f;
    box.for_each_child_of_type<TableRowGroupBox>([&](auto& row_group_box) {
        float row_group_height = 0.0f;
        float row_group_width = 0.0f;

        auto& row_group_box_state = m_state.get_mutable(row_group_box);
        row_group_box_state.set_content_y(row_group_top_offset);

        float row_top_offset = 0.0f;
        row_group_box.template for_each_child_of_type<TableRowBox>([&](auto& row) {
            auto& row_state = m_state.get_mutable(row);
            row_state.set_content_y(row_top_offset);
            row_group_height += row_state.border_box_height();
            row_group_width = max(row_group_width, row_state.border_box_width());
            row_top_offset += row_state.border_box_height();
        });

        row_group_top_offset += row_top_offset;

        row_group_box_state.set_content_height(row_group_height);
        row_group_box_state.set_content_width(row_group_width);
    });

    for (auto& cell : m_cells) {
        auto& cell_state = m_state.get_mutable(cell.box);
        auto& row_state = m_state.get(m_rows[cell.row_index].box);
        cell_state.set_content_height(row_state.content_height() - cell_state.border_box_top() - cell_state.border_box_bottom());
        cell_state.offset = row_state.offset.translated(cell_state.border_box_left() + m_columns[cell.column_index].left_offset, cell_state.border_box_top());
    }

    m_state.get_mutable(context_box()).set_content_height(total_content_height);

    // FIXME: This is a total hack, we should respect the 'height' property.
    m_automatic_content_height = total_content_height;
}

float TableFormattingContext::automatic_content_height() const
{
    return m_automatic_content_height;
}

}
