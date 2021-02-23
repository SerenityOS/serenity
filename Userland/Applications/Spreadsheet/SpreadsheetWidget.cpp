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
#include "LibGUI/InputBox.h"
#include <LibCore/File.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/Label.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Splitter.h>
#include <LibGUI/TabWidget.h>
#include <LibGUI/TextEditor.h>
#include <LibGfx/FontDatabase.h>
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
    top_bar.set_fixed_height(26);
    auto& current_cell_label = top_bar.add<GUI::Label>("");
    current_cell_label.set_fixed_width(50);

    auto& help_button = top_bar.add<GUI::Button>("🛈");
    help_button.set_fixed_size(20, 20);
    help_button.on_click = [&](auto) {
        auto docs = m_selected_view->sheet().gather_documentation();
        auto help_window = HelpWindow::the(window());
        help_window->set_docs(move(docs));
        help_window->show();
    };

    auto& cell_value_editor = top_bar.add<GUI::TextEditor>(GUI::TextEditor::Type::SingleLine);
    cell_value_editor.set_font(Gfx::FontDatabase::default_fixed_width_font());
    cell_value_editor.set_scrollbars_enabled(false);

    cell_value_editor.set_syntax_highlighter(make<CellSyntaxHighlighter>());
    cell_value_editor.set_enabled(false);
    current_cell_label.set_enabled(false);

    m_tab_widget = container.add<GUI::TabWidget>();
    m_tab_widget->set_tab_position(GUI::TabWidget::TabPosition::Bottom);

    m_cell_value_editor = cell_value_editor;
    m_current_cell_label = current_cell_label;
    m_inline_documentation_window = GUI::Window::construct(window());
    m_inline_documentation_window->set_rect(m_cell_value_editor->rect().translated(0, m_cell_value_editor->height() + 7).inflated(6, 6));
    m_inline_documentation_window->set_window_type(GUI::WindowType::Tooltip);
    m_inline_documentation_window->set_resizable(false);
    auto& inline_widget = m_inline_documentation_window->set_main_widget<GUI::Frame>();
    inline_widget.set_fill_with_background_color(true);
    inline_widget.set_layout<GUI::VerticalBoxLayout>().set_margins({ 4, 4, 4, 4 });
    inline_widget.set_frame_shape(Gfx::FrameShape::Box);
    m_inline_documentation_label = inline_widget.add<GUI::Label>();
    m_inline_documentation_label->set_fill_with_background_color(true);
    m_inline_documentation_label->set_autosize(false);
    m_inline_documentation_label->set_text_alignment(Gfx::TextAlignment::CenterLeft);

    if (!m_workbook->has_sheets() && should_add_sheet_if_empty)
        m_workbook->add_sheet("Sheet 1");

    m_tab_context_menu = GUI::Menu::construct();
    auto rename_action = GUI::Action::create("Rename...", [this](auto&) {
        VERIFY(m_tab_context_menu_sheet_view);

        auto& sheet = m_tab_context_menu_sheet_view->sheet();
        String new_name;
        if (GUI::InputBox::show(window(), new_name, String::formatted("New name for '{}'", sheet.name()), "Rename sheet") == GUI::Dialog::ExecOK) {
            sheet.set_name(new_name);
            sheet.update();
            m_tab_widget->set_tab_title(static_cast<GUI::Widget&>(*m_tab_context_menu_sheet_view), new_name);
        }
    });
    m_tab_context_menu->add_action(rename_action);
    m_tab_context_menu->add_action(GUI::Action::create("Add new sheet...", [this](auto&) {
        String name;
        if (GUI::InputBox::show(window(), name, "Name for new sheet", "Create sheet") == GUI::Dialog::ExecOK) {
            NonnullRefPtrVector<Sheet> new_sheets;
            new_sheets.append(m_workbook->add_sheet(name));
            setup_tabs(move(new_sheets));
        }
    }));

    setup_tabs(m_workbook->sheets());
}

