/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SpreadsheetWidget.h"
#include "CellSyntaxHighlighter.h"
#include "HelpWindow.h"
#include "LibFileSystemAccessClient/Client.h"
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/InputBox.h>
#include <LibGUI/Label.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Splitter.h>
#include <LibGUI/TabWidget.h>
#include <LibGUI/TextEditor.h>
#include <LibGUI/Toolbar.h>
#include <LibGUI/ToolbarContainer.h>
#include <LibGfx/FontDatabase.h>
#include <string.h>

namespace Spreadsheet {

SpreadsheetWidget::SpreadsheetWidget(GUI::Window& parent_window, NonnullRefPtrVector<Sheet>&& sheets, bool should_add_sheet_if_empty)
    : m_workbook(make<Workbook>(move(sheets), parent_window))
{
    set_fill_with_background_color(true);
    set_layout<GUI::VerticalBoxLayout>().set_margins(2);

    auto& toolbar_container = add<GUI::ToolbarContainer>();
    auto& toolbar = toolbar_container.add<GUI::Toolbar>();

    auto& container = add<GUI::VerticalSplitter>();

    auto& top_bar = container.add<GUI::Frame>();
    top_bar.set_layout<GUI::HorizontalBoxLayout>().set_spacing(1);
    top_bar.set_fixed_height(26);
    auto& current_cell_label = top_bar.add<GUI::Label>("");
    current_cell_label.set_fixed_width(50);

    auto& help_button = top_bar.add<GUI::Button>("");
    help_button.set_icon(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/app-help.png").release_value_but_fixme_should_propagate_errors());
    help_button.set_tooltip("Functions Help");
    help_button.set_fixed_size(20, 20);
    help_button.on_click = [&](auto) {
        if (!m_selected_view) {
            GUI::MessageBox::show_error(window(), "Can only show function documentation/help when a worksheet exists and is open");
        } else if (auto* sheet_ptr = m_selected_view->sheet_if_available()) {
            auto docs = sheet_ptr->gather_documentation();
            auto help_window = HelpWindow::the(window());
            help_window->set_docs(move(docs));
            help_window->show();
        }
    };

    auto& cell_value_editor = top_bar.add<GUI::TextEditor>(GUI::TextEditor::Type::SingleLine);
    cell_value_editor.set_font(Gfx::FontDatabase::default_fixed_width_font());
    cell_value_editor.set_scrollbars_enabled(false);

    cell_value_editor.on_return_pressed = [this]() {
        m_selected_view->move_cursor(GUI::AbstractView::CursorMovement::Down);
    };

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
    inline_widget.set_layout<GUI::VerticalBoxLayout>().set_margins(4);
    inline_widget.set_frame_shape(Gfx::FrameShape::Box);
    m_inline_documentation_label = inline_widget.add<GUI::Label>();
    m_inline_documentation_label->set_fill_with_background_color(true);
    m_inline_documentation_label->set_autosize(false);
    m_inline_documentation_label->set_text_alignment(Gfx::TextAlignment::CenterLeft);

    if (!m_workbook->has_sheets() && should_add_sheet_if_empty)
        m_workbook->add_sheet("Sheet 1");

    m_tab_context_menu = GUI::Menu::construct();
    m_rename_action = GUI::Action::create("Rename...", Gfx::Bitmap::try_load_from_file("/res/icons/16x16/rename.png").release_value_but_fixme_should_propagate_errors(), [this](auto&) {
        VERIFY(m_tab_context_menu_sheet_view);

        auto* sheet_ptr = m_tab_context_menu_sheet_view->sheet_if_available();
        VERIFY(sheet_ptr); // How did we get here without a sheet?
        auto& sheet = *sheet_ptr;
        String new_name;
        if (GUI::InputBox::show(window(), new_name, String::formatted("New name for '{}'", sheet.name()), "Rename sheet") == GUI::Dialog::ExecOK) {
            sheet.set_name(new_name);
            sheet.update();
            m_tab_widget->set_tab_title(static_cast<GUI::Widget&>(*m_tab_context_menu_sheet_view), new_name);
        }
    });
    m_tab_context_menu->add_action(*m_rename_action);
    m_tab_context_menu->add_action(GUI::Action::create("Add new sheet...", Gfx::Bitmap::try_load_from_file("/res/icons/16x16/new-tab.png").release_value_but_fixme_should_propagate_errors(), [this](auto&) {
        String name;
        if (GUI::InputBox::show(window(), name, "Name for new sheet", "Create sheet") == GUI::Dialog::ExecOK) {
            NonnullRefPtrVector<Sheet> new_sheets;
            new_sheets.append(m_workbook->add_sheet(name));
            setup_tabs(move(new_sheets));
        }
    }));

    setup_tabs(m_workbook->sheets());

    m_new_action = GUI::Action::create("Add New Sheet", Gfx::Bitmap::try_load_from_file("/res/icons/16x16/new-tab.png").release_value_but_fixme_should_propagate_errors(), [&](auto&) {
        add_sheet();
    });

    m_open_action = GUI::CommonActions::make_open_action([&](auto&) {
        Optional<String> load_path = GUI::FilePicker::get_open_filepath(window());
        if (!load_path.has_value())
            return;

        auto response = FileSystemAccessClient::Client::the().try_request_file_read_only_approved(window(), *load_path);
        if (response.is_error())
            return;
        load_file(*response.value());
    });

    m_save_action = GUI::CommonActions::make_save_action([&](auto&) {
        if (current_filename().is_empty()) {
            String name = "workbook";
            Optional<String> save_path = GUI::FilePicker::get_save_filepath(window(), name, "sheets");
            if (!save_path.has_value())
                return;

            save(save_path.value());
        } else {
            save(current_filename());
        }
    });

    m_save_as_action = GUI::CommonActions::make_save_as_action([&](auto&) {
        String name = "workbook";
        Optional<String> save_path = GUI::FilePicker::get_save_filepath(window(), name, "sheets");
        if (!save_path.has_value())
            return;

        save(save_path.value());

        if (!current_filename().is_empty())
            set_filename(current_filename());
    });

    m_quit_action = GUI::CommonActions::make_quit_action([&](auto&) {
        if (!request_close())
            return;
        GUI::Application::the()->quit(0);
    });

    m_cut_action = GUI::CommonActions::make_cut_action([&](auto&) { clipboard_action(true); }, window());
    m_copy_action = GUI::CommonActions::make_copy_action([&](auto&) { clipboard_action(false); }, window());
    m_paste_action = GUI::CommonActions::make_paste_action([&](auto&) {
        ScopeGuard update_after_paste { [&] { update(); } };

        auto* worksheet_ptr = current_worksheet_if_available();
        if (!worksheet_ptr) {
            GUI::MessageBox::show_error(window(), "There are no active worksheets");
            return;
        }
        auto& sheet = *worksheet_ptr;
        auto& cells = sheet.selected_cells();
        VERIFY(!cells.is_empty());
        const auto& data = GUI::Clipboard::the().fetch_data_and_type();
        if (auto spreadsheet_data = data.metadata.get("text/x-spreadsheet-data"); spreadsheet_data.has_value()) {
            Vector<Spreadsheet::Position> source_positions, target_positions;
            auto lines = spreadsheet_data.value().split_view('\n');
            auto action = lines.take_first();

            for (auto& line : lines) {
                dbgln("Paste line '{}'", line);
                auto position = sheet.position_from_url(line);
                if (position.has_value())
                    source_positions.append(position.release_value());
            }

            for (auto& position : sheet.selected_cells())
                target_positions.append(position);

            if (source_positions.is_empty())
                return;

            auto first_position = source_positions.take_first();
            sheet.copy_cells(move(source_positions), move(target_positions), first_position, action == "cut" ? Spreadsheet::Sheet::CopyOperation::Cut : Spreadsheet::Sheet::CopyOperation::Copy);
        } else {
            for (auto& cell : sheet.selected_cells())
                sheet.ensure(cell).set_data(StringView { data.data.data(), data.data.size() });
            update();
        }
    },
        window());

    m_functions_help_action = GUI::Action::create(
        "&Functions Help", Gfx::Bitmap::try_load_from_file("/res/icons/16x16/app-help.png").release_value_but_fixme_should_propagate_errors(), [&](auto&) {
            if (auto* worksheet_ptr = current_worksheet_if_available()) {
                auto docs = worksheet_ptr->gather_documentation();
                auto help_window = Spreadsheet::HelpWindow::the(window());
                help_window->set_docs(move(docs));
                help_window->show();
            } else {
                GUI::MessageBox::show_error(window(), "Cannot prepare documentation/help without an active worksheet");
            }
        },
        window());

    m_about_action = GUI::CommonActions::make_about_action("Spreadsheet", GUI::Icon::default_icon("app-spreadsheet"), window());

    toolbar.add_action(*m_new_action);
    toolbar.add_action(*m_open_action);
    toolbar.add_action(*m_save_action);
    toolbar.add_separator();
    toolbar.add_action(*m_cut_action);
    toolbar.add_action(*m_copy_action);
    toolbar.add_action(*m_paste_action);
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
        }
        m_selected_view = &static_cast<SpreadsheetView&>(selected_widget);
        m_selected_view->on_selection_changed = [&](Vector<Position>&& selection) {
            auto* sheet_ptr = m_selected_view->sheet_if_available();
            // How did this even happen?
            VERIFY(sheet_ptr);
            auto& sheet = *sheet_ptr;
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
                m_current_cell_label->set_enabled(true);
                m_current_cell_label->set_text(position.to_cell_identifier(sheet));

                auto& cell = sheet.ensure(position);
                m_cell_value_editor->on_change = nullptr;
                m_cell_value_editor->set_text(cell.source());
                m_cell_value_editor->on_change = [&] {
                    auto text = m_cell_value_editor->text();
                    // FIXME: Lines?
                    auto offset = m_cell_value_editor->cursor().column();
                    try_generate_tip_for_input_expression(text, offset);
                    cell.set_data(move(text));
                    sheet.update();
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

            Vector<Cell&> cells;
            for (auto& position : selection)
                cells.append(sheet.ensure(position));

            auto& first_cell = cells.first();
            m_cell_value_editor->on_change = nullptr;
            m_cell_value_editor->set_text("");
            m_should_change_selected_cells = false;
            m_cell_value_editor->on_focusin = [this] { m_should_change_selected_cells = true; };
            m_cell_value_editor->on_focusout = [this] { m_should_change_selected_cells = false; };
            m_cell_value_editor->on_change = [cells = move(cells), this]() mutable {
                if (m_should_change_selected_cells) {
                    auto* sheet_ptr = m_selected_view->sheet_if_available();
                    if (!sheet_ptr)
                        return;
                    auto& sheet = *sheet_ptr;
                    auto text = m_cell_value_editor->text();
                    // FIXME: Lines?
                    auto offset = m_cell_value_editor->cursor().column();
                    try_generate_tip_for_input_expression(text, offset);
                    for (auto& cell : cells)
                        cell.set_data(text);
                    sheet.update();
                    update();
                }
            };
            m_cell_value_editor->set_enabled(true);
            static_cast<CellSyntaxHighlighter*>(const_cast<Syntax::Highlighter*>(m_cell_value_editor->syntax_highlighter()))->set_cell(&first_cell);
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
        m_tab_context_menu_sheet_view = static_cast<SpreadsheetView&>(widget);
        m_tab_context_menu->popup(event.screen_position());
    };

    m_tab_widget->on_double_click = [&](auto& widget) {
        m_tab_context_menu_sheet_view = static_cast<SpreadsheetView&>(widget);
        VERIFY(m_tab_context_menu_sheet_view);

        auto* sheet_ptr = m_tab_context_menu_sheet_view->sheet_if_available();
        VERIFY(sheet_ptr); // How did we get here without a sheet?
        auto& sheet = *sheet_ptr;
        String new_name;
        if (GUI::InputBox::show(window(), new_name, String::formatted("New name for '{}'", sheet.name()), "Rename sheet") == GUI::Dialog::ExecOK) {
            sheet.set_name(new_name);
            sheet.update();
            m_tab_widget->set_tab_title(static_cast<GUI::Widget&>(*m_tab_context_menu_sheet_view), new_name);
        }
    };
}

void SpreadsheetWidget::try_generate_tip_for_input_expression(StringView source, size_t cursor_offset)
{
    auto* sheet_ptr = m_selected_view->sheet_if_available();
    if (!sheet_ptr)
        return;

    auto& sheet = *sheet_ptr;

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
    auto text = sheet.generate_inline_documentation_for(name, index);
    if (text.is_empty()) {
        m_inline_documentation_window->hide();
    } else {
        m_inline_documentation_label->set_text(move(text));
        m_inline_documentation_window->show();
    }
}

void SpreadsheetWidget::save(StringView filename)
{
    auto result = m_workbook->save(filename);
    if (result.is_error())
        GUI::MessageBox::show_error(window(), result.error());
}

void SpreadsheetWidget::load_file(Core::File& file)
{
    auto result = m_workbook->open_file(file);
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
    m_workbook->sheets().extend(new_sheets);
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

void SpreadsheetWidget::clipboard_action(bool is_cut)
{
    /// text/x-spreadsheet-data:
    /// - action: copy/cut
    /// - currently selected cell
    /// - selected cell+
    auto* worksheet_ptr = current_worksheet_if_available();
    if (!worksheet_ptr) {
        GUI::MessageBox::show_error(window(), "There are no active worksheets");
        return;
    }
    auto& worksheet = *worksheet_ptr;
    auto& cells = worksheet.selected_cells();
    VERIFY(!cells.is_empty());
    StringBuilder text_builder, url_builder;
    url_builder.append(is_cut ? "cut\n" : "copy\n");
    bool first = true;
    auto cursor = current_selection_cursor();
    if (cursor) {
        Spreadsheet::Position position { (size_t)cursor->column(), (size_t)cursor->row() };
        url_builder.append(position.to_url(worksheet).to_string());
        url_builder.append('\n');
    }

    for (auto& cell : cells) {
        if (first && !cursor) {
            url_builder.append(cell.to_url(worksheet).to_string());
            url_builder.append('\n');
        }

        url_builder.append(cell.to_url(worksheet).to_string());
        url_builder.append('\n');

        auto cell_data = worksheet.at(cell);
        if (!first)
            text_builder.append('\t');
        if (cell_data)
            text_builder.append(cell_data->data());
        first = false;
    }
    HashMap<String, String> metadata;
    metadata.set("text/x-spreadsheet-data", url_builder.to_string());
    dbgln(url_builder.to_string());

    GUI::Clipboard::the().set_data(text_builder.string_view().bytes(), "text/plain", move(metadata));
}

void SpreadsheetWidget::initialize_menubar(GUI::Window& window)
{
    auto& file_menu = window.add_menu("&File");
    file_menu.add_action(*m_new_action);
    file_menu.add_action(*m_open_action);
    file_menu.add_action(*m_save_action);
    file_menu.add_action(*m_save_as_action);
    file_menu.add_separator();
    file_menu.add_action(*m_quit_action);

    auto& edit_menu = window.add_menu("&Edit");
    edit_menu.add_action(*m_cut_action);
    edit_menu.add_action(*m_copy_action);
    edit_menu.add_action(*m_paste_action);

    auto& help_menu = window.add_menu("&Help");
    help_menu.add_action(*m_functions_help_action);
    help_menu.add_action(*m_about_action);
}
}
