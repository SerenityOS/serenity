/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SpreadsheetWidget.h"
#include "CellSyntaxHighlighter.h"
#include "HelpWindow.h"
#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/ColorPicker.h>
#include <LibGUI/EmojiInputDialog.h>
#include <LibGUI/InputBox.h>
#include <LibGUI/Label.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Splitter.h>
#include <LibGUI/TabWidget.h>
#include <LibGUI/TextEditor.h>
#include <LibGUI/Toolbar.h>
#include <LibGUI/ToolbarContainer.h>
#include <LibGfx/Font/FontDatabase.h>
#include <string.h>

namespace Spreadsheet {

SpreadsheetWidget::SpreadsheetWidget(GUI::Window& parent_window, Vector<NonnullRefPtr<Sheet>>&& sheets, bool should_add_sheet_if_empty)
    : m_workbook(make<Workbook>(move(sheets), parent_window))
{
    set_fill_with_background_color(true);
    set_layout<GUI::VerticalBoxLayout>(2);

    auto& toolbar_container = add<GUI::ToolbarContainer>();
    auto& toolbar = toolbar_container.add<GUI::Toolbar>();

    auto& container = add<GUI::VerticalSplitter>();

    auto& top_bar = container.add<GUI::Frame>();
    top_bar.set_layout<GUI::HorizontalBoxLayout>(GUI::Margins {}, 1);
    top_bar.set_preferred_height(26);
    auto& current_cell_label = top_bar.add<GUI::Label>();
    current_cell_label.set_fixed_width(50);

    auto& help_button = top_bar.add<GUI::Button>();
    help_button.set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-help.png"sv).release_value_but_fixme_should_propagate_errors());
    help_button.set_tooltip("Functions Help"_string);
    help_button.set_fixed_size(20, 20);
    help_button.on_click = [&](auto) {
        if (!current_view()) {
            GUI::MessageBox::show_error(window(), "Can only show function documentation/help when a worksheet exists and is open"sv);
        } else if (auto* sheet_ptr = current_worksheet_if_available()) {
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
        current_view()->move_cursor(GUI::AbstractView::CursorMovement::Down);
    };

    cell_value_editor.set_syntax_highlighter(make<CellSyntaxHighlighter>());
    cell_value_editor.set_enabled(false);
    current_cell_label.set_enabled(false);

    m_tab_widget = container.add<GUI::TabWidget>();
    m_tab_widget->set_tab_position(TabPosition::Bottom);

    m_cell_value_editor = cell_value_editor;
    m_current_cell_label = current_cell_label;
    m_inline_documentation_window = GUI::Window::construct(window());
    m_inline_documentation_window->set_rect(m_cell_value_editor->rect().translated(0, m_cell_value_editor->height() + 7).inflated(6, 6));
    m_inline_documentation_window->set_window_type(GUI::WindowType::Tooltip);
    m_inline_documentation_window->set_resizable(false);
    auto inline_widget = m_inline_documentation_window->set_main_widget<GUI::Frame>();
    inline_widget->set_fill_with_background_color(true);
    inline_widget->set_layout<GUI::VerticalBoxLayout>(4);
    inline_widget->set_frame_style(Gfx::FrameStyle::Plain);
    m_inline_documentation_label = inline_widget->add<GUI::Label>();
    m_inline_documentation_label->set_fill_with_background_color(true);
    m_inline_documentation_label->set_autosize(false);
    m_inline_documentation_label->set_text_alignment(Gfx::TextAlignment::CenterLeft);

    if (!m_workbook->has_sheets() && should_add_sheet_if_empty)
        m_workbook->add_sheet("Sheet 1"sv);

    m_tab_context_menu = GUI::Menu::construct();
    m_rename_action = GUI::CommonActions::make_rename_action([this](auto&) {
        VERIFY(m_tab_context_menu_sheet_view);

        auto* sheet_ptr = m_tab_context_menu_sheet_view->sheet_if_available();
        VERIFY(sheet_ptr); // How did we get here without a sheet?
        auto& sheet = *sheet_ptr;
        String new_name = String::from_byte_string(sheet.name()).release_value_but_fixme_should_propagate_errors();
        if (GUI::InputBox::show(window(), new_name, {}, "Rename Sheet"sv, GUI::InputType::NonemptyText, "Name"sv) == GUI::Dialog::ExecResult::OK) {
            sheet.set_name(new_name);
            sheet.update();
            m_tab_widget->set_tab_title(static_cast<GUI::Widget&>(*m_tab_context_menu_sheet_view), new_name);
        }
    });
    m_tab_context_menu->add_action(*m_rename_action);
    m_tab_context_menu->add_action(GUI::Action::create("Add New Sheet...", Gfx::Bitmap::load_from_file("/res/icons/16x16/new-tab.png"sv).release_value_but_fixme_should_propagate_errors(), [this](auto&) {
        String name;
        auto icon = Gfx::Bitmap::load_from_file("/res/icons/32x32/filetype-spreadsheet.png"sv).release_value_but_fixme_should_propagate_errors();
        if (GUI::InputBox::show(window(), name, "Enter a name:"sv, "New sheet"sv, GUI::InputType::NonemptyText, {}, move(icon)) == GUI::Dialog::ExecResult::OK) {
            Vector<NonnullRefPtr<Sheet>> new_sheets;
            new_sheets.append(m_workbook->add_sheet(name));
            setup_tabs(move(new_sheets));
        }
    }));

    setup_tabs(m_workbook->sheets());

    m_new_action = GUI::Action::create("Add New Sheet", Gfx::Bitmap::load_from_file("/res/icons/16x16/new-tab.png"sv).release_value_but_fixme_should_propagate_errors(), [&](auto&) {
        add_sheet();
    });

    m_open_action = GUI::CommonActions::make_open_action([&](auto&) {
        if (!request_close())
            return;

        FileSystemAccessClient::OpenFileOptions options {
            .allowed_file_types = Vector {
                { "Spreadsheets", { { "sheets", "csv" } } },
                GUI::FileTypeFilter::all_files(),
            },
        };
        auto response = FileSystemAccessClient::Client::the().open_file(window(), options);
        if (response.is_error())
            return;
        load_file(response.value().filename(), response.value().stream());
    });

    m_import_action = GUI::Action::create("Import Sheets...", [&](auto&) {
        FileSystemAccessClient::OpenFileOptions options {
            .allowed_file_types = Vector {
                { "Spreadsheets", { { "sheets", "csv" } } },
                GUI::FileTypeFilter::all_files(),
            },
        };
        auto response = FileSystemAccessClient::Client::the().open_file(window(), options);
        if (response.is_error())
            return;

        import_sheets(response.value().filename(), response.value().stream());
    });

    m_save_action = GUI::CommonActions::make_save_action([&](auto&) {
        if (current_filename().is_empty()) {
            m_save_as_action->activate();
            return;
        }

        auto response = FileSystemAccessClient::Client::the().request_file(window(), current_filename(), Core::File::OpenMode::Write);
        if (response.is_error())
            return;
        save(response.value().filename(), response.value().stream());
    });

    m_save_as_action = GUI::CommonActions::make_save_as_action([&](auto&) {
        ByteString name = "workbook";
        auto response = FileSystemAccessClient::Client::the().save_file(window(), name, "sheets");
        if (response.is_error())
            return;
        save(response.value().filename(), response.value().stream());
        update_window_title();
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
            GUI::MessageBox::show_error(window(), "There are no active worksheets"sv);
            return;
        }
        auto& sheet = *worksheet_ptr;
        auto& cells = sheet.selected_cells();
        VERIFY(!cells.is_empty());
        auto const& data = GUI::Clipboard::the().fetch_data_and_type();
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
            auto cell_changes = sheet.copy_cells(move(source_positions), move(target_positions), first_position, action == "cut" ? Spreadsheet::Sheet::CopyOperation::Cut : Spreadsheet::Sheet::CopyOperation::Copy);
            undo_stack().push(make<CellsUndoCommand>(cell_changes));
        } else {
            for (auto& cell : sheet.selected_cells())
                sheet.ensure(cell).set_data(StringView { data.data.data(), data.data.size() });
            update();
        }
    },
        window());

