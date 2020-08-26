/*
 * Copyright (c) 2020, the SerenityOS developers.
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

#include "SpreadsheetView.h"
#include "SpreadsheetModel.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/HeaderView.h>
#include <LibGUI/ModelEditingDelegate.h>
#include <LibGUI/Painter.h>
#include <LibGUI/TableView.h>
#include <LibGfx/Palette.h>

namespace Spreadsheet {

SpreadsheetView::~SpreadsheetView()
{
}

void SpreadsheetView::EditingDelegate::set_value(const GUI::Variant& value)
{
    if (m_has_set_initial_value)
        return StringModelEditingDelegate::set_value(value);

    m_has_set_initial_value = true;
    const auto option = m_sheet.at({ m_sheet.column(index().column()), (size_t)index().row() });
    if (option)
        return StringModelEditingDelegate::set_value(option->source());

    StringModelEditingDelegate::set_value("");
}

SpreadsheetView::SpreadsheetView(Sheet& sheet)
    : m_sheet(sheet)
{
    set_layout<GUI::VerticalBoxLayout>().set_margins({ 2, 2, 2, 2 });
    m_table_view = add<GUI::TableView>();
    m_table_view->row_header().set_visible(true);
    m_table_view->set_model(SheetModel::create(*m_sheet));

    set_focus_proxy(m_table_view);

    // FIXME: This is dumb.
    for (size_t i = 0; i < m_sheet->column_count(); ++i) {
        m_table_view->set_column_painting_delegate(i, make<TableCellPainter>(*m_table_view));
        m_table_view->set_column_width(i, 50);
        m_table_view->set_column_header_alignment(i, Gfx::TextAlignment::Center);
    }

    m_table_view->set_alternating_row_colors(false);
    m_table_view->set_highlight_selected_rows(false);
    m_table_view->set_editable(true);
    m_table_view->aid_create_editing_delegate = [&](auto&) {
        return make<EditingDelegate>(*m_sheet);
    };

    m_table_view->on_selection_change = [&] {
        if (m_table_view->selection().is_empty() && on_selection_dropped)
            return on_selection_dropped();

        auto selection = m_table_view->selection().first();

        Position position { m_sheet->column(selection.column()), (size_t)selection.row() };
        auto& cell = m_sheet->ensure(position);
        if (on_selection_changed)
            on_selection_changed(position, cell);

        m_table_view->model()->update();
        m_table_view->update();
    };
}

void SpreadsheetView::TableCellPainter::paint(GUI::Painter& painter, const Gfx::IntRect& rect, const Gfx::Palette& palette, const GUI::ModelIndex& index)
{
    // Draw a border.
    // Undo the horizontal padding done by the table view...
    painter.draw_rect(rect.inflated(m_table_view.horizontal_padding() * 2, 0), palette.ruler());
    if (m_table_view.selection().contains(index))
        painter.draw_rect(rect.inflated(m_table_view.horizontal_padding() * 2 + 1, 1), palette.ruler_border());

    auto data = index.data();
    auto text_alignment = index.data(GUI::ModelRole::TextAlignment).to_text_alignment(Gfx::TextAlignment::CenterLeft);
    painter.draw_text(rect, data.to_string(), m_table_view.font_for_index(index), text_alignment, palette.color(m_table_view.foreground_role()), Gfx::TextElision::Right);
}

}
