/*
 * Copyright (c) 2022, Dylan Katz <dykatz@uw.edu>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibDesktop/Launcher.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Statusbar.h>
#include <LibGUI/TabWidget.h>
#include <LibGUI/TextDocument.h>
#include <LibGUI/TextEditor.h>
#include <LibGUI/Toolbar.h>
#include <LibGUI/ToolbarContainer.h>

#include "MainWidget.h"
#include "ScriptEditor.h"

namespace SQLStudio {

MainWidget::MainWidget()
{
    set_fill_with_background_color(true);
    set_layout<GUI::VerticalBoxLayout>();

    m_new_action = GUI::Action::create("&New", { Mod_Ctrl, Key_N }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/new.png").release_value_but_fixme_should_propagate_errors(), [this](auto&) {
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
            GUI::MessageBox::show_error(window(), String::formatted("Failed to save\n{}", save_attempt.release_error()));
    });

    m_save_as_action = GUI::CommonActions::make_save_as_action([&](auto&) {
        if (!m_tab_widget)
            return;
        auto editor = dynamic_cast<ScriptEditor*>(m_tab_widget->active_widget());
        if (!editor)
            return;
        auto save_attempt = editor->save_as();
        if (save_attempt.is_error())
            GUI::MessageBox::show_error(window(), String::formatted("Failed to save\n{}", save_attempt.release_error()));
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
            GUI::MessageBox::show_error(window(), String::formatted("Failed to save all files\n{}", error.release_error()));
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

    auto& toolbar_container = add<GUI::ToolbarContainer>();
    auto& toolbar = toolbar_container.add<GUI::Toolbar>();

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

    m_tab_widget = add<GUI::TabWidget>();
    m_tab_widget->set_close_button_enabled(true);
    m_tab_widget->set_reorder_allowed(true);

    m_tab_widget->on_tab_close_click = [&](auto& widget) {
        auto editor = dynamic_cast<ScriptEditor*>(&widget);
        if (!editor)
            return;
        auto close_attempt = editor->attempt_to_close();
        if (close_attempt.is_error()) {
            GUI::MessageBox::show_error(window(), String::formatted("Failed to save before closing\n{}", close_attempt.release_error()));
            return;
        }
        if (close_attempt.release_value()) {
            m_tab_widget->remove_tab(widget);
            update_title();
        }
    };

    m_tab_widget->on_change = [&](auto&) {
        update_title();
        on_editor_change();
    };

    m_statusbar = add<GUI::Statusbar>(3);

    m_statusbar->segment(1).set_mode(GUI::Statusbar::Segment::Mode::Fixed);
    m_statusbar->segment(1).set_fixed_width(font().width("000000 characters (00000 words) selected") + font().max_glyph_width());

    m_statusbar->segment(2).set_mode(GUI::Statusbar::Segment::Mode::Fixed);
    m_statusbar->segment(2).set_fixed_width(font().width("Ln 0000, Col 000") + font().max_glyph_width());
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

    auto& help_menu = window->add_menu("&Help");
    help_menu.add_action(GUI::CommonActions::make_help_action([](auto&) {
        Desktop::Launcher::open(URL::create_with_file_protocol("/usr/share/man/man1/SQLStudio.md"), "/bin/Help");
    }));
    help_menu.add_action(GUI::CommonActions::make_about_action("SQL Studio", GUI::Icon::default_icon("app-sql-studio"), window));
}

void MainWidget::open_new_script()
{
    auto new_script_name = String::formatted("New Script - {}", m_new_script_counter);
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
        GUI::MessageBox::show_error(window(), String::formatted("Failed to open {}\n{}", file_path, maybe_error.release_error()));
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
        window()->set_title(String::formatted("{} - SQL Studio", editor->name()));
    } else {
        window()->set_title("SQL Studio");
    }
}

void MainWidget::on_editor_change()
{
    auto editor = dynamic_cast<ScriptEditor*>(m_tab_widget->active_widget());
    if (editor) {
        update_statusbar(editor);
        update_editor_actions(editor);
    }
}

void MainWidget::update_statusbar(ScriptEditor* editor)
{
    if (editor->has_selection()) {
        auto character_count = editor->selected_text().length();
        auto word_count = editor->number_of_selected_words();
        m_statusbar->set_text(1, String::formatted("{} {} ({} {}) selected", character_count, character_count == 1 ? "character" : "characters", word_count, word_count == 1 ? "word" : "words"));
    } else {
        auto character_count = editor->text().length();
        auto word_count = editor->number_of_words();
        m_statusbar->set_text(1, String::formatted("{} {} ({} {})", character_count, character_count == 1 ? "character" : "characters", word_count, word_count == 1 ? "word" : "words"));
    }

    m_statusbar->set_text(2, String::formatted("Ln {}, Col {}", editor->cursor().line() + 1, editor->cursor().column()));
}

void MainWidget::update_editor_actions(ScriptEditor* editor)
{
    m_copy_action->set_enabled(editor->copy_action().is_enabled());
    m_cut_action->set_enabled(editor->cut_action().is_enabled());
    m_paste_action->set_enabled(editor->paste_action().is_enabled());

    m_undo_action->set_enabled(editor->undo_action().is_enabled());
    m_redo_action->set_enabled(editor->redo_action().is_enabled());
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
            if (!scheme.equals_ignoring_case("file"))
                continue;

            auto lexical_path = LexicalPath(url.path());
            if (lexical_path.extension().equals_ignoring_case("sql"sv))
                open_script_from_file(lexical_path);
            if (lexical_path.extension().equals_ignoring_case("db"sv))
                open_database_from_file(lexical_path);
        }
    }
}

}
