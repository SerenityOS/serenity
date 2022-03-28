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

TableFormattingContext::TableFormattingContext(FormattingState& state, BlockContainer const& block_container, FormattingContext* parent)
    : BlockFormattingContext(state, block_container, parent)
{
}

TableFormattingContext::~TableFormattingContext() = default;

void TableFormattingContext::run(Box const& box, LayoutMode)
{
    auto& box_state = m_state.get_mutable(box);

    compute_width(box);
    auto table_width = CSS::Length::make_px(box_state.content_width);

    float total_content_width = 0;
    float total_content_height = 0;

    box.for_each_child_of_type<TableRowGroupBox>([&](auto& row_group_box) {
        auto& row_group_box_state = m_state.get_mutable(row_group_box);

        compute_width(row_group_box);
        auto column_count = row_group_box.column_count();
        Vector<float> column_widths;
        column_widths.resize(column_count);

        row_group_box.template for_each_child_of_type<TableRowBox>([&](auto& row) {
            calculate_column_widths(row, table_width, column_widths);
        });

        float missing_width = box_state.content_width;
        for (auto column_width : column_widths)
            missing_width -= column_width;
        if (missing_width > 0)
            column_widths[column_widths.size() - 1] += missing_width;

        float content_width = 0;
        float content_height = 0;

        row_group_box.template for_each_child_of_type<TableRowBox>([&](auto& row) {
            auto& row_state = m_state.get_mutable(row);
            row_state.offset = { 0, content_height };
            layout_row(row, column_widths);
            content_width = max(content_width, row_state.content_width);
            content_height += row_state.content_height;
        });

        if (row_group_box.computed_values().width().has_value() && row_group_box.computed_values().width()->is_length() && row_group_box.computed_values().width()->length().is_auto())
            row_group_box_state.content_width = content_width;
        row_group_box_state.content_height = content_height;

        row_group_box_state.offset = { 0, total_content_height };
        total_content_height += content_height;
        total_content_width = max(total_content_width, row_group_box_state.content_width);
    });

    if (box.computed_values().width().has_value() && box.computed_values().width()->is_length() && box.computed_values().width()->length().is_auto())
        box_state.content_width = total_content_width;

    // FIXME: This is a total hack, we should respect the 'height' property.
    box_state.content_height = total_content_height;
}

void TableFormattingContext::calculate_column_widths(Box const& row, CSS::Length const& table_width, Vector<float>& column_widths)
{
    m_state.get_mutable(row);
    size_t column_index = 0;
    bool use_auto_layout = table_width.is_auto();
    row.for_each_child_of_type<TableCellBox>([&](auto& cell) {
        auto& cell_state = m_state.get_mutable(cell);
        auto const& computed_values = cell.computed_values();
        auto specified_width = computed_values.width().has_value() ? computed_values.width()->resolved(cell, table_width).resolved(cell) : CSS::Length::make_auto();
        compute_width(cell, specified_width.is_auto() ? LayoutMode::MinContent : LayoutMode::Normal);
        if (use_auto_layout) {
            (void)layout_inside(cell, LayoutMode::MaxContent);
        } else {
            (void)layout_inside(cell, LayoutMode::Normal);
        }
        if (cell.colspan() == 1)
            column_widths[column_index] = max(column_widths[column_index], cell_state.border_box_width());
        column_index += cell.colspan();
    });
    column_index = 0;
    row.for_each_child_of_type<TableCellBox>([&](auto& cell) {
        auto& cell_state = m_state.get_mutable(cell);
        size_t colspan = cell.colspan();
        if (colspan != 1) {
            float missing = cell_state.border_box_width();
            for (size_t i = 0; i < colspan; ++i)
                missing -= column_widths[column_index + i];
            if (missing > 0) {
                float extra = missing / (float)colspan;
                for (size_t i = 0; i < colspan; ++i)
                    column_widths[column_index + i] += extra;
            }
        }
        column_index += colspan;
    });
}

void TableFormattingContext::layout_row(Box const& row, Vector<float>& column_widths)
{
    auto& row_state = m_state.get_mutable(row);
    size_t column_index = 0;
    float tallest_cell_height = 0;
    float content_width = 0;
    auto* table = row.first_ancestor_of_type<TableBox>();
    bool use_auto_layout = !table || (!table->computed_values().width().has_value() || (table->computed_values().width()->is_length() && table->computed_values().width()->length().is_auto()));

    row.for_each_child_of_type<TableCellBox>([&](auto& cell) {
        auto& cell_state = m_state.get_mutable(cell);

        float span_width = 0;
        for (size_t i = 0; i < cell.colspan(); ++i)
            span_width += column_widths[column_index++];
        cell_state.content_width = span_width - cell_state.border_box_left() - cell_state.border_box_right();

        BlockFormattingContext::compute_height(cell, m_state);
        cell_state.offset = row_state.offset.translated(cell_state.border_box_left() + content_width, cell_state.border_box_top());

        // Layout the cell contents a second time, now that we know its final width.
        if (use_auto_layout) {
            (void)layout_inside(cell, LayoutMode::MaxContent);
        } else {
            (void)layout_inside(cell, LayoutMode::Normal);
        }

        content_width += span_width;
        tallest_cell_height = max(tallest_cell_height, cell_state.border_box_height());
    });

    row_state.content_height = tallest_cell_height;

    row.for_each_child_of_type<TableCellBox>([&](auto& cell) {
        auto& cell_state = m_state.get_mutable(cell);
        cell_state.content_height = tallest_cell_height - cell_state.border_box_top() - cell_state.border_box_bottom();
    });

    if (use_auto_layout) {
        row_state.content_width = content_width;
    } else {
        auto& table_state = m_state.get_mutable(*table);
        row_state.content_width = table_state.content_width;
    }
}

}
