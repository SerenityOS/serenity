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

#include "SpreadsheetWidget.h"
#include "CellSyntaxHighlighter.h"
#include "HelpWindow.h"
#include <LibCore/File.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Label.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Splitter.h>
#include <LibGUI/TabWidget.h>
#include <LibGUI/TextEditor.h>
#include <string.h>

namespace Spreadsheet {

SpreadsheetWidget::SpreadsheetWidget(NonnullRefPtrVector<Sheet>&& sheets, bool should_add_sheet_if_empty)
    : m_workbook(make<Workbook>(move(sheets)))
{
    set_fill_with_background_color(true);
    set_layout<GUI::VerticalBoxLayout>().set_margins({ 2, 2, 2, 2 });
    auto& container = add<GUI::VerticalSplitter>();

    auto& top_bar = container.add<GUI::Frame>();
    top_bar.set_layout<GUI::HorizontalBoxLayout>().set_spacing(1);
    top_bar.set_preferred_size(0, 50);
    top_bar.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    auto& current_cell_label = top_bar.add<GUI::Label>("");
    current_cell_label.set_preferred_size(50, 0);
    current_cell_label.set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fill);

    auto& help_button = top_bar.add<GUI::Button>("🛈");
    help_button.set_preferred_size(20, 20);
    help_button.set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fixed);
    help_button.on_click = [&](auto) {
        auto docs = m_selected_view->sheet().gather_documentation();
        auto help_window = HelpWindow::the();
        help_window->set_docs(move(docs));
        help_window->show();
    };

    auto& cell_value_editor = top_bar.add<GUI::TextEditor>(GUI::TextEditor::Type::SingleLine);
    cell_value_editor.set_font(Gfx::Font::default_fixed_width_font());
    cell_value_editor.set_scrollbars_enabled(false);

    cell_value_editor.set_syntax_highlighter(make<CellSyntaxHighlighter>());
    cell_value_editor.set_enabled(false);
    current_cell_label.set_enabled(false);

    m_tab_widget = container.add<GUI::TabWidget>();
    m_tab_widget->set_tab_position(GUI::TabWidget::TabPosition::Bottom);

    m_cell_value_editor = cell_value_editor;
    m_current_cell_label = current_cell_label;

    if (!m_workbook->has_sheets() && should_add_sheet_if_empty)
        m_workbook->add_sheet("Sheet 1");

    setup_tabs(m_workbook->sheets());
}

void SpreadsheetWidget::setup_tabs(NonnullRefPtrVector<Sheet> new_sheets)
{
    RefPtr<GUI::Widget> first_tab_widget;
    for (auto& sheet : new_sheets) {
        auto& tab = m_tab_widget->add_tab<SpreadsheetView>(sheet.name(), sheet);
        if (!first_tab_widget)
            first_tab_widget = &tab;
    }

    auto change = [&](auto& selected_widget) {
        if (m_selected_view) {
            m_selected_view->on_selection_changed = nullptr;
            m_selected_view->on_selection_dropped = nullptr;
        };
        m_selected_view = &static_cast<SpreadsheetView&>(selected_widget);
        m_selected_view->on_selection_changed = [&](Vector<Position>&& selection) {
            if (selection.size() == 1) {
                auto& position = selection.first();
                StringBuilder builder;
                builder.append(position.column);
                builder.appendff("{}", position.row);
                m_current_cell_label->set_enabled(true);
                m_current_cell_label->set_text(builder.string_view());

                auto& cell = m_selected_view->sheet().ensure(position);
                m_cell_value_editor->on_change = nullptr;
                m_cell_value_editor->set_text(cell.source());
                m_cell_value_editor->on_change = [&] {
                    cell.set_data(m_cell_value_editor->text());
                    m_selected_view->sheet().update();
                };
                m_cell_value_editor->set_enabled(true);
                return;
            }

            // There are many cells selected, change all of them.
            StringBuilder builder;
            builder.appendff("<{}>", selection.size());
            m_current_cell_label->set_enabled(true);
            m_current_cell_label->set_text(builder.string_view());

            Vector<Cell*> cells;
            for (auto& position : selection)
                cells.append(&m_selected_view->sheet().ensure(position));

            m_cell_value_editor->on_change = nullptr;
            m_cell_value_editor->set_text("");
            m_should_change_selected_cells = false;
            m_cell_value_editor->on_focusin = [this] { m_should_change_selected_cells = true; };
            m_cell_value_editor->on_focusout = [this] { m_should_change_selected_cells = false; };
            m_cell_value_editor->on_change = [cells = move(cells), this] {
                if (m_should_change_selected_cells) {
                    for (auto* cell : cells)
                        cell->set_data(m_cell_value_editor->text());
                    m_selected_view->sheet().update();
                }
            };
            m_cell_value_editor->set_enabled(true);
        };
        m_selected_view->on_selection_dropped = [&]() {
            m_cell_value_editor->set_enabled(false);
            m_cell_value_editor->set_text("");
            m_current_cell_label->set_enabled(false);
            m_current_cell_label->set_text("");
        };
    };

    if (first_tab_widget)
        change(*first_tab_widget);

    m_tab_widget->on_change = [change = move(change)](auto& selected_widget) {
        change(selected_widget);
    };
}

void SpreadsheetWidget::save(const StringView& filename)
{
    auto result = m_workbook->save(filename);
    if (result.is_error())
        GUI::MessageBox::show_error(window(), result.error());
}

void SpreadsheetWidget::load(const StringView& filename)
{
    auto result = m_workbook->load(filename);
    if (result.is_error()) {
        GUI::MessageBox::show_error(window(), result.error());
        return;
    }
    while (auto* widget = m_tab_widget->active_widget()) {
        m_tab_widget->remove_tab(*widget);
    }

    setup_tabs(m_workbook->sheets());
}

void SpreadsheetWidget::add_sheet()
{
    StringBuilder name;
    name.append("Sheet");
    name.appendff(" {}", m_workbook->sheets().size() + 1);

    auto& sheet = m_workbook->add_sheet(name.string_view());

    NonnullRefPtrVector<Sheet> new_sheets;
    new_sheets.append(sheet);
    setup_tabs(new_sheets);
}

void SpreadsheetWidget::set_filename(const String& filename)
{
    if (m_workbook->set_filename(filename)) {
        StringBuilder builder;
        builder.append("Spreadsheet - ");
        builder.append(current_filename());

        window()->set_title(builder.string_view());
        window()->update();
    }
}

SpreadsheetWidget::~SpreadsheetWidget()
{
}
}