    m_insert_emoji_action = GUI::CommonActions::make_insert_emoji_action([&](auto&) {
        auto emoji_input_dialog = GUI::EmojiInputDialog::construct(window());
        if (emoji_input_dialog->exec() != GUI::EmojiInputDialog::ExecResult::OK)
            return;

        auto emoji_code_point = emoji_input_dialog->selected_emoji_text();

        if (m_cell_value_editor->has_focus_within()) {
            m_cell_value_editor->insert_at_cursor_or_replace_selection(emoji_code_point);
        }

        auto* worksheet_ptr = current_worksheet_if_available();
        if (!worksheet_ptr) {
            GUI::MessageBox::show_error(window(), "There are no active worksheets"sv);
            return;
        }
        auto& sheet = *worksheet_ptr;
        for (auto& cell : sheet.selected_cells())
            sheet.ensure(cell).set_data(emoji_code_point);

        update();
    },
        window());

    m_undo_action = GUI::CommonActions::make_undo_action([&](auto&) {
        undo();
    });

    m_redo_action = GUI::CommonActions::make_redo_action([&](auto&) {
        redo();
    });

    m_undo_stack.on_state_change = [this] {
        m_undo_action->set_enabled(m_undo_stack.can_undo());
        m_redo_action->set_enabled(m_undo_stack.can_redo());
    };

