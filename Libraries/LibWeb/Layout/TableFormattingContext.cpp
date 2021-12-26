/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <LibWeb/CSS/Length.h>
#include <LibWeb/DOM/Node.h>
#include <LibWeb/Layout/BlockBox.h>
#include <LibWeb/Layout/Box.h>
#include <LibWeb/Layout/InlineFormattingContext.h>
#include <LibWeb/Layout/TableBox.h>
#include <LibWeb/Layout/TableCellBox.h>
#include <LibWeb/Layout/TableFormattingContext.h>
#include <LibWeb/Layout/TableRowBox.h>
#include <LibWeb/Layout/TableRowGroupBox.h>
#include <LibWeb/Page/Frame.h>

namespace Web::Layout {

TableFormattingContext::TableFormattingContext(Box& context_box)
    : BlockFormattingContext(context_box)
{
}

TableFormattingContext::~TableFormattingContext()
{
}

void TableFormattingContext::run(LayoutMode)
{
    compute_width(context_box());

    context_box().for_each_child_of_type<TableRowGroupBox>([&](auto& box) {
        compute_width(box);
        auto column_count = box.column_count();
        Vector<float> column_widths;
        column_widths.resize(column_count);

        box.template for_each_child_of_type<TableRowBox>([&](auto& row) {
            calculate_column_widths(row, column_widths);
        });

        float content_height = 0;

        box.template for_each_child_of_type<TableRowBox>([&](auto& row) {
            row.set_offset(0, content_height);
            layout_row(row, column_widths);
            content_height += row.height();
        });

        box.set_height(content_height);
    });

    compute_height(context_box());
}

void TableFormattingContext::calculate_column_widths(Box& row, Vector<float>& column_widths)
{
    size_t column_index = 0;
    auto* table = row.first_ancestor_of_type<TableBox>();
    bool use_auto_layout = !table || table->style().width().is_undefined_or_auto();
    row.for_each_child_of_type<TableCellBox>([&](auto& cell) {
        compute_width(cell);
        if (use_auto_layout) {
            layout_inside(cell, LayoutMode::OnlyRequiredLineBreaks);
        } else {
            layout_inside(cell, LayoutMode::Default);
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
    bool use_auto_layout = !table || table->style().width().is_undefined_or_auto();

    row.for_each_child_of_type<TableCellBox>([&](auto& cell) {
        cell.set_offset(row.effective_offset().translated(content_width, 0));

        // Layout the cell contents a second time, now that we know its final width.
        if (use_auto_layout) {
            layout_inside(cell, LayoutMode::OnlyRequiredLineBreaks);
        } else {
            layout_inside(cell, LayoutMode::Default);
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
