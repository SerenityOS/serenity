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

TableFormattingContext::TableFormattingContext(LayoutState& state, BlockContainer const& root, FormattingContext* parent)
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
    for (auto& cell : m_cells) {
        auto width_of_containing_block = m_available_space->width.to_px();
        auto width_of_containing_block_as_length = CSS::Length::make_px(width_of_containing_block);
        auto& computed_values = cell.box.computed_values();
        CSSPixels padding_left = computed_values.padding().left().resolved(cell.box, width_of_containing_block_as_length).to_px(cell.box);
        CSSPixels padding_right = computed_values.padding().right().resolved(cell.box, width_of_containing_block_as_length).to_px(cell.box);

        auto is_left_most_cell = cell.column_index == 0;
        auto is_right_most_cell = cell.column_index == m_columns.size() - 1;
        auto should_hide_borders = cell.box.computed_values().border_collapse() == CSS::BorderCollapse::Collapse;
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

        auto cell_outer_min_content_width = min_width + cell_intrinsic_offsets;
        auto cell_outer_max_content_width = max(max(width, min_width), max_width) + cell_intrinsic_offsets;
        m_columns[cell.column_index].min_width = max(m_columns[cell.column_index].min_width, cell_outer_min_content_width);
        m_columns[cell.column_index].max_width = max(m_columns[cell.column_index].max_width, cell_outer_max_content_width);
    }
}

void TableFormattingContext::compute_table_width()
{
    auto const& table_box = context_box();
    auto& table_box_state = m_state.get_mutable(table_box);

    auto& computed_values = table_box.computed_values();

    CSSPixels width_of_table_containing_block = m_state.get(*table_box.containing_block()).content_width();

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
        used_min_width = max(used_min_width, computed_values.min_width().resolved(table_box, CSS::Length::make_px(width_of_table_containing_block)).to_px(table_box));
    }

    CSSPixels used_width;
    if (computed_values.width().is_auto()) {
        // If the table-root has 'width: auto', the used width is the greater of
        // min(GRIDMAX, the table’s containing block width), the used min-width of the table.
        used_width = max(min(grid_max, width_of_table_containing_block), used_min_width);
        table_box_state.set_content_width(used_width);
    } else {
        // If the table-root’s width property has a computed value (resolving to
        // resolved-table-width) other than auto, the used width is the greater
        // of resolved-table-width, and the used min-width of the table.
        CSSPixels resolved_table_width = computed_values.width().resolved(table_box, CSS::Length::make_px(width_of_table_containing_block)).to_px(table_box);
        used_width = max(resolved_table_width, used_min_width);
        table_box_state.set_content_width(used_width);
    }
}

void TableFormattingContext::distribute_width_to_columns()
{
    // Implements https://www.w3.org/TR/css-tables-3/#width-distribution-algorithm

    CSSPixels available_width = m_state.get(context_box()).content_width();

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
}

