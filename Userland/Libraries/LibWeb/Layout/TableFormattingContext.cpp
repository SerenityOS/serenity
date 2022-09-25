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

namespace Web::Layout {

TableFormattingContext::TableFormattingContext(LayoutState& state, BlockContainer const& block_container, FormattingContext* parent)
    : BlockFormattingContext(state, block_container, parent)
{
}

TableFormattingContext::~TableFormattingContext() = default;

void TableFormattingContext::run(Box const& box, LayoutMode, [[maybe_unused]] AvailableSpace const& available_width, [[maybe_unused]] AvailableSpace const& available_height)
{
    auto& box_state = m_state.get_mutable(box);

    compute_width(box);
    auto table_width = CSS::Length::make_px(box_state.content_width());
    auto table_width_is_auto = box.computed_values().width().is_auto();

    float total_content_width = 0;
    float total_content_height = 0;

    Vector<ColumnWidth> column_widths;
    box.for_each_child_of_type<TableRowGroupBox>([&](auto& row_group_box) {
        compute_width(row_group_box);
        auto column_count = row_group_box.column_count();
        column_widths.resize(max(column_count, column_widths.size()));

        row_group_box.template for_each_child_of_type<TableRowBox>([&](auto& row) {
            calculate_column_widths(row, table_width, column_widths);
        });
    });
    box.for_each_child_of_type<TableRowGroupBox>([&](auto& row_group_box) {
        auto& row_group_box_state = m_state.get_mutable(row_group_box);

        float remaining_for_max = box_state.content_width();
        float remaining_for_min = box_state.content_width();
        for (auto& column_width : column_widths) {
            remaining_for_max -= column_width.max;
            remaining_for_min -= column_width.min;
        }

        bool max_fits = remaining_for_max >= 0;
        bool min_fits = remaining_for_min >= 0;

        if (max_fits) {
            for (auto& column_width : column_widths)
                column_width.used = column_width.max;
        } else {
            for (auto& column_width : column_widths)
                column_width.used = column_width.min;
        }

        if (!table_width_is_auto || (min_fits && !max_fits)) {
            float missing_width = max_fits ? remaining_for_max : remaining_for_min;
            if (missing_width > 0) {
                size_t num_auto_columns = 0;
                for (auto& column_width : column_widths) {
                    if (column_width.is_auto)
                        num_auto_columns++;
                }
                if (num_auto_columns) {
                    float extra = missing_width / (float)num_auto_columns;
                    for (auto& column_width : column_widths) {
                        if (column_width.is_auto)
                            column_width.used += extra;
                    }
                }
            }
        }

        float content_width = 0;
        float content_height = 0;

        row_group_box.template for_each_child_of_type<TableRowBox>([&](auto& row) {
            auto& row_state = m_state.get_mutable(row);
            row_state.offset = { 0, content_height };
            layout_row(row, column_widths);
            content_width = max(content_width, row_state.content_width());
            content_height += row_state.content_height();
        });

        if (row_group_box.computed_values().width().is_auto())
            row_group_box_state.set_content_width(content_width);
        row_group_box_state.set_content_height(content_height);

        row_group_box_state.offset = { 0, total_content_height };
        total_content_height += content_height;
        total_content_width = max(total_content_width, row_group_box_state.content_width());
    });

    if (table_width_is_auto)
        box_state.set_content_width(total_content_width);

    // FIXME: This is a total hack, we should respect the 'height' property.
    m_automatic_content_height = total_content_height;
}

void TableFormattingContext::calculate_column_widths(Box const& row, CSS::Length const& table_width, Vector<ColumnWidth>& column_widths)
{
    m_state.get_mutable(row);
    size_t column_index = 0;
    row.for_each_child_of_type<TableCellBox>([&](auto& cell) {
        auto& cell_state = m_state.get_mutable(cell);
        auto const& computed_values = cell.computed_values();
        auto specified_width = computed_values.width().resolved(cell, table_width).resolved(cell);

        if (specified_width.is_auto()) {
            auto width = calculate_max_content_width(cell);
            cell_state.set_content_width(width);
        } else {
            compute_width(cell, LayoutMode::Normal);
        }

        (void)layout_inside(cell, LayoutMode::Normal);

        if (cell.colspan() == 1) {
            auto min_width = calculate_min_content_width(cell);
            auto max_width = calculate_max_content_width(cell);
            min_width = max(min_width, cell_state.border_box_width());
            max_width = max(max_width, cell_state.border_box_width());
            column_widths[column_index].min = max(column_widths[column_index].min, min_width);
            column_widths[column_index].max = max(column_widths[column_index].max, max_width);
            column_widths[column_index].is_auto &= specified_width.is_auto();
        }
        column_index += cell.colspan();
    });
    column_index = 0;
    row.for_each_child_of_type<TableCellBox>([&](auto& cell) {
        size_t colspan = cell.colspan();
        if (colspan > 1) {
            auto& cell_state = m_state.get_mutable(cell);
            auto min_width = calculate_min_content_width(cell);
            auto max_width = calculate_max_content_width(cell);
            float missing_min = max(min_width, cell_state.border_box_width());
            float missing_max = max(max_width, cell_state.border_box_width());
            for (size_t i = 0; i < colspan; ++i) {
                missing_min -= column_widths[column_index + i].min;
                missing_max -= column_widths[column_index + i].max;
            }
            if (missing_min > 0) {
                float extra = missing_min / (float)colspan;
                for (size_t i = 0; i < colspan; ++i)
                    column_widths[column_index + i].min += extra;
            }
            if (missing_max > 0) {
                float extra = missing_max / (float)colspan;
                for (size_t i = 0; i < colspan; ++i)
                    column_widths[column_index + i].max += extra;
            }
        }
        column_index += colspan;
    });
}

void TableFormattingContext::layout_row(Box const& row, Vector<ColumnWidth>& column_widths)
{
    auto& row_state = m_state.get_mutable(row);
    size_t column_index = 0;
    float tallest_cell_height = 0;
    float content_width = 0;
    auto* table = row.first_ancestor_of_type<TableBox>();
    bool use_auto_layout = !table || table->computed_values().width().is_auto();

    row.for_each_child_of_type<TableCellBox>([&](auto& cell) {
        auto& cell_state = m_state.get_mutable(cell);

        float span_width = 0;
        for (size_t i = 0; i < cell.colspan(); ++i)
            span_width += column_widths[column_index++].used;
        cell_state.set_content_width(span_width - cell_state.border_box_left() - cell_state.border_box_right());

        BlockFormattingContext::compute_height(cell, m_state);
        cell_state.offset = row_state.offset.translated(cell_state.border_box_left() + content_width, cell_state.border_box_top());

        // Layout the cell contents a second time, now that we know its final width.
        (void)layout_inside(cell, LayoutMode::Normal);

        content_width += span_width;
        tallest_cell_height = max(tallest_cell_height, cell_state.border_box_height());
    });

    row_state.set_content_height(tallest_cell_height);

    row.for_each_child_of_type<TableCellBox>([&](auto& cell) {
        auto& cell_state = m_state.get_mutable(cell);
        cell_state.set_content_height(tallest_cell_height - cell_state.border_box_top() - cell_state.border_box_bottom());
    });

    if (use_auto_layout) {
        row_state.set_content_width(content_width);
    } else {
        auto& table_state = m_state.get_mutable(*table);
        row_state.set_content_width(table_state.content_width());
    }
}

float TableFormattingContext::automatic_content_height() const
{
    return m_automatic_content_height;
}

}