void SpreadsheetWidget::resize_event(GUI::ResizeEvent& event)
{
    GUI::Widget::resize_event(event);
    if (m_inline_documentation_window && m_cell_value_editor && window())
        m_inline_documentation_window->set_rect(m_cell_value_editor->screen_relative_rect().translated(0, m_cell_value_editor->height() + 7).inflated(6, 6));
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
            if (selection.is_empty()) {
                m_current_cell_label->set_enabled(false);
                m_current_cell_label->set_text({});
                m_cell_value_editor->on_change = nullptr;
                m_cell_value_editor->on_focusin = nullptr;
                m_cell_value_editor->on_focusout = nullptr;
                m_cell_value_editor->set_text("");
                m_cell_value_editor->set_enabled(false);
                return;
            }

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
                    auto text = m_cell_value_editor->text();
                    // FIXME: Lines?
                    auto offset = m_cell_value_editor->cursor().column();
                    try_generate_tip_for_input_expression(text, offset);
                    cell.set_data(move(text));
                    m_selected_view->sheet().update();
                    update();
                };
                m_cell_value_editor->set_enabled(true);
                static_cast<CellSyntaxHighlighter*>(const_cast<Syntax::Highlighter*>(m_cell_value_editor->syntax_highlighter()))->set_cell(&cell);
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

            auto first_cell = cells.first();
            m_cell_value_editor->on_change = nullptr;
            m_cell_value_editor->set_text("");
            m_should_change_selected_cells = false;
            m_cell_value_editor->on_focusin = [this] { m_should_change_selected_cells = true; };
            m_cell_value_editor->on_focusout = [this] { m_should_change_selected_cells = false; };
            m_cell_value_editor->on_change = [cells = move(cells), this] {
                if (m_should_change_selected_cells) {
                    auto text = m_cell_value_editor->text();
                    // FIXME: Lines?
                    auto offset = m_cell_value_editor->cursor().column();
                    try_generate_tip_for_input_expression(text, offset);
                    for (auto* cell : cells)
                        cell->set_data(text);
                    m_selected_view->sheet().update();
                    update();
                }
            };
            m_cell_value_editor->set_enabled(true);
            static_cast<CellSyntaxHighlighter*>(const_cast<Syntax::Highlighter*>(m_cell_value_editor->syntax_highlighter()))->set_cell(first_cell);
        };
        m_selected_view->on_selection_dropped = [&]() {
            m_cell_value_editor->set_enabled(false);
            static_cast<CellSyntaxHighlighter*>(const_cast<Syntax::Highlighter*>(m_cell_value_editor->syntax_highlighter()))->set_cell(nullptr);
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

    m_tab_widget->on_context_menu_request = [&](auto& widget, auto& event) {
        m_tab_context_menu_sheet_view = widget;
        m_tab_context_menu->popup(event.screen_position());
    };
}

void SpreadsheetWidget::try_generate_tip_for_input_expression(StringView source, size_t cursor_offset)
{
    m_inline_documentation_window->set_rect(m_cell_value_editor->screen_relative_rect().translated(0, m_cell_value_editor->height() + 7).inflated(6, 6));
    if (!m_selected_view || !source.starts_with('=')) {
        m_inline_documentation_window->hide();
        return;
    }
    auto maybe_function_and_argument = get_function_and_argument_index(source.substring_view(0, cursor_offset));
    if (!maybe_function_and_argument.has_value()) {
        m_inline_documentation_window->hide();
        return;
    }

    auto& [name, index] = maybe_function_and_argument.value();
    auto& sheet = m_selected_view->sheet();
    auto text = sheet.generate_inline_documentation_for(name, index);
    if (text.is_empty()) {
        m_inline_documentation_window->hide();
    } else {
        m_inline_documentation_label->set_text(move(text));
        m_inline_documentation_window->show();
    }
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

    m_tab_widget->on_change = nullptr;
    m_cell_value_editor->on_change = nullptr;
    m_current_cell_label->set_text("");
    m_should_change_selected_cells = false;
    while (auto* widget = m_tab_widget->active_widget()) {
        m_tab_widget->remove_tab(*widget);
    }

    setup_tabs(m_workbook->sheets());
}

bool SpreadsheetWidget::request_close()
{
    if (!m_workbook->dirty())
        return true;

    auto result = GUI::MessageBox::show(window(), "The spreadsheet has been modified. Would you like to save?", "Unsaved changes", GUI::MessageBox::Type::Warning, GUI::MessageBox::InputType::YesNoCancel);

    if (result == GUI::MessageBox::ExecYes) {
        if (current_filename().is_empty()) {
            String name = "workbook";
            Optional<String> save_path = GUI::FilePicker::get_save_filepath(window(), name, "sheets");
            if (!save_path.has_value())
                return false;

            save(save_path.value());
        } else {
            save(current_filename());
        }
        return true;
    }

    if (result == GUI::MessageBox::ExecNo)
        return true;

    return false;
}

void SpreadsheetWidget::add_sheet()
{
    StringBuilder name;
    name.append("Sheet");
    name.appendff(" {}", m_workbook->sheets().size() + 1);

    NonnullRefPtrVector<Sheet> new_sheets;
    new_sheets.append(m_workbook->add_sheet(name.string_view()));
    setup_tabs(move(new_sheets));
}

void SpreadsheetWidget::add_sheet(NonnullRefPtr<Sheet>&& sheet)
{
    VERIFY(m_workbook == &sheet->workbook());

    NonnullRefPtrVector<Sheet> new_sheets;
    new_sheets.append(move(sheet));
    m_workbook->sheets().append(new_sheets);
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