    m_undo_action->set_enabled(false);
    m_redo_action->set_enabled(false);

    m_change_background_color_action = GUI::Action::create(
        "&Change Background Color", { Mod_Ctrl, Key_B }, Gfx::Bitmap::load_from_file("/res/icons/pixelpaint/bucket.png"sv).release_value_but_fixme_should_propagate_errors(), [&](auto&) {
            change_cell_static_color_format(Spreadsheet::FormatType::Background);
        },
        window());

    m_change_foreground_color_action = GUI::Action::create(
        "&Change Foreground Color", { Mod_Ctrl, Key_T }, Gfx::Bitmap::load_from_file("/res/icons/16x16/text-color.png"sv).release_value_but_fixme_should_propagate_errors(), [&](auto&) {
            change_cell_static_color_format(Spreadsheet::FormatType::Foreground);
        },
        window());

    m_change_background_color_action->set_enabled(false);
    m_change_foreground_color_action->set_enabled(false);

    m_functions_help_action = GUI::Action::create(
        "&Functions Help", Gfx::Bitmap::load_from_file("/res/icons/16x16/app-help.png"sv).release_value_but_fixme_should_propagate_errors(), [&](auto&) {
            if (auto* worksheet_ptr = current_worksheet_if_available()) {
                auto docs = worksheet_ptr->gather_documentation();
                auto help_window = Spreadsheet::HelpWindow::the(window());
                help_window->set_docs(move(docs));
                help_window->show();
            } else {
                GUI::MessageBox::show_error(window(), "Cannot prepare documentation/help without an active worksheet"sv);
            }
        },
        window());

    m_search_action = GUI::CommonActions::make_command_palette_action(&parent_window);

    m_about_action = GUI::CommonActions::make_about_action("Spreadsheet"_string, GUI::Icon::default_icon("app-spreadsheet"sv), &parent_window);

    toolbar.add_action(*m_new_action);
    toolbar.add_action(*m_open_action);
    toolbar.add_action(*m_save_action);
    toolbar.add_separator();
    toolbar.add_action(*m_cut_action);
    toolbar.add_action(*m_copy_action);
    toolbar.add_action(*m_paste_action);
    toolbar.add_action(*m_undo_action);
    toolbar.add_action(*m_redo_action);
    toolbar.add_separator();
    toolbar.add_action(*m_change_background_color_action);
    toolbar.add_action(*m_change_foreground_color_action);

