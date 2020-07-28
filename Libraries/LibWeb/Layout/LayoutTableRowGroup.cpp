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

#include <LibWeb/DOM/Element.h>
#include <LibWeb/Layout/LayoutTableCell.h>
#include <LibWeb/Layout/LayoutTableRow.h>
#include <LibWeb/Layout/LayoutTableRowGroup.h>

namespace Web {

LayoutTableRowGroup::LayoutTableRowGroup(DOM::Document& document, DOM::Element& element, NonnullRefPtr<CSS::StyleProperties> style)
    : LayoutBlock(document, &element, move(style))
{
}

LayoutTableRowGroup::~LayoutTableRowGroup()
{
}

size_t LayoutTableRowGroup::column_count() const
{
    size_t table_column_count = 0;
    for_each_child_of_type<LayoutTableRow>([&](auto& row) {
        size_t row_column_count = 0;
        row.template for_each_child_of_type<LayoutTableCell>([&](auto& cell) {
            row_column_count += cell.colspan();
        });
        table_column_count = max(table_column_count, row_column_count);
    });
    return table_column_count;
}

void LayoutTableRowGroup::layout(LayoutMode)
{
    compute_width();

    auto column_count = this->column_count();
    Vector<float> column_widths;
    column_widths.resize(column_count);

    for_each_child_of_type<LayoutTableRow>([&](auto& row) {
        row.calculate_column_widths(column_widths);
    });

    float content_height = 0;

    for_each_child_of_type<LayoutTableRow>([&](auto& row) {
        row.set_offset(0, content_height);
        row.layout_row(column_widths);
        content_height += row.height();
    });

    set_height(content_height);
}

}
