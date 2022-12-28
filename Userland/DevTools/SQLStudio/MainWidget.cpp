/*
 * Copyright (c) 2022, Dylan Katz <dykatz@uw.edu>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <DevTools/SQLStudio/SQLStudioGML.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/GroupBox.h>
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

MainWidget::MainWidget()
{
    if (!load_from_gml(sql_studio_gml))
        VERIFY_NOT_REACHED();

    m_new_action = GUI::Action::create("&New", { Mod_Ctrl, Key_N }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/new.png"sv).release_value_but_fixme_should_propagate_errors(), [this](auto&) {
        open_new_script();
    });

    m_open_action = GUI::CommonActions::make_open_action([&](auto&) {
        auto maybe_load_path = GUI::FilePicker::get_open_filepath(window());
        if (!maybe_load_path.has_value())
            return;
        auto lexical_path = LexicalPath(maybe_load_path.release_value());
        open_script_from_file(lexical_path);
    });

    m_save_action = GUI::CommonActions::make_save_action([&](auto&) {
        if (!m_tab_widget)
            return;
        auto editor = dynamic_cast<ScriptEditor*>(m_tab_widget->active_widget());
        if (!editor)
            return;
        auto save_attempt = editor->save();
        if (save_attempt.is_error())
            GUI::MessageBox::show_error(window(), DeprecatedString::formatted("Failed to save\n{}", save_attempt.release_error()));
    });

    m_save_as_action = GUI::CommonActions::make_save_as_action([&](auto&) {
        if (!m_tab_widget)
            return;
        auto editor = dynamic_cast<ScriptEditor*>(m_tab_widget->active_widget());
        if (!editor)
            return;
        auto save_attempt = editor->save_as();
        if (save_attempt.is_error())
            GUI::MessageBox::show_error(window(), DeprecatedString::formatted("Failed to save\n{}", save_attempt.release_error()));
    });

    m_save_all_action = GUI::Action::create("Save All", { Mod_Ctrl | Mod_Alt, Key_S }, [this](auto&) {
        auto current_active_widget = m_tab_widget->active_widget();
        ErrorOr<void> error {};
        m_tab_widget->for_each_child_widget([&](auto& child) {
            auto editor = dynamic_cast<ScriptEditor*>(&child);
            if (!editor)
                return IterationDecision::Continue;
            m_tab_widget->set_active_widget(editor);
            auto save_attempt = editor->save();
            if (save_attempt.is_error()) {
                error = save_attempt.release_error();
                return IterationDecision::Break;
            }
            auto save_result = save_attempt.release_value();
            if (save_result)
                return IterationDecision::Continue;
            return IterationDecision::Break;
        });
        if (error.is_error())
            GUI::MessageBox::show_error(window(), DeprecatedString::formatted("Failed to save all files\n{}", error.release_error()));
        m_tab_widget->set_active_widget(current_active_widget);
    });

    m_copy_action = GUI::CommonActions::make_copy_action([&](auto&) {
        if (!m_tab_widget)
            return;
        auto editor = dynamic_cast<ScriptEditor*>(m_tab_widget->active_widget());
        if (!editor)
            return;
        editor->copy_action().activate();
        update_editor_actions(editor);
    });

    m_cut_action = GUI::CommonActions::make_cut_action([&](auto&) {
        if (!m_tab_widget)
            return;
        auto editor = dynamic_cast<ScriptEditor*>(m_tab_widget->active_widget());
        if (!editor)
            return;
        editor->cut_action().activate();
        update_editor_actions(editor);
    });

    m_paste_action = GUI::CommonActions::make_paste_action([&](auto&) {
        if (!m_tab_widget)
            return;
        auto editor = dynamic_cast<ScriptEditor*>(m_tab_widget->active_widget());
        if (!editor)
            return;
        editor->paste_action().activate();
        update_editor_actions(editor);
    });

    m_undo_action = GUI::CommonActions::make_undo_action([&](auto&) {
        if (!m_tab_widget)
            return;
        auto editor = dynamic_cast<ScriptEditor*>(m_tab_widget->active_widget());
        if (!editor)
            return;
        editor->document().undo();
        update_editor_actions(editor);
    });

    m_redo_action = GUI::CommonActions::make_redo_action([&](auto&) {
        if (!m_tab_widget)
            return;
        auto editor = dynamic_cast<ScriptEditor*>(m_tab_widget->active_widget());
        if (!editor)
            return;
        editor->document().redo();
        update_editor_actions(editor);
    });

    m_run_script_action = GUI::Action::create("Run script", { Mod_Alt, Key_F9 }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/play.png"sv).release_value_but_fixme_should_propagate_errors(), [&](auto&) {
        m_results.clear();
        m_current_line_for_parsing = 0;

        // TODO select the database to use in UI.
        constexpr auto database_name = "Test"sv;

        if (auto connection_id = m_sql_client->connect(database_name); connection_id.has_value()) {
            m_connection_id = connection_id.release_value();
            read_next_sql_statement_of_editor();
        } else {
            warnln("\033[33;1mCould not connect to:\033[0m {}", database_name);
        }
    });

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
    toolbar.add_action(*m_run_script_action);

    m_tab_widget = find_descendant_of_type_named<GUI::TabWidget>("script_tab_widget"sv);

    m_tab_widget->on_tab_close_click = [&](auto& widget) {
        auto editor = dynamic_cast<ScriptEditor*>(&widget);
        if (!editor)
            return;
        auto close_attempt = editor->attempt_to_close();
        if (close_attempt.is_error()) {
            GUI::MessageBox::show_error(window(), DeprecatedString::formatted("Failed to save before closing\n{}", close_attempt.release_error()));
            return;
        }
        if (close_attempt.release_value()) {
            m_tab_widget->remove_tab(widget);
            update_title();
            on_editor_change();
        }
    };

    m_tab_widget->on_change = [&](auto&) {
        update_title();
        on_editor_change();
    };

    m_action_tab_widget = find_descendant_of_type_named<GUI::TabWidget>("action_tab_widget"sv);

    m_query_results_widget = m_action_tab_widget->add_tab<GUI::Widget>("Results");
    m_query_results_widget->set_layout<GUI::VerticalBoxLayout>();
    m_query_results_widget->layout()->set_margins(6);
    m_query_results_table_view = m_query_results_widget->add<GUI::TableView>();

    m_action_tab_widget->on_tab_close_click = [this](auto&) {
        m_action_tab_widget->set_fixed_height(0);
    };

    m_statusbar = find_descendant_of_type_named<GUI::Statusbar>("statusbar"sv);

    m_statusbar->segment(1).set_mode(GUI::Statusbar::Segment::Mode::Fixed);
    m_statusbar->segment(1).set_fixed_width(font().width("000000 characters (00000 words) selected"sv) + font().max_glyph_width());

    m_statusbar->segment(2).set_mode(GUI::Statusbar::Segment::Mode::Fixed);
    m_statusbar->segment(2).set_fixed_width(font().width("Ln 0000, Col 000"sv) + font().max_glyph_width());

    m_sql_client = SQL::SQLClient::try_create().release_value_but_fixme_should_propagate_errors();
    m_sql_client->on_execution_success = [this](auto, auto, auto, auto, auto, auto) {
        read_next_sql_statement_of_editor();
    };
    m_sql_client->on_next_result = [this](auto, auto, auto row) {
        m_results.append({});
        m_results.last().ensure_capacity(row.size());

        for (auto const& value : row)
            m_results.last().unchecked_append(value.to_deprecated_string());
    };
    m_sql_client->on_results_exhausted = [this](auto, auto, auto) {
        if (m_results.size() == 0)
            return;
        if (m_results[0].size() == 0)
            return;
        Vector<GUI::JsonArrayModel::FieldSpec> query_result_fields;
        for (size_t i = 0; i < m_results[0].size(); i++)
            query_result_fields.empend(DeprecatedString::formatted("column_{}", i + 1), DeprecatedString::formatted("Column {}", i + 1), Gfx::TextAlignment::CenterLeft);
        auto query_results_model = GUI::JsonArrayModel::create("{}", move(query_result_fields));
        m_query_results_table_view->set_model(MUST(GUI::SortingProxyModel::create(*query_results_model)));
        for (auto& result_row : m_results) {
            Vector<JsonValue> individual_result_as_json;
            for (auto& result_row_column : result_row)
                individual_result_as_json.append(result_row_column);
            query_results_model->add(move(individual_result_as_json));
        }
        m_action_tab_widget->set_fixed_height(200);
    };
}

void MainWidget::initialize_menu(GUI::Window* window)
{
    auto& file_menu = window->add_menu("&File");
    file_menu.add_action(*m_new_action);
    file_menu.add_action(*m_open_action);
    file_menu.add_action(*m_save_action);
    file_menu.add_action(*m_save_as_action);
    file_menu.add_action(*m_save_all_action);
    file_menu.add_separator();
    file_menu.add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the()->quit();
    }));

    auto& edit_menu = window->add_menu("&Edit");
    edit_menu.add_action(*m_copy_action);
    edit_menu.add_action(*m_cut_action);
    edit_menu.add_action(*m_paste_action);
    edit_menu.add_separator();
    edit_menu.add_action(*m_undo_action);
    edit_menu.add_action(*m_redo_action);
    edit_menu.add_separator();
    edit_menu.add_action(*m_run_script_action);

    auto& help_menu = window->add_menu("&Help");
    help_menu.add_action(GUI::CommonActions::make_command_palette_action(window));
    help_menu.add_action(GUI::CommonActions::make_help_action([](auto&) {
        Desktop::Launcher::open(URL::create_with_file_scheme("/usr/share/man/man1/SQLStudio.md"), "/bin/Help");
    }));
    help_menu.add_action(GUI::CommonActions::make_about_action("SQL Studio", GUI::Icon::default_icon("app-sql-studio"sv), window));
}

void MainWidget::open_new_script()
{
    auto new_script_name = DeprecatedString::formatted("New Script - {}", m_new_script_counter);
    ++m_new_script_counter;

    auto& editor = m_tab_widget->add_tab<ScriptEditor>(new_script_name);
    editor.new_script_with_temp_name(new_script_name);

    editor.on_cursor_change = [this] { on_editor_change(); };
    editor.on_selection_change = [this] { on_editor_change(); };
    editor.on_highlighter_change = [this] { on_editor_change(); };

    m_tab_widget->set_active_widget(&editor);
}

void MainWidget::open_script_from_file(LexicalPath const& file_path)
{
    auto& editor = m_tab_widget->add_tab<ScriptEditor>(file_path.title());
    auto maybe_error = editor.open_script_from_file(file_path);
    if (maybe_error.is_error()) {
        GUI::MessageBox::show_error(window(), DeprecatedString::formatted("Failed to open {}\n{}", file_path, maybe_error.release_error()));
        return;
    }

    editor.on_cursor_change = [this] { on_editor_change(); };
    editor.on_selection_change = [this] { on_editor_change(); };
    editor.on_highlighter_change = [this] { on_editor_change(); };

    m_tab_widget->set_active_widget(&editor);
}

void MainWidget::open_database_from_file(LexicalPath const&)
{
    TODO();
}

bool MainWidget::request_close()
{
    auto any_scripts_modified { false };
    auto is_script_modified = [&](auto& child) {
        auto editor = dynamic_cast<ScriptEditor*>(&child);
        if (!editor)
            return IterationDecision::Continue;
        if (editor->document().is_modified()) {
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

void MainWidget::update_title()
{
    auto editor = dynamic_cast<ScriptEditor*>(m_tab_widget->active_widget());
    if (editor) {
        window()->set_title(DeprecatedString::formatted("{} - SQL Studio", editor->name()));
    } else {
        window()->set_title("SQL Studio");
    }
}

void MainWidget::on_editor_change()
{
    auto editor = dynamic_cast<ScriptEditor*>(m_tab_widget->active_widget());
    update_statusbar(editor);
    update_editor_actions(editor);
}

void MainWidget::update_statusbar(ScriptEditor* editor)
{
    if (!editor) {
        m_statusbar->set_text(1, "");
        m_statusbar->set_text(2, "");
        return;
    }

    if (editor->has_selection()) {
        auto character_count = editor->selected_text().length();
        auto word_count = editor->number_of_selected_words();
        m_statusbar->set_text(1, DeprecatedString::formatted("{} {} ({} {}) selected", character_count, character_count == 1 ? "character" : "characters", word_count, word_count == 1 ? "word" : "words"));
    } else {
        auto character_count = editor->text().length();
        auto word_count = editor->number_of_words();
        m_statusbar->set_text(1, DeprecatedString::formatted("{} {} ({} {})", character_count, character_count == 1 ? "character" : "characters", word_count, word_count == 1 ? "word" : "words"));
    }

    m_statusbar->set_text(2, DeprecatedString::formatted("Ln {}, Col {}", editor->cursor().line() + 1, editor->cursor().column()));
}

void MainWidget::update_editor_actions(ScriptEditor* editor)
{
    if (!editor) {
        m_copy_action->set_enabled(false);
        m_cut_action->set_enabled(false);
        m_paste_action->set_enabled(false);
        m_undo_action->set_enabled(false);
        m_redo_action->set_enabled(false);
        return;
    }

    m_copy_action->set_enabled(editor->copy_action().is_enabled());
    m_cut_action->set_enabled(editor->cut_action().is_enabled());
    m_paste_action->set_enabled(editor->paste_action().is_enabled());

    m_undo_action->set_enabled(editor->undo_action().is_enabled());
    m_redo_action->set_enabled(editor->redo_action().is_enabled());
}

void MainWidget::drag_enter_event(GUI::DragEvent& event)
{
    auto const& mime_types = event.mime_types();
    if (mime_types.contains_slow("text/uri-list"))
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
            if (!scheme.equals_ignoring_case("file"sv))
                continue;

            auto lexical_path = LexicalPath(url.path());
            if (lexical_path.extension().equals_ignoring_case("sql"sv))
                open_script_from_file(lexical_path);
            if (lexical_path.extension().equals_ignoring_case("db"sv))
                open_database_from_file(lexical_path);
        }
    }
}

void MainWidget::read_next_sql_statement_of_editor()
{
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

    auto sql_statement = piece.to_deprecated_string();

    if (auto statement_id = m_sql_client->prepare_statement(m_connection_id, sql_statement); statement_id.has_value()) {
        m_sql_client->async_execute_statement(*statement_id, {});
    } else {
        auto* editor = dynamic_cast<ScriptEditor*>(m_tab_widget->active_widget());
        VERIFY(editor);

        GUI::MessageBox::show_error(window(), DeprecatedString::formatted("Could not parse {}\n{}", editor->path(), sql_statement));
    }
}

Optional<DeprecatedString> MainWidget::read_next_line_of_editor()
{
    auto editor = dynamic_cast<ScriptEditor*>(m_tab_widget->active_widget());
    if (!editor)
        return {};
    if (m_current_line_for_parsing < editor->document().line_count()) {
        DeprecatedString result = editor->document().line(m_current_line_for_parsing).to_utf8();
        m_current_line_for_parsing++;
        return result;
    } else {
        return {};
    }
}

}