    m_cut_action->set_enabled(false);
    m_copy_action->set_enabled(false);
    m_paste_action->set_enabled(false);
    m_insert_emoji_action->set_enabled(false);

    m_tab_widget->on_change = [this](auto& selected_widget) {
        // for keyboard shortcuts and command palette
        m_tab_context_menu_sheet_view = static_cast<SpreadsheetView&>(selected_widget);
    };

    m_tab_widget->on_context_menu_request = [&](auto& widget, auto& event) {
        m_tab_context_menu_sheet_view = static_cast<SpreadsheetView&>(widget);
        m_tab_context_menu->popup(event.screen_position());
    };

    m_tab_widget->on_double_click = [&](auto& widget) {
        m_tab_context_menu_sheet_view = static_cast<SpreadsheetView&>(widget);
        m_rename_action->activate();
    };
}

void SpreadsheetWidget::resize_event(GUI::ResizeEvent& event)
{
    GUI::Widget::resize_event(event);
    if (m_inline_documentation_window && m_cell_value_editor && window())
        m_inline_documentation_window->set_rect(m_cell_value_editor->screen_relative_rect().translated(0, m_cell_value_editor->height() + 7).inflated(6, 6));
}

void SpreadsheetWidget::clipboard_content_did_change(ByteString const& mime_type)
{
    if (auto* sheet = current_worksheet_if_available())
        m_paste_action->set_enabled(!sheet->selected_cells().is_empty() && mime_type.starts_with("text/"sv));
}