void TableFormattingContext::determine_intrisic_size_of_table_container(AvailableSpace const& available_space)
{
    auto const& table_box = context_box();
    auto& table_box_state = m_state.get_mutable(table_box);

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

void TableFormattingContext::run(Box const& box, LayoutMode, AvailableSpace const& available_space)
{
    m_available_space = available_space;

    CSSPixels total_content_height = 0;

    // Determine the number of rows/columns the table requires.
    calculate_row_column_grid(box);

    // Compute the minimum width of each column.
    compute_table_measures();

    if (available_space.width.is_intrinsic_sizing_constraint()) {
        determine_intrisic_size_of_table_container(available_space);
        return;
    }

    // Compute the width of the table.
    compute_table_width();

    // Distribute the width of the table among columns.
    distribute_width_to_columns();

    CSSPixels left_column_offset = 0;
    for (auto& column : m_columns) {
        column.left_offset = left_column_offset;
        left_column_offset += column.used_width;
    }

    for (auto& cell : m_cells) {
        auto& cell_state = m_state.get_mutable(cell.box);

        CSSPixels span_width = 0;
        for (size_t i = 0; i < cell.column_span; ++i)
            span_width += m_columns[cell.column_index + i].used_width;

        auto width_of_containing_block = m_state.get(*cell.box.containing_block()).content_width();
        auto width_of_containing_block_as_length = CSS::Length::make_px(width_of_containing_block);

        cell_state.padding_top = cell.box.computed_values().padding().top().resolved(cell.box, width_of_containing_block_as_length).to_px(cell.box);
        cell_state.padding_bottom = cell.box.computed_values().padding().bottom().resolved(cell.box, width_of_containing_block_as_length).to_px(cell.box);
        cell_state.padding_left = cell.box.computed_values().padding().left().resolved(cell.box, width_of_containing_block_as_length).to_px(cell.box);
        cell_state.padding_right = cell.box.computed_values().padding().right().resolved(cell.box, width_of_containing_block_as_length).to_px(cell.box);

        auto is_top_most_cell = cell.row_index == 0;
        auto is_left_most_cell = cell.column_index == 0;
        auto is_right_most_cell = cell.column_index == m_columns.size() - 1;
        auto is_bottom_most_cell = cell.row_index == m_rows.size() - 1;
        auto should_hide_borders = cell.box.computed_values().border_collapse() == CSS::BorderCollapse::Collapse;

        cell_state.border_top = (should_hide_borders && is_top_most_cell) ? 0 : cell.box.computed_values().border_top().width;
        cell_state.border_bottom = (should_hide_borders && is_bottom_most_cell) ? 0 : cell.box.computed_values().border_bottom().width;
        cell_state.border_left = (should_hide_borders && is_left_most_cell) ? 0 : cell.box.computed_values().border_left().width;
        cell_state.border_right = (should_hide_borders && is_right_most_cell) ? 0 : cell.box.computed_values().border_right().width;

        cell_state.set_content_width((span_width - cell_state.border_box_left() - cell_state.border_box_right()));
        auto independent_formatting_context = layout_inside(cell.box, LayoutMode::Normal, cell_state.available_inner_space_or_constraints_from(available_space));
        VERIFY(independent_formatting_context);
        cell_state.set_content_height(independent_formatting_context->automatic_content_height());
        independent_formatting_context->parent_context_did_dimension_child_root_box();

        cell.baseline = box_baseline(m_state, cell.box);

        auto& row = m_rows[cell.row_index];
        row.used_width = max(row.used_width, cell_state.border_box_height());
        row.baseline = max(row.baseline, cell.baseline);
    }

    CSSPixels row_top_offset = 0.0f;
    for (size_t y = 0; y < m_rows.size(); y++) {
        auto& row = m_rows[y];
        auto& row_state = m_state.get_mutable(row.box);
        CSSPixels row_width = 0.0f;
        for (auto& column : m_columns) {
            row_width += column.used_width;
        }

        row_state.set_content_height(row.used_width);
        row_state.set_content_width(row_width);
        row_state.set_content_y(row_top_offset);
        row_top_offset += row_state.content_height();
    }

    CSSPixels row_group_top_offset = 0.0f;
    box.for_each_child_of_type<TableRowGroupBox>([&](auto& row_group_box) {
        CSSPixels row_group_height = 0.0f;
        CSSPixels row_group_width = 0.0f;

        auto& row_group_box_state = m_state.get_mutable(row_group_box);
        row_group_box_state.set_content_y(row_group_top_offset);

        CSSPixels row_top_offset = 0.0f;
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
        CSSPixels const cell_border_box_height = cell_state.content_height() + cell_state.border_box_top() + cell_state.border_box_bottom();
        CSSPixels const row_content_height = row_state.content_height();
        auto const& vertical_align = cell.box.computed_values().vertical_align();
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

    m_state.get_mutable(context_box()).set_content_height(total_content_height);

    // FIXME: This is a total hack, we should respect the 'height' property.
    m_automatic_content_height = total_content_height;
}

CSSPixels TableFormattingContext::automatic_content_height() const
{
    return m_automatic_content_height;
}

}
