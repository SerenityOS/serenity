/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <LibWeb/DOM/Element.h>
#include <LibWeb/Layout/LayoutTable.h>
#include <LibWeb/Layout/LayoutTableCell.h>
#include <LibWeb/Layout/LayoutTableRow.h>

namespace Web {

LayoutTableRow::LayoutTableRow(DOM::Document& document, DOM::Element& element, NonnullRefPtr<CSS::StyleProperties> style)
    : LayoutBox(document, &element, move(style))
{
}

LayoutTableRow::~LayoutTableRow()
{
}

void LayoutTableRow::layout(LayoutMode)
{
}

void LayoutTableRow::calculate_column_widths(Vector<float>& column_widths)
{
    size_t column_index = 0;
    auto* table = first_ancestor_of_type<LayoutTable>();
    bool use_auto_layout = !table || table->style().width().is_undefined_or_auto();
    for_each_child_of_type<LayoutTableCell>([&](auto& cell) {
        if (use_auto_layout) {
            cell.layout(LayoutMode::OnlyRequiredLineBreaks);
        } else {
            cell.layout(LayoutMode::Default);
        }
        column_widths[column_index] = max(column_widths[column_index], cell.width());
        column_index += cell.colspan();
    });
}

void LayoutTableRow::layout_row(const Vector<float>& column_widths)
{
    size_t column_index = 0;
    float tallest_cell_height = 0;
    float content_width = 0;
    auto* table = first_ancestor_of_type<LayoutTable>();
    bool use_auto_layout = !table || table->style().width().is_undefined_or_auto();

    for_each_child_of_type<LayoutTableCell>([&](auto& cell) {
        cell.set_offset(effective_offset().translated(content_width, 0));

        // Layout the cell contents a second time, now that we know its final width.
        if (use_auto_layout) {
            cell.layout_inside(LayoutMode::OnlyRequiredLineBreaks);
        } else {
            cell.layout_inside(LayoutMode::Default);
        }

        size_t cell_colspan = cell.colspan();
        for (size_t i = 0; i < cell_colspan; ++i)
            content_width += column_widths[column_index++];
        tallest_cell_height = max(tallest_cell_height, cell.height());
    });

    if (use_auto_layout) {
        set_width(content_width);
    } else {
        set_width(table->width());
    }

    set_height(tallest_cell_height);
}

}
