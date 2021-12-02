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

TableFormattingContext::TableFormattingContext(BlockContainer& block_container, FormattingContext* parent)
    : BlockFormattingContext(block_container, parent)
{
}

TableFormattingContext::~TableFormattingContext()
{
}

void TableFormattingContext::run(Box& box, LayoutMode)
{
    compute_width(box);

    float total_content_width = 0;
    float total_content_height = 0;

    box.for_each_child_of_type<TableRowGroupBox>([&](auto& row_group_box) {
        compute_width(row_group_box);
        auto column_count = row_group_box.column_count();
        Vector<float> column_widths;
        column_widths.resize(column_count);

        row_group_box.template for_each_child_of_type<TableRowBox>([&](auto& row) {
            calculate_column_widths(row, column_widths);
        });

        float content_width = 0;
        float content_height = 0;

        row_group_box.template for_each_child_of_type<TableRowBox>([&](auto& row) {
            row.set_offset(0, content_height);
            layout_row(row, column_widths);
            content_width = max(content_width, row.width());
            content_height += row.height();
        });

        if (row_group_box.computed_values().width().is_auto())
            row_group_box.set_width(content_width);
        row_group_box.set_height(content_height);

        row_group_box.set_offset(0, total_content_height);
        total_content_height += content_height;
        total_content_width = max(total_content_width, row_group_box.width());
    });

    if (box.computed_values().width().is_auto())
        box.set_width(total_content_width);

    // FIXME: This is a total hack, we should respect the 'height' property.
    box.set_height(total_content_height);
}

void TableFormattingContext::calculate_column_widths(Box& row, Vector<float>& column_widths)
{
    size_t column_index = 0;
    auto* table = row.first_ancestor_of_type<TableBox>();
    bool use_auto_layout = !table || table->computed_values().width().is_undefined_or_auto();
    row.for_each_child_of_type<TableCellBox>([&](auto& cell) {
        compute_width(cell);
        if (use_auto_layout) {
            (void)layout_inside(cell, LayoutMode::OnlyRequiredLineBreaks);
        } else {
            (void)layout_inside(cell, LayoutMode::Default);
        }
        column_widths[column_index] = max(column_widths[column_index], cell.width());
        column_index += cell.colspan();
    });
}

void TableFormattingContext::layout_row(Box& row, Vector<float>& column_widths)
{
    size_t column_index = 0;
    float tallest_cell_height = 0;
    float content_width = 0;
    auto* table = row.first_ancestor_of_type<TableBox>();
    bool use_auto_layout = !table || table->computed_values().width().is_undefined_or_auto();

    row.for_each_child_of_type<TableCellBox>([&](auto& cell) {
        cell.set_offset(row.effective_offset().translated(content_width, 0));

        // Layout the cell contents a second time, now that we know its final width.
        if (use_auto_layout) {
            (void)layout_inside(cell, LayoutMode::OnlyRequiredLineBreaks);
        } else {
            (void)layout_inside(cell, LayoutMode::Default);
        }

        size_t cell_colspan = cell.colspan();
        for (size_t i = 0; i < cell_colspan; ++i)
            content_width += column_widths[column_index++];
        tallest_cell_height = max(tallest_cell_height, cell.height());
    });

    if (use_auto_layout) {
        row.set_width(content_width);
    } else {
        row.set_width(table->width());
    }

    row.set_height(tallest_cell_height);
}

}
