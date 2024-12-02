/*
 * Copyright (c) 2022, Dylan Katz <dykatz@uw.edu>
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 * Copyright (c) 2023, Cameron Youell <cameronyouell@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/DirIterator.h>
#include <LibCore/StandardPaths.h>
#include <LibDesktop/Launcher.h>
#include <LibFileSystem/FileSystem.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/GroupBox.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/JsonArrayModel.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/SortingProxyModel.h>
#include <LibGUI/Statusbar.h>
#include <LibGUI/TabWidget.h>
#include <LibGUI/TableView.h>
#include <LibGUI/TextDocument.h>
#include <LibGUI/TextEditor.h>
#include <LibGUI/Toolbar.h>
#include <LibGUI/ToolbarContainer.h>
#include <LibSQL/AST/Lexer.h>
#include <LibSQL/AST/Token.h>
#include <LibSQL/SQLClient.h>
#include <LibSQL/Value.h>

#include "MainWidget.h"
#include "ScriptEditor.h"

REGISTER_WIDGET(SQLStudio, MainWidget);

namespace SQLStudio {

static Vector<ByteString> lookup_database_names()
{
    static constexpr auto database_extension = ".db"sv;

    auto database_path = ByteString::formatted("{}/sql", Core::StandardPaths::data_directory());
    if (!FileSystem::exists(database_path))
        return {};

    Core::DirIterator iterator(move(database_path), Core::DirIterator::SkipParentAndBaseDir);
    Vector<ByteString> database_names;

    while (iterator.has_next()) {
        if (auto database = iterator.next_path(); database.ends_with(database_extension))
            database_names.append(database.substring(0, database.length() - database_extension.length()));
    }

    return database_names;
}

ErrorOr<void> MainWidget::initialize()
{
    m_new_action = GUI::Action::create("&New", { Mod_Ctrl, Key_N }, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/new.png"sv)), [this](auto&) {
        open_new_script();
    });

    m_open_action = GUI::CommonActions::make_open_action([&](auto&) {
        if (auto result = GUI::FilePicker::get_open_filepath(window()); result.has_value())
            open_script_from_file(LexicalPath { result.release_value() });
    });

    m_save_action = GUI::CommonActions::make_save_action([&](auto&) {
        auto* editor = active_editor();
        VERIFY(editor);

        if (auto result = editor->save(); result.is_error())
            GUI::MessageBox::show_error(window(), ByteString::formatted("Failed to save {}\n{}", editor->path(), result.error()));
    });

    m_save_as_action = GUI::CommonActions::make_save_as_action([&](auto&) {
        auto* editor = active_editor();
        VERIFY(editor);

        if (auto result = editor->save_as(); result.is_error())
            GUI::MessageBox::show_error(window(), ByteString::formatted("Failed to save {}\n{}", editor->path(), result.error()));
    });

    m_save_all_action = GUI::Action::create("Save All", { Mod_Ctrl | Mod_Alt, Key_S }, [this](auto&) {
        auto* editor = active_editor();
        VERIFY(editor);

        m_tab_widget->for_each_child_widget([&](auto& child) {
            auto& editor = verify_cast<ScriptEditor>(child);
            m_tab_widget->set_active_widget(&editor);

            if (auto result = editor.save(); result.is_error()) {
                GUI::MessageBox::show_error(window(), ByteString::formatted("Failed to save {}\n{}", editor.path(), result.error()));
                return IterationDecision::Break;
            } else if (!result.value()) {
                return IterationDecision::Break;
            }

            return IterationDecision::Continue;
        });

        m_tab_widget->set_active_widget(editor);
    });

    m_copy_action = GUI::CommonActions::make_copy_action([&](auto&) {
        auto* editor = active_editor();
        VERIFY(editor);

        editor->copy_action().activate();
        update_editor_actions(editor);
    });

    m_cut_action = GUI::CommonActions::make_cut_action([&](auto&) {
        auto* editor = active_editor();
        VERIFY(editor);

        editor->cut_action().activate();
        update_editor_actions(editor);
    });

    m_paste_action = GUI::CommonActions::make_paste_action([&](auto&) {
        auto* editor = active_editor();
        VERIFY(editor);

        editor->paste_action().activate();
        update_editor_actions(editor);
    });

    m_undo_action = GUI::CommonActions::make_undo_action([&](auto&) {
        auto* editor = active_editor();
        VERIFY(editor);

        editor->document().undo();
        update_editor_actions(editor);
    });

    m_redo_action = GUI::CommonActions::make_redo_action([&](auto&) {
        auto* editor = active_editor();
        VERIFY(editor);

        editor->document().redo();
        update_editor_actions(editor);
    });

    m_connect_to_database_action = GUI::Action::create("Connect to Database"sv, { Mod_Alt, Key_C }, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/go-forward.png"sv)), [this](auto&) {
        auto database_name = m_databases_combo_box->text().trim_whitespace();
        if (database_name.is_empty())
            return;

        m_run_script_action->set_enabled(false);
        m_statusbar->set_text(1, "Disconnected"_string);

        if (m_connection_id.has_value()) {
            m_sql_client->disconnect(*m_connection_id);
            m_connection_id.clear();
        }

        if (auto connection_id = m_sql_client->connect(database_name); connection_id.has_value()) {
            m_statusbar->set_text(1, String::formatted("Connected to: {}", database_name).release_value_but_fixme_should_propagate_errors());
            m_connection_id = *connection_id;
            m_run_script_action->set_enabled(true);
        } else {
            GUI::MessageBox::show_error(window(), ByteString::formatted("Could not connect to {}", database_name));
        }
    });

    m_run_script_action = GUI::Action::create("Run Script", { Mod_Alt, Key_F9 }, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/play.png"sv)), [&](auto&) {
        m_results.clear();
        m_current_line_for_parsing = 0;
        read_next_sql_statement_of_editor();
    });
    m_run_script_action->set_enabled(false);

    static auto database_names = lookup_database_names();
    m_databases_combo_box = GUI::ComboBox::construct();
    m_databases_combo_box->set_editor_placeholder("Enter new database or select existing database"sv);
    m_databases_combo_box->set_max_width(font().width(m_databases_combo_box->editor_placeholder()) + font().max_glyph_width() + 16);
    m_databases_combo_box->set_model(*GUI::ItemListModel<ByteString>::create(database_names));
    m_databases_combo_box->on_return_pressed = [this]() {
        m_connect_to_database_action->activate(m_databases_combo_box);
    };

    auto& toolbar = *find_descendant_of_type_named<GUI::Toolbar>("toolbar"sv);
    toolbar.add_action(*m_new_action);
    toolbar.add_action(*m_open_action);
    toolbar.add_action(*m_save_action);
    toolbar.add_action(*m_save_as_action);
    toolbar.add_separator();
    toolbar.add_action(*m_copy_action);
    toolbar.add_action(*m_cut_action);
    toolbar.add_action(*m_paste_action);
    toolbar.add_separator();
    toolbar.add_action(*m_undo_action);
    toolbar.add_action(*m_redo_action);
    toolbar.add_separator();
    toolbar.add_child(*m_databases_combo_box);
    toolbar.add_action(*m_connect_to_database_action);
    toolbar.add_separator();
    toolbar.add_action(*m_run_script_action);

    m_tab_widget = find_descendant_of_type_named<GUI::TabWidget>("script_tab_widget"sv);

    m_tab_widget->on_tab_close_click = [&](auto& widget) {
        auto& editor = verify_cast<ScriptEditor>(widget);

        if (auto result = editor.attempt_to_close(); result.is_error()) {
            GUI::MessageBox::show_error(window(), ByteString::formatted("Failed to save {}\n{}", editor.path(), result.error()));
        } else if (result.value()) {
            m_tab_widget->remove_tab(editor);
            update_title();
            on_editor_change();
        }
    };

    m_tab_widget->on_change = [&](auto&) {
        update_title();
        on_editor_change();
    };

    m_action_tab_widget = find_descendant_of_type_named<GUI::TabWidget>("action_tab_widget"sv);

    m_query_results_widget = m_action_tab_widget->add_tab<GUI::Widget>("Results"_string);
    m_query_results_widget->set_layout<GUI::VerticalBoxLayout>(6);
    m_query_results_table_view = m_query_results_widget->add<GUI::TableView>();

    m_action_tab_widget->on_tab_close_click = [this](auto&) {
        m_action_tab_widget->set_visible(false);
    };

    m_statusbar = find_descendant_of_type_named<GUI::Statusbar>("statusbar"sv);
    m_statusbar->segment(1).set_mode(GUI::Statusbar::Segment::Mode::Auto);
    m_statusbar->set_text(1, "Disconnected"_string);
    m_statusbar->segment(2).set_mode(GUI::Statusbar::Segment::Mode::Fixed);
    m_statusbar->segment(2).set_fixed_width(font().width("Ln 0,000  Col 000"sv) + font().max_glyph_width());

    GUI::Application::the()->on_action_enter = [this](GUI::Action& action) {
        m_statusbar->set_override_text(action.status_tip());
    };

    GUI::Application::the()->on_action_leave = [this](GUI::Action&) {
        m_statusbar->set_override_text({});
    };

    m_sql_client = TRY(SQL::SQLClient::try_create());
    m_sql_client->on_execution_success = [this](auto result) {
        m_result_column_names = move(result.column_names);
        read_next_sql_statement_of_editor();
    };
    m_sql_client->on_execution_error = [this](auto result) {
        auto* editor = active_editor();
        VERIFY(editor);

        GUI::MessageBox::show_error(window(), ByteString::formatted("Error executing {}\n{}", editor->path(), result.error_message));
    };
    m_sql_client->on_next_result = [this](auto result) {
        m_results.append({});
        m_results.last().ensure_capacity(result.values.size());

        for (auto const& value : result.values)
            m_results.last().unchecked_append(value.to_byte_string());
    };
    m_sql_client->on_results_exhausted = [this](auto) {
        if (m_results.size() == 0)
            return;
        if (m_results[0].size() == 0)
            return;

        Vector<GUI::JsonArrayModel::FieldSpec> query_result_fields;
        for (auto& column_name : m_result_column_names)
            query_result_fields.empend(column_name, String::from_byte_string(column_name).release_value_but_fixme_should_propagate_errors(), Gfx::TextAlignment::CenterLeft);

        auto query_results_model = GUI::JsonArrayModel::create("{}", move(query_result_fields));
        m_query_results_table_view->set_model(MUST(GUI::SortingProxyModel::create(*query_results_model)));
        for (auto& result_row : m_results) {
            Vector<JsonValue> individual_result_as_json;
            for (auto& result_row_column : result_row)
                individual_result_as_json.append(result_row_column);
            MUST(query_results_model->add(move(individual_result_as_json)));
        }
        m_action_tab_widget->set_visible(true);
    };

    return {};
}

ErrorOr<void> MainWidget::initialize_menu(GUI::Window* window)
{
    auto file_menu = window->add_menu("&File"_string);
    file_menu->add_action(*m_new_action);
    file_menu->add_action(*m_open_action);
    file_menu->add_action(*m_save_action);
    file_menu->add_action(*m_save_as_action);
    file_menu->add_action(*m_save_all_action);
    file_menu->add_separator();
    file_menu->add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the()->quit();
    }));

    auto edit_menu = window->add_menu("&Edit"_string);
    edit_menu->add_action(*m_copy_action);
    edit_menu->add_action(*m_cut_action);
    edit_menu->add_action(*m_paste_action);
    edit_menu->add_separator();
    edit_menu->add_action(*m_undo_action);
    edit_menu->add_action(*m_redo_action);
    edit_menu->add_separator();
    edit_menu->add_action(*m_run_script_action);

    auto view_menu = window->add_menu("&View"_string);
    view_menu->add_action(GUI::CommonActions::make_fullscreen_action([window](auto&) {
        window->set_fullscreen(!window->is_fullscreen());
    }));

    auto help_menu = window->add_menu("&Help"_string);
    help_menu->add_action(GUI::CommonActions::make_command_palette_action(window));
    help_menu->add_action(GUI::CommonActions::make_help_action([](auto&) {
        Desktop::Launcher::open(URL::create_with_file_scheme("/usr/share/man/man1/Applications/SQLStudio.md"), "/bin/Help");
    }));
    help_menu->add_action(GUI::CommonActions::make_about_action("SQL Studio"_string, GUI::Icon::default_icon("app-sql-studio"sv), window));
    return {};
}

void MainWidget::open_new_script()
{
    auto new_script_name = ByteString::formatted("New Script - {}", m_new_script_counter);
    ++m_new_script_counter;

    auto& editor = m_tab_widget->add_tab<ScriptEditor>(String::from_byte_string(new_script_name).release_value_but_fixme_should_propagate_errors());
    editor.new_script_with_temp_name(new_script_name);

    editor.on_cursor_change = [this] { on_editor_change(); };
    editor.on_selection_change = [this] { on_editor_change(); };
    editor.on_highlighter_change = [this] { on_editor_change(); };

    m_tab_widget->set_active_widget(&editor);
}

void MainWidget::open_script_from_file(LexicalPath const& file_path)
{
    auto& editor = m_tab_widget->add_tab<ScriptEditor>(String::from_utf8(file_path.title()).release_value_but_fixme_should_propagate_errors());

    if (auto result = editor.open_script_from_file(file_path); result.is_error()) {
        GUI::MessageBox::show_error(window(), ByteString::formatted("Failed to open {}\n{}", file_path, result.error()));
        return;
    }

    editor.on_cursor_change = [this] { on_editor_change(); };
    editor.on_selection_change = [this] { on_editor_change(); };
    editor.on_highlighter_change = [this] { on_editor_change(); };

    m_tab_widget->set_active_widget(&editor);
}

bool MainWidget::request_close()
{
    auto any_scripts_modified { false };
    auto is_script_modified = [&](auto& child) {
        auto& editor = verify_cast<ScriptEditor>(child);

        if (editor.document().is_modified()) {
            any_scripts_modified = true;
            return IterationDecision::Break;
        }

        return IterationDecision::Continue;
    };

    m_tab_widget->for_each_child_widget(is_script_modified);
    if (!any_scripts_modified)
        return true;

    auto result = GUI::MessageBox::ask_about_unsaved_changes(window(), {});
    switch (result) {
    case GUI::Dialog::ExecResult::Yes:
        break;
    case GUI::Dialog::ExecResult::No:
        return true;
    default:
        return false;
    }

    m_save_all_action->activate();
    any_scripts_modified = false;

    m_tab_widget->for_each_child_widget(is_script_modified);
    return !any_scripts_modified;
}

ScriptEditor* MainWidget::active_editor()
{
    if (!m_tab_widget || !m_tab_widget->active_widget())
        return nullptr;
    return verify_cast<ScriptEditor>(m_tab_widget->active_widget());
}

void MainWidget::update_title()
{
    if (auto* editor = active_editor())
        window()->set_title(ByteString::formatted("{} - SQL Studio", editor->name()));
    else
        window()->set_title("SQL Studio");
}

void MainWidget::on_editor_change()
{
    auto* editor = active_editor();
    update_statusbar(editor);
    update_editor_actions(editor);
}

void MainWidget::update_statusbar(ScriptEditor* editor)
{
    if (!editor) {
        m_statusbar->set_text(0, {});
        m_statusbar->set_text(2, {});
        return;
    }

    StringBuilder builder;
    if (editor->has_selection()) {
        auto character_count = editor->selected_text().length();
        auto word_count = editor->number_of_selected_words();
        builder.appendff("Selected: {:'d} {} ({:'d} {})", character_count, character_count == 1 ? "character" : "characters", word_count, word_count != 1 ? "words" : "word");
    }

    m_statusbar->set_text(0, builder.to_string().release_value_but_fixme_should_propagate_errors());
    m_statusbar->set_text(2, String::formatted("Ln {:'d}  Col {:'d}", editor->cursor().line() + 1, editor->cursor().column()).release_value_but_fixme_should_propagate_errors());
}

void MainWidget::update_editor_actions(ScriptEditor* editor)
{
    if (!editor) {
        m_save_action->set_enabled(false);
        m_save_as_action->set_enabled(false);
        m_save_all_action->set_enabled(false);
        m_run_script_action->set_enabled(false);

        m_copy_action->set_enabled(false);
        m_cut_action->set_enabled(false);
        m_paste_action->set_enabled(false);
        m_undo_action->set_enabled(false);
        m_redo_action->set_enabled(false);

        return;
    }

    m_save_action->set_enabled(true);
    m_save_as_action->set_enabled(true);
    m_save_all_action->set_enabled(true);
    m_run_script_action->set_enabled(m_connection_id.has_value());

    m_copy_action->set_enabled(editor->copy_action().is_enabled());
    m_cut_action->set_enabled(editor->cut_action().is_enabled());
    m_paste_action->set_enabled(editor->paste_action().is_enabled());
    m_undo_action->set_enabled(editor->undo_action().is_enabled());
    m_redo_action->set_enabled(editor->redo_action().is_enabled());
}

void MainWidget::drag_enter_event(GUI::DragEvent& event)
{
    if (event.mime_data().has_urls())
        event.accept();
}

void MainWidget::drop_event(GUI::DropEvent& drop_event)
{
    drop_event.accept();
    window()->move_to_front();

    if (drop_event.mime_data().has_urls()) {
        auto urls = drop_event.mime_data().urls();
        if (urls.is_empty())
            return;

        for (auto& url : urls) {
            auto& scheme = url.scheme();
            if (!scheme.bytes_as_string_view().equals_ignoring_ascii_case("file"sv))
                continue;

            auto lexical_path = LexicalPath(URL::percent_decode(url.serialize_path()));
            open_script_from_file(lexical_path);
        }
    }
}

void MainWidget::read_next_sql_statement_of_editor()
{
    if (!m_connection_id.has_value())
        return;

    StringBuilder piece;
    do {
        if (!piece.is_empty())
            piece.append('\n');

        auto line_maybe = read_next_line_of_editor();

        if (!line_maybe.has_value())
            return;

        auto& line = line_maybe.value();
        auto lexer = SQL::AST::Lexer(line);

        piece.append(line);

        bool is_first_token = true;
        bool is_command = false;
        bool last_token_ended_statement = false;
        bool tokens_found = false;

        for (SQL::AST::Token token = lexer.next(); token.type() != SQL::AST::TokenType::Eof; token = lexer.next()) {
            tokens_found = true;
            switch (token.type()) {
            case SQL::AST::TokenType::ParenOpen:
                ++m_editor_line_level;
                break;
            case SQL::AST::TokenType::ParenClose:
                --m_editor_line_level;
                break;
            case SQL::AST::TokenType::SemiColon:
                last_token_ended_statement = true;
                break;
            case SQL::AST::TokenType::Period:
                if (is_first_token)
                    is_command = true;
                break;
            default:
                last_token_ended_statement = is_command;
                break;
            }

            is_first_token = false;
        }

        if (tokens_found)
            m_editor_line_level = last_token_ended_statement ? 0 : (m_editor_line_level > 0 ? m_editor_line_level : 1);
    } while ((m_editor_line_level > 0) || piece.is_empty());

    auto sql_statement = piece.to_byte_string();

    if (auto statement_id = m_sql_client->prepare_statement(*m_connection_id, sql_statement); statement_id.has_value()) {
        m_sql_client->async_execute_statement(*statement_id, {});
    } else {
        auto* editor = active_editor();
        VERIFY(editor);

        GUI::MessageBox::show_error(window(), ByteString::formatted("Could not parse {}\n{}", editor->path(), sql_statement));
    }
}

Optional<ByteString> MainWidget::read_next_line_of_editor()
{
    auto* editor = active_editor();
    if (!editor)
        return {};

    if (m_current_line_for_parsing >= editor->document().line_count())
        return {};

    auto result = editor->document().line(m_current_line_for_parsing).to_utf8();
    ++m_current_line_for_parsing;
    return result;
}

}