void SpreadsheetWidget::setup_tabs(Vector<NonnullRefPtr<Sheet>> new_sheets)
{
    for (auto& sheet : new_sheets) {
        auto& new_view = m_tab_widget->add_tab<SpreadsheetView>(String::from_byte_string(sheet->name()).release_value_but_fixme_should_propagate_errors(), sheet);
        new_view.model()->on_cell_data_change = [&](auto& cell, auto& previous_data) {
            undo_stack().push(make<CellsUndoCommand>(cell, previous_data));
            window()->set_modified(true);
        };
        new_view.model()->on_cells_data_change = [&](Vector<CellChange> cell_changes) {
            undo_stack().push(make<CellsUndoCommand>(cell_changes));
            window()->set_modified(true);
        };
        new_view.on_selection_changed = [&](Vector<Position>&& selection) {
            auto* sheet_ptr = current_worksheet_if_available();
            // How did this even happen?
            VERIFY(sheet_ptr);
            auto& sheet = *sheet_ptr;

            VERIFY(!selection.is_empty());
            m_cut_action->set_enabled(true);
            m_copy_action->set_enabled(true);
            m_paste_action->set_enabled(GUI::Clipboard::the().fetch_mime_type().starts_with("text/"sv));
            m_insert_emoji_action->set_enabled(true);
            m_current_cell_label->set_enabled(true);
            m_cell_value_editor->set_enabled(true);
            m_change_background_color_action->set_enabled(true);
            m_change_foreground_color_action->set_enabled(true);

            if (selection.size() == 1) {
                auto& position = selection.first();
                m_current_cell_label->set_text(String::from_byte_string(position.to_cell_identifier(sheet)).release_value_but_fixme_should_propagate_errors());

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
                static_cast<CellSyntaxHighlighter*>(const_cast<Syntax::Highlighter*>(m_cell_value_editor->syntax_highlighter()))->set_cell(&cell);
                return;
            }

            // There are many cells selected, change all of them.
            StringBuilder builder;
            builder.appendff("<{}>", selection.size());
            m_current_cell_label->set_text(builder.to_string().release_value_but_fixme_should_propagate_errors());

            Vector<Cell&> cells;
            for (auto& position : selection)
                cells.append(sheet.ensure(position));

            auto& first_cell = cells.first();
            m_cell_value_editor->on_change = nullptr;
            m_cell_value_editor->set_text(""sv);
            m_should_change_selected_cells = false;
            m_cell_value_editor->on_focusin = [this] { m_should_change_selected_cells = true; };
            m_cell_value_editor->on_focusout = [this] { m_should_change_selected_cells = false; };
            m_cell_value_editor->on_change = [cells = move(cells), this]() mutable {
                if (m_should_change_selected_cells) {
                    auto* sheet_ptr = current_worksheet_if_available();
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
            static_cast<CellSyntaxHighlighter*>(const_cast<Syntax::Highlighter*>(m_cell_value_editor->syntax_highlighter()))->set_cell(&first_cell);
        };
        new_view.on_selection_dropped = [&]() {
            m_current_cell_label->set_enabled(false);
            m_current_cell_label->set_text({});
            m_cell_value_editor->on_change = nullptr;
            m_cell_value_editor->on_focusin = nullptr;
            m_cell_value_editor->on_focusout = nullptr;
            m_cell_value_editor->set_text({});
            m_cell_value_editor->set_enabled(false);

            m_cut_action->set_enabled(false);
            m_copy_action->set_enabled(false);
            m_paste_action->set_enabled(false);
            m_insert_emoji_action->set_enabled(false);

            static_cast<CellSyntaxHighlighter*>(const_cast<Syntax::Highlighter*>(m_cell_value_editor->syntax_highlighter()))->set_cell(nullptr);
        };
    }
}

void SpreadsheetWidget::try_generate_tip_for_input_expression(StringView source, size_t cursor_offset)
{
    auto* sheet_ptr = current_view()->sheet_if_available();
    if (!sheet_ptr)
        return;

    auto& sheet = *sheet_ptr;

    m_inline_documentation_window->set_rect(m_cell_value_editor->screen_relative_rect().translated(0, m_cell_value_editor->height() + 7).inflated(6, 6));
    if (!current_view() || !source.starts_with('=')) {
        m_inline_documentation_window->hide();
        return;
    }
    cursor_offset = min(cursor_offset, source.length());
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
        m_inline_documentation_label->set_text(String::from_byte_string(text).release_value_but_fixme_should_propagate_errors());
        m_inline_documentation_window->show();
    }
}

void SpreadsheetWidget::undo()
{
    if (!m_undo_stack.can_undo())
        return;

    m_undo_stack.undo();
    update();
}

void SpreadsheetWidget::redo()
{
    if (!m_undo_stack.can_redo())
        return;

    m_undo_stack.redo();
    update();
}

void SpreadsheetWidget::change_cell_static_color_format(Spreadsheet::FormatType format_type)
{
    VERIFY(current_worksheet_if_available());

    auto preview_color_in_selected_cells = [this, format_type](Gfx::Color color) {
        for (auto& position : current_worksheet_if_available()->selected_cells()) {
            auto* cell = current_worksheet_if_available()->at(position);
            auto previous_type_metadata = cell->type_metadata();
            if (format_type == Spreadsheet::FormatType::Background)
                cell->type_metadata().static_format.background_color = color;
            else
                cell->type_metadata().static_format.foreground_color = color;
            update();
        }
    };
    auto apply_color_to_selected_cells = [this, format_type](Gfx::Color color) {
        Vector<CellChange> cell_changes;
        for (auto& position : current_worksheet_if_available()->selected_cells()) {
            auto* cell = current_worksheet_if_available()->at(position);
            auto previous_type_metadata = cell->type_metadata();
            if (format_type == Spreadsheet::FormatType::Background)
                cell->type_metadata().static_format.background_color = color;
            else
                cell->type_metadata().static_format.foreground_color = color;
            cell_changes.append(CellChange(*cell, previous_type_metadata));
        }
        undo_stack().push(make<CellsUndoMetadataCommand>(move(cell_changes)));
        window()->set_modified(true);
    };
    auto get_selection_color = [this, format_type](void) {
        // FIXME: Not sure what to do if a selection of multiple cells has more than one color.
        //        For now we just grab the first one we see and pass that to GUI::ColorPicker
        for (auto& position : current_worksheet_if_available()->selected_cells()) {
            auto* cell = current_worksheet_if_available()->at(position);
            auto previous_type_metadata = cell->type_metadata();
            if (format_type == Spreadsheet::FormatType::Background)
                return cell->type_metadata().static_format.background_color.value_or(Color::White);
            else
                return cell->type_metadata().static_format.foreground_color.value_or(Color::White);
        }
        return Color(Color::White);
    };

    // FIXME: Hack, we want to restore the cell metadata to the actual state before computing the change
    auto get_current_selection_metadata = [this](void) {
        Vector<CellTypeMetadata> cell_metadata;
        for (auto& position : current_worksheet_if_available()->selected_cells()) {
            auto* cell = current_worksheet_if_available()->at(position);
            cell_metadata.append(cell->type_metadata());
        }
        return cell_metadata;
    };
    auto restore_current_selection_metadata = [this](Vector<CellTypeMetadata> metadata) {
        for (auto& position : current_worksheet_if_available()->selected_cells()) {
            auto* cell = current_worksheet_if_available()->at(position);
            cell->type_metadata() = metadata.take_first();
        }
    };

    auto dialog = GUI::ColorPicker::construct(get_selection_color(), window(), "Select Color");
    dialog->on_color_changed = [&preview_color_in_selected_cells](Gfx::Color color) {
        preview_color_in_selected_cells(color);
    };
    Vector<CellTypeMetadata> preserved_state = get_current_selection_metadata();
    auto result = dialog->exec();
    restore_current_selection_metadata(preserved_state);
    if (result == GUI::Dialog::ExecResult::OK)
        apply_color_to_selected_cells(dialog->color());
}

void SpreadsheetWidget::save(ByteString const& filename, Core::File& file)
{
    auto result = m_workbook->write_to_file(filename, file);
    if (result.is_error()) {
        GUI::MessageBox::show_error(window(), ByteString::formatted("Cannot save file: {}", result.error()));
        return;
    }
    undo_stack().set_current_unmodified();
    window()->set_modified(false);
    GUI::Application::the()->set_most_recently_open_file(filename);
}

void SpreadsheetWidget::load_file(ByteString const& filename, Core::File& file)
{
    auto result = m_workbook->open_file(filename, file);
    if (result.is_error()) {
        GUI::MessageBox::show_error(window(), result.error());
        if (!m_workbook->has_sheets()) {
            add_sheet();
        }
        return;
    }

    m_cell_value_editor->on_change = nullptr;
    m_current_cell_label->set_text({});
    m_should_change_selected_cells = false;
    while (auto* widget = m_tab_widget->active_widget()) {
        m_tab_widget->remove_tab(*widget);
    }

    setup_tabs(m_workbook->sheets());
    update_window_title();
    GUI::Application::the()->set_most_recently_open_file(filename);
}

void SpreadsheetWidget::import_sheets(ByteString const& filename, Core::File& file)
{
    auto result = m_workbook->import_file(filename, file);
    if (result.is_error()) {
        GUI::MessageBox::show_error(window(), result.error());
        return;
    }

    if (!result.value())
        return;

    window()->set_modified(true);

    m_cell_value_editor->on_change = nullptr;
    m_current_cell_label->set_text({});
    m_should_change_selected_cells = false;
    while (auto* widget = m_tab_widget->active_widget()) {
        m_tab_widget->remove_tab(*widget);
    }

    setup_tabs(m_workbook->sheets());
    update_window_title();
}

bool SpreadsheetWidget::request_close()
{
    if (!undo_stack().is_current_modified())
        return true;

    auto result = GUI::MessageBox::ask_about_unsaved_changes(window(), current_filename());
    if (result == GUI::MessageBox::ExecResult::Yes) {
        m_save_action->activate();
        return !m_workbook->dirty();
    }

    if (result == GUI::MessageBox::ExecResult::No)
        return true;

    return false;
}

void SpreadsheetWidget::add_sheet()
{
    StringBuilder name;
    name.append("Sheet"sv);
    name.appendff(" {}", m_workbook->sheets().size() + 1);

    Vector<NonnullRefPtr<Sheet>> new_sheets;
    new_sheets.append(m_workbook->add_sheet(name.string_view()));
    setup_tabs(move(new_sheets));
}

void SpreadsheetWidget::add_sheet(NonnullRefPtr<Sheet>&& sheet)
{
    VERIFY(m_workbook == &sheet->workbook());

    Vector<NonnullRefPtr<Sheet>> new_sheets;
    new_sheets.append(move(sheet));
    m_workbook->sheets().extend(new_sheets);
    setup_tabs(new_sheets);
}

void SpreadsheetWidget::update_window_title()
{
    StringBuilder builder;
    if (current_filename().is_empty())
        builder.append("Untitled"sv);
    else
        builder.append(current_filename());
    builder.append("[*] - Spreadsheet"sv);

    window()->set_title(builder.to_byte_string());
}

void SpreadsheetWidget::clipboard_action(bool is_cut)
{
    /// text/x-spreadsheet-data:
    /// - action: copy/cut
    /// - currently selected cell
    /// - selected cell+
    auto* worksheet_ptr = current_worksheet_if_available();
    if (!worksheet_ptr) {
        GUI::MessageBox::show_error(window(), "There are no active worksheets"sv);
        return;
    }
    auto& worksheet = *worksheet_ptr;
    auto& cells = worksheet.selected_cells();
    VERIFY(!cells.is_empty());
    StringBuilder text_builder, url_builder;
    url_builder.append(is_cut ? "cut\n"sv : "copy\n"sv);
    bool first = true;
    auto cursor = current_selection_cursor();
    if (cursor) {
        Spreadsheet::Position position { (size_t)cursor->column(), (size_t)cursor->row() };
        url_builder.append(position.to_url(worksheet).to_byte_string());
        url_builder.append('\n');
    }

    for (auto& cell : cells) {
        if (first && !cursor) {
            url_builder.append(cell.to_url(worksheet).to_byte_string());
            url_builder.append('\n');
        }

        url_builder.append(cell.to_url(worksheet).to_byte_string());
        url_builder.append('\n');

        auto cell_data = worksheet.at(cell);
        if (!first)
            text_builder.append('\t');
        if (cell_data)
            text_builder.append(cell_data->data());
        first = false;
    }
    HashMap<ByteString, ByteString> metadata;
    metadata.set("text/x-spreadsheet-data", url_builder.to_byte_string());
    dbgln(url_builder.to_byte_string());

    GUI::Clipboard::the().set_data(text_builder.string_view().bytes(), "text/plain", move(metadata));
}

ErrorOr<void> SpreadsheetWidget::initialize_menubar(GUI::Window& window)
{
    auto file_menu = window.add_menu("&File"_string);
    file_menu->add_action(*m_new_action);
    file_menu->add_action(*m_open_action);
    file_menu->add_action(*m_save_action);
    file_menu->add_action(*m_save_as_action);
    file_menu->add_separator();
    file_menu->add_action(*m_import_action);
    file_menu->add_separator();
    file_menu->add_recent_files_list([&](auto& action) {
        if (!request_close())
            return;

        auto response = FileSystemAccessClient::Client::the().request_file_read_only_approved(&window, action.text());
        if (response.is_error())
            return;
        load_file(response.value().filename(), response.value().stream());
    });
    file_menu->add_action(*m_quit_action);

    auto edit_menu = window.add_menu("&Edit"_string);
    edit_menu->add_action(*m_undo_action);
    edit_menu->add_action(*m_redo_action);
    edit_menu->add_separator();
    edit_menu->add_action(*m_cut_action);
    edit_menu->add_action(*m_copy_action);
    edit_menu->add_action(*m_paste_action);
    edit_menu->add_action(*m_insert_emoji_action);

    auto view_menu = window.add_menu("&View"_string);
    view_menu->add_action(GUI::CommonActions::make_fullscreen_action([&](auto&) {
        window.set_fullscreen(!window.is_fullscreen());
    }));

    auto help_menu = window.add_menu("&Help"_string);
    help_menu->add_action(*m_search_action);
    help_menu->add_action(*m_functions_help_action);
    help_menu->add_action(*m_about_action);

    return {};
}

}
