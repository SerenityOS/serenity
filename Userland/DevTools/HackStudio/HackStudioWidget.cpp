/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2022, Itamar S. <itamar8910@gmail.com>
 * Copyright (c) 2023-2024, Abhishek R. <raturiabhi1000@gmail.com>
 * Copyright (c) 2020-2022, the SerenityOS developers.
 * Copyright (c) 2024, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "HackStudioWidget.h"
#include "Debugger/DebugInfoWidget.h"
#include "Debugger/Debugger.h"
#include "Debugger/DisassemblyWidget.h"
#include "Dialogs/NewProjectDialog.h"
#include "Editor.h"
#include "EditorWrapper.h"
#include "FindInFilesWidget.h"
#include "Git/DiffViewer.h"
#include "Git/GitWidget.h"
#include "HackStudio.h"
#include "Locator.h"
#include "Project.h"
#include "ProjectDeclarations.h"
#include "TerminalWrapper.h"
#include "ToDoEntries.h"
#include <AK/JsonParser.h>
#include <AK/LexicalPath.h>
#include <AK/StringBuilder.h>
#include <Kernel/API/InodeWatcherEvent.h>
#include <LibConfig/Client.h>
#include <LibCore/Event.h>
#include <LibCore/EventLoop.h>
#include <LibCore/FileWatcher.h>
#include <LibCore/System.h>
#include <LibDebug/DebugSession.h>
#include <LibDesktop/Launcher.h>
#include <LibFileSystem/FileSystem.h>
#include <LibGUI/Action.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Dialog.h>
#include <LibGUI/EditingEngine.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/FontPicker.h>
#include <LibGUI/InputBox.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/Label.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/ModelEditingDelegate.h>
#include <LibGUI/RegularEditingEngine.h>
#include <LibGUI/Splitter.h>
#include <LibGUI/StackWidget.h>
#include <LibGUI/Statusbar.h>
#include <LibGUI/TabWidget.h>
#include <LibGUI/TableView.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/TextEditor.h>
#include <LibGUI/Toolbar.h>
#include <LibGUI/ToolbarContainer.h>
#include <LibGUI/TreeView.h>
#include <LibGUI/VimEditingEngine.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibGfx/Palette.h>
#include <LibThreading/Mutex.h>
#include <LibThreading/Thread.h>
#include <LibVT/TerminalWidget.h>
#include <fcntl.h>
#include <stdio.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>

namespace HackStudio {

ErrorOr<NonnullRefPtr<HackStudioWidget>> HackStudioWidget::create(ByteString path_to_project)
{
    auto widget = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) HackStudioWidget));

    widget->m_editor_font = widget->read_editor_font_from_config();
    widget->set_fill_with_background_color(true);
    widget->set_layout<GUI::VerticalBoxLayout>(GUI::Margins {}, 2);

    auto& toolbar_container = widget->add<GUI::ToolbarContainer>();

    auto& outer_splitter = widget->add<GUI::HorizontalSplitter>();
    outer_splitter.layout()->set_spacing(4);

    auto& left_hand_splitter = outer_splitter.add<GUI::VerticalSplitter>();
    left_hand_splitter.layout()->set_spacing(6);
    left_hand_splitter.set_preferred_width(150);

    widget->m_project_tree_view_context_menu = TRY(widget->create_project_tree_view_context_menu());

    widget->m_right_hand_splitter = outer_splitter.add<GUI::VerticalSplitter>();
    widget->m_right_hand_stack = widget->m_right_hand_splitter->add<GUI::StackWidget>();

    TRY(widget->create_action_tab(*widget->m_right_hand_splitter));

    widget->open_project(path_to_project);
    widget->create_project_tab(left_hand_splitter);
    widget->create_open_files_view(left_hand_splitter);

    // Put a placeholder widget front & center since we don't have a file open yet.
    widget->m_right_hand_stack->add<GUI::Widget>();

    widget->m_diff_viewer = widget->m_right_hand_stack->add<DiffViewer>();

    widget->m_editors_splitter = widget->m_right_hand_stack->add<GUI::VerticalSplitter>();
    widget->m_editors_splitter->layout()->set_margins({ 3, 0, 0 });
    widget->add_new_editor_tab_widget(*widget->m_editors_splitter);

    widget->m_switch_to_next_editor_tab_widget = widget->create_switch_to_next_editor_tab_widget_action();
    widget->m_switch_to_next_editor = widget->create_switch_to_next_editor_action();
    widget->m_switch_to_previous_editor = widget->create_switch_to_previous_editor_action();

    widget->m_remove_current_editor_tab_widget_action = widget->create_remove_current_editor_tab_widget_action();
    widget->m_remove_current_editor_action = TRY(widget->create_remove_current_editor_action());
    widget->m_open_action = TRY(widget->create_open_action());
    widget->m_save_action = widget->create_save_action();
    widget->m_save_as_action = widget->create_save_as_action();
    widget->m_new_project_action = TRY(widget->create_new_project_action());

    widget->m_add_editor_tab_widget_action = widget->create_add_editor_tab_widget_action();
    widget->m_add_editor_action = TRY(widget->create_add_editor_action());
    widget->m_add_terminal_action = TRY(widget->create_add_terminal_action());
    widget->m_remove_current_terminal_action = TRY(widget->create_remove_current_terminal_action());

    widget->m_locator = widget->add<Locator>();

    widget->m_terminal_wrapper->on_command_exit = [widget] {
        widget->m_stop_action->set_enabled(false);
    };

    widget->m_open_project_configuration_action = TRY(widget->create_open_project_configuration_action());
    widget->m_build_action = TRY(widget->create_build_action());
    widget->m_run_action = TRY(widget->create_run_action());
    widget->m_stop_action = TRY(widget->create_stop_action());
    widget->m_debug_action = TRY(widget->create_debug_action());

    widget->initialize_debugger();

    widget->create_toolbar(toolbar_container);

    widget->m_statusbar = widget->add<GUI::Statusbar>(3);
    widget->m_statusbar->segment(1).set_mode(GUI::Statusbar::Segment::Mode::Auto);
    widget->m_statusbar->segment(2).set_mode(GUI::Statusbar::Segment::Mode::Fixed);
    auto width = widget->font().width("Ln 0,000  Col 000"sv) + widget->font().max_glyph_width();
    widget->m_statusbar->segment(2).set_fixed_width(width);
    widget->update_statusbar();

    GUI::Application::the()->on_action_enter = [widget](GUI::Action& action) {
        widget->m_statusbar->set_override_text(action.status_tip());
    };

    GUI::Application::the()->on_action_leave = [widget](GUI::Action&) {
        widget->m_statusbar->set_override_text({});
    };

    auto maybe_watcher = Core::FileWatcher::create();
    if (maybe_watcher.is_error()) {
        warnln("Couldn't create a file watcher, deleted files won't be noticed! Error: {}", maybe_watcher.error());
    } else {
        widget->m_file_watcher = maybe_watcher.release_value();
        widget->m_file_watcher->on_change = [widget](Core::FileWatcherEvent const& event) {
            if (event.type != Core::FileWatcherEvent::Type::Deleted)
                return;

            if (event.event_path.starts_with(widget->project().root_path())) {
                ByteString relative_path = LexicalPath::relative_path(event.event_path, widget->project().root_path());
                widget->handle_external_file_deletion(relative_path);
            } else {
                widget->handle_external_file_deletion(event.event_path);
            }
        };
    }

    widget->project().model().set_should_show_dotfiles(Config::read_bool("HackStudio"sv, "Global"sv, "ShowDotfiles"sv, false));

    return widget;
}

void HackStudioWidget::update_actions()
{
    auto is_remove_terminal_enabled = [this]() {
        auto widget = m_action_tab_widget->active_widget();
        if (!widget)
            return false;
        if ("TerminalWrapper"sv != widget->class_name())
            return false;
        if (!reinterpret_cast<TerminalWrapper*>(widget)->user_spawned())
            return false;
        return true;
    };

    m_remove_current_editor_action->set_enabled(m_all_editor_wrappers.size() > 1);
    m_remove_current_terminal_action->set_enabled(is_remove_terminal_enabled());
}

void HackStudioWidget::on_action_tab_change()
{
    update_actions();
    if (auto* active_widget = m_action_tab_widget->active_widget()) {
        if (is<GitWidget>(*active_widget))
            static_cast<GitWidget&>(*active_widget).refresh();
    }
}

Vector<ByteString> HackStudioWidget::read_recent_projects()
{
    auto json = Config::read_string("HackStudio"sv, "Global"sv, "RecentProjects"sv);
    AK::JsonParser parser(json);
    auto value_or_error = parser.parse();
    if (value_or_error.is_error())
        return {};

    auto value = value_or_error.release_value();
    if (!value.is_array())
        return {};

    Vector<ByteString> paths;
    for (auto& json_value : value.as_array().values()) {
        if (!json_value.is_string())
            return {};
        paths.append(json_value.as_string());
    }

    return paths;
}

void HackStudioWidget::open_project(ByteString const& root_path)
{
    if (warn_unsaved_changes("There are unsaved changes, do you want to save before closing current project?") == ContinueDecision::No)
        return;
    auto absolute_root_path = FileSystem::absolute_path(root_path).release_value_but_fixme_should_propagate_errors();
    if (auto result = Core::System::chdir(absolute_root_path); result.is_error()) {
        warnln("Failed to open project: {}", result.release_error());
        exit(1);
    }
    if (m_project) {
        close_current_project();
    }
    m_project = Project::open_with_root_path(absolute_root_path);
    VERIFY(m_project);
    m_project_builder = make<ProjectBuilder>(*m_terminal_wrapper, *m_project);
    if (m_project_tree_view) {
        m_project_tree_view->set_model(m_project->model());
        m_project_tree_view->update();
    }
    if (m_git_widget->initialized()) {
        m_git_widget->change_repo(absolute_root_path);
        m_git_widget->refresh();
    }
    if (Debugger::is_initialized()) {
        auto& debugger = Debugger::the();
        debugger.reset_breakpoints();
        debugger.set_source_root(m_project->root_path());
    }
    for (auto& editor_wrapper : m_all_editor_wrappers)
        editor_wrapper->set_project_root(m_project->root_path());

    m_locations_history.clear();
    m_locations_history_end_index = 0;

    m_project->model().on_rename_successful = [this](auto& absolute_old_path, auto& absolute_new_path) {
        file_renamed(
            LexicalPath::relative_path(absolute_old_path, m_project->root_path()),
            LexicalPath::relative_path(absolute_new_path, m_project->root_path()));
    };

    GUI::Application::the()->set_most_recently_open_file(absolute_root_path);
}

Vector<ByteString> HackStudioWidget::selected_file_paths() const
{
    Vector<ByteString> files;
    m_project_tree_view->selection().for_each_index([&](const GUI::ModelIndex& index) {
        ByteString sub_path = index.data().as_string();

        GUI::ModelIndex parent_or_invalid = index.parent();

        while (parent_or_invalid.is_valid()) {
            sub_path = ByteString::formatted("{}/{}", parent_or_invalid.data().as_string(), sub_path);

            parent_or_invalid = parent_or_invalid.parent();
        }

        files.append(sub_path);
    });
    return files;
}

bool HackStudioWidget::open_file(ByteString const& full_filename, size_t line, size_t column)
{
    ByteString filename = full_filename;
    if (full_filename.starts_with(project().root_path())) {
        filename = LexicalPath::relative_path(full_filename, project().root_path());
    }
    if (FileSystem::is_directory(filename) || !FileSystem::exists(filename))
        return false;

    auto editor_wrapper_or_none = m_all_editor_wrappers.first_matching([&](auto& wrapper) {
        return wrapper->filename() == filename;
    });

    if (editor_wrapper_or_none.has_value()) {
        set_current_editor_wrapper(editor_wrapper_or_none.release_value());
        current_editor().set_cursor_and_focus_line(line, column);
        return true;
    } else if (active_file().is_empty() && !current_editor().document().is_modified() && !full_filename.is_empty()) {
        // Replace "Untitled" blank file since it hasn't been modified
    } else {
        add_new_editor(*m_current_editor_tab_widget);
    }

    if (!active_file().is_empty()) {
        // Since the file is previously open, it should always be in m_open_files.
        VERIFY(m_open_files.find(active_file()) != m_open_files.end());
        auto previous_open_project_file = m_open_files.get(active_file()).value();

        // Update the scrollbar values of the previous_open_project_file and save them to m_open_files.
        previous_open_project_file->vertical_scroll_value(current_editor().vertical_scrollbar().value());
        previous_open_project_file->horizontal_scroll_value(current_editor().horizontal_scrollbar().value());
    }

    RefPtr<ProjectFile> new_project_file = nullptr;
    if (auto it = m_open_files.find(filename); it != m_open_files.end()) {
        new_project_file = it->value;
    } else {
        new_project_file = m_project->create_file(filename);
        m_open_files.set(filename, *new_project_file);
        m_open_files_vector.append(filename);

        if (!m_file_watcher.is_null()) {
            auto watch_result = m_file_watcher->add_watch(filename, Core::FileWatcherEvent::Type::Deleted);
            if (watch_result.is_error()) {
                warnln("Couldn't watch '{}'", filename);
            }
        }
        m_open_files_view->model()->invalidate();
    }

    current_editor().on_cursor_change = nullptr; // Disable callback while we're swapping the document.
    current_editor().set_document(const_cast<GUI::TextDocument&>(new_project_file->document()));
    if (new_project_file->could_render_text()) {
        current_editor_wrapper().set_mode_displayable();
    } else {
        current_editor_wrapper().set_mode_non_displayable();
    }
    current_editor().horizontal_scrollbar().set_value(new_project_file->horizontal_scroll_value());
    current_editor().vertical_scrollbar().set_value(new_project_file->vertical_scroll_value());
    if (current_editor().editing_engine()->is_regular())
        current_editor().set_editing_engine(make<GUI::RegularEditingEngine>());
    else if (current_editor().editing_engine()->is_vim())
        current_editor().set_editing_engine(make<GUI::VimEditingEngine>());
    else
        VERIFY_NOT_REACHED();

    set_edit_mode(EditMode::Text);

    ByteString relative_file_path = filename;
    if (filename.starts_with(m_project->root_path()))
        relative_file_path = filename.substring(m_project->root_path().length() + 1);

    m_project_tree_view->update();

    current_editor_wrapper().set_filename(filename);
    update_current_editor_title();
    current_editor().set_focus(true);

    current_editor().on_cursor_change = [this] { on_cursor_change(); };
    current_editor().on_change = [this] { update_window_title(); };
    current_editor_wrapper().on_change = [this] { update_gml_preview(); };
    current_editor().set_cursor_and_focus_line(line, column);
    update_gml_preview();

    return true;
}

void HackStudioWidget::close_file_in_all_editors(ByteString const& filename)
{
    m_open_files.remove(filename);
    m_open_files_vector.remove_all_matching(
        [&filename](ByteString const& element) { return element == filename; });

    for (auto& editor_wrapper : m_all_editor_wrappers) {
        Editor& editor = editor_wrapper->editor();
        ByteString editor_file_path = editor.code_document().file_path();
        ByteString relative_editor_file_path = LexicalPath::relative_path(editor_file_path, project().root_path());

        if (relative_editor_file_path == filename) {
            if (m_open_files_vector.is_empty()) {
                editor.set_document(CodeDocument::create());
                editor_wrapper->set_filename("");
            } else {
                auto& first_path = m_open_files_vector[0];
                auto& document = m_open_files.get(first_path).value()->code_document();
                editor.set_document(document);
                editor_wrapper->set_filename(first_path);
            }
        }
    }

    m_open_files_view->model()->invalidate();
}

GUI::TabWidget& HackStudioWidget::current_editor_tab_widget()
{
    VERIFY(m_current_editor_tab_widget);
    return *m_current_editor_tab_widget;
}

GUI::TabWidget const& HackStudioWidget::current_editor_tab_widget() const
{
    VERIFY(m_current_editor_tab_widget);
    return *m_current_editor_tab_widget;
}

EditorWrapper& HackStudioWidget::current_editor_wrapper()
{
    VERIFY(m_current_editor_wrapper);
    return *m_current_editor_wrapper;
}

EditorWrapper const& HackStudioWidget::current_editor_wrapper() const
{
    VERIFY(m_current_editor_wrapper);
    return *m_current_editor_wrapper;
}

GUI::TextEditor& HackStudioWidget::current_editor()
{
    return current_editor_wrapper().editor();
}

GUI::TextEditor const& HackStudioWidget::current_editor() const
{
    return current_editor_wrapper().editor();
}

void HackStudioWidget::set_edit_mode(EditMode mode)
{
    if (mode == EditMode::Text) {
        m_right_hand_stack->set_active_widget(m_editors_splitter);
    } else if (mode == EditMode::Diff) {
        m_right_hand_stack->set_active_widget(m_diff_viewer);
    } else {
        VERIFY_NOT_REACHED();
    }
    m_right_hand_stack->active_widget()->update();
}

ErrorOr<NonnullRefPtr<GUI::Menu>> HackStudioWidget::create_project_tree_view_context_menu()
{
    TRY(m_new_file_actions.try_append(TRY(create_new_file_action("&C++ Source File", "/res/icons/16x16/filetype-cplusplus.png", "cpp"))));
    TRY(m_new_file_actions.try_append(TRY(create_new_file_action("C++ &Header File", "/res/icons/16x16/filetype-header.png", "h"))));
    TRY(m_new_file_actions.try_append(TRY(create_new_file_action("&GML File", "/res/icons/16x16/filetype-gml.png", "gml"))));
    TRY(m_new_file_actions.try_append(TRY(create_new_file_action("P&ython Source File", "/res/icons/16x16/filetype-python.png", "py"))));
    TRY(m_new_file_actions.try_append(TRY(create_new_file_action("Ja&va Source File", "/res/icons/16x16/filetype-java.png", "java"))));
    TRY(m_new_file_actions.try_append(TRY(create_new_file_action("C Source File", "/res/icons/16x16/filetype-c.png", "c"))));
    TRY(m_new_file_actions.try_append(TRY(create_new_file_action("&JavaScript Source File", "/res/icons/16x16/filetype-javascript.png", "js"))));
    TRY(m_new_file_actions.try_append(TRY(create_new_file_action("HT&ML File", "/res/icons/16x16/filetype-html.png", "html"))));
    TRY(m_new_file_actions.try_append(TRY(create_new_file_action("C&SS File", "/res/icons/16x16/filetype-css.png", "css"))));
    TRY(m_new_file_actions.try_append(TRY(create_new_file_action("&PHP File", "/res/icons/16x16/filetype-php.png", "php"))));
    TRY(m_new_file_actions.try_append(TRY(create_new_file_action("&Wasm File", "/res/icons/16x16/filetype-wasm.png", "wasm"))));
    TRY(m_new_file_actions.try_append(TRY(create_new_file_action("&INI File", "/res/icons/16x16/filetype-ini.png", "ini"))));
    TRY(m_new_file_actions.try_append(TRY(create_new_file_action("JS&ON File", "/res/icons/16x16/filetype-json.png", "json"))));
    TRY(m_new_file_actions.try_append(TRY(create_new_file_action("Mark&down File", "/res/icons/16x16/filetype-markdown.png", "md"))));

    m_new_plain_file_action = TRY(create_new_file_action("Plain &File", "/res/icons/16x16/new.png", ""));

    m_open_selected_action = TRY(create_open_selected_action());
    m_show_in_file_manager_action = create_show_in_file_manager_action();
    m_copy_relative_path_action = create_copy_relative_path_action();
    m_copy_full_path_action = create_copy_full_path_action();

    m_new_directory_action = TRY(create_new_directory_action());
    m_delete_action = create_delete_action();
    m_tree_view_rename_action = GUI::CommonActions::make_rename_action([this](GUI::Action const&) {
        m_project_tree_view->begin_editing(m_project_tree_view->cursor_index());
    });
    auto project_tree_view_context_menu = GUI::Menu::construct("Project Files"_string);

    auto new_file_submenu = project_tree_view_context_menu->add_submenu("N&ew..."_string);
    for (auto& new_file_action : m_new_file_actions) {
        new_file_submenu->add_action(new_file_action);
    }
    auto icon = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/new.png"sv));
    new_file_submenu->set_icon(icon);
    new_file_submenu->add_action(*m_new_plain_file_action);
    new_file_submenu->add_separator();
    new_file_submenu->add_action(*m_new_directory_action);

    project_tree_view_context_menu->add_action(*m_open_selected_action);
    project_tree_view_context_menu->add_action(*m_show_in_file_manager_action);
    project_tree_view_context_menu->add_action(*m_copy_relative_path_action);
    project_tree_view_context_menu->add_action(*m_copy_full_path_action);
    // TODO: Cut, copy, duplicate with new name...
    project_tree_view_context_menu->add_separator();
    project_tree_view_context_menu->add_action(*m_tree_view_rename_action);
    project_tree_view_context_menu->add_action(*m_delete_action);
    return project_tree_view_context_menu;
}

ErrorOr<NonnullRefPtr<GUI::Action>> HackStudioWidget::create_new_file_action(ByteString const& label, ByteString const& icon, ByteString const& extension)
{
    auto icon_no_shadow = TRY(Gfx::Bitmap::load_from_file(icon));
    return GUI::Action::create(label, icon_no_shadow, [this, extension](const GUI::Action&) {
        String filename;
        if (GUI::InputBox::show(window(), filename, "Enter a name:"sv, "New File"sv) != GUI::InputBox::ExecResult::OK)
            return;

        if (!extension.is_empty() && !AK::StringUtils::ends_with(filename, ByteString::formatted(".{}", extension), CaseSensitivity::CaseSensitive)) {
            filename = String::formatted("{}.{}", filename, extension).release_value_but_fixme_should_propagate_errors();
        }

        auto path_to_selected = selected_file_paths();

        ByteString filepath;

        if (!path_to_selected.is_empty()) {
            VERIFY(FileSystem::exists(path_to_selected.first()));

            LexicalPath selected(path_to_selected.first());

            ByteString dir_path;

            if (FileSystem::is_directory(selected.string()))
                dir_path = selected.string();
            else
                dir_path = selected.dirname();

            filepath = ByteString::formatted("{}/", dir_path);
        }

        filepath = ByteString::formatted("{}{}", filepath, filename);

        auto file_or_error = Core::File::open(filepath, Core::File::OpenMode::Write | Core::File::OpenMode::MustBeNew);
        if (file_or_error.is_error()) {
            GUI::MessageBox::show_error(window(), ByteString::formatted("Failed to create '{}': {}", filepath, file_or_error.error()));
            return;
        }
        open_file(filepath);
    });
}

ErrorOr<NonnullRefPtr<GUI::Action>> HackStudioWidget::create_new_directory_action()
{
    auto icon = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/mkdir.png"sv));
    return GUI::Action::create("&Directory...", { Mod_Ctrl | Mod_Shift, Key_N }, icon, [this](const GUI::Action&) {
        String directory_name;
        if (GUI::InputBox::show(window(), directory_name, "Enter a name:"sv, "New Directory"sv) != GUI::InputBox::ExecResult::OK)
            return;

        auto path_to_selected = selected_file_paths();

        if (!path_to_selected.is_empty()) {
            LexicalPath selected(path_to_selected.first());

            ByteString dir_path;

            if (FileSystem::is_directory(selected.string()))
                dir_path = selected.string();
            else
                dir_path = selected.dirname();

            directory_name = String::formatted("{}/{}", dir_path, directory_name).release_value_but_fixme_should_propagate_errors();
        }

        auto formatted_dir_name = LexicalPath::canonicalized_path(ByteString::formatted("{}/{}", m_project->model().root_path(), directory_name));
        int rc = mkdir(formatted_dir_name.characters(), 0755);
        if (rc < 0) {
            GUI::MessageBox::show(window(), "Failed to create new directory"sv, "Error"sv, GUI::MessageBox::Type::Error);
            return;
        }
    });
}

ErrorOr<NonnullRefPtr<GUI::Action>> HackStudioWidget::create_open_selected_action()
{
    auto open_selected_action = GUI::Action::create("&Open", [this](const GUI::Action&) {
        auto files = selected_file_paths();
        for (auto& file : files)
            open_file(file);
    });
    auto icon = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/open.png"sv));
    open_selected_action->set_icon(icon);
    open_selected_action->set_enabled(true);
    return open_selected_action;
}

NonnullRefPtr<GUI::Action> HackStudioWidget::create_show_in_file_manager_action()
{
    auto show_in_file_manager_action = GUI::Action::create("Show in File &Manager", [this](const GUI::Action&) {
        auto files = selected_file_paths();
        for (auto& file : files)
            Desktop::Launcher::open(URL::create_with_file_scheme(m_project->root_path(), file));
    });
    show_in_file_manager_action->set_enabled(true);
    show_in_file_manager_action->set_icon(GUI::Icon::default_icon("app-file-manager"sv).bitmap_for_size(16));

    return show_in_file_manager_action;
}

NonnullRefPtr<GUI::Action> HackStudioWidget::create_copy_relative_path_action()
{
    auto copy_relative_path_action = GUI::Action::create("Copy &Relative Path", [this](const GUI::Action&) {
        auto paths = selected_file_paths();
        VERIFY(!paths.is_empty());
        auto paths_string = ByteString::join('\n', paths);
        GUI::Clipboard::the().set_plain_text(paths_string);
    });
    copy_relative_path_action->set_enabled(true);
    copy_relative_path_action->set_icon(GUI::Icon::default_icon("hard-disk"sv).bitmap_for_size(16));

    return copy_relative_path_action;
}

NonnullRefPtr<GUI::Action> HackStudioWidget::create_copy_full_path_action()
{
    auto copy_full_path_action = GUI::Action::create("Copy &Full Path", [this](const GUI::Action&) {
        auto paths = selected_file_paths();
        VERIFY(!paths.is_empty());
        Vector<ByteString> full_paths;
        for (auto& path : paths)
            full_paths.append(get_absolute_path(path));
        auto paths_string = ByteString::join('\n', full_paths);
        GUI::Clipboard::the().set_plain_text(paths_string);
    });
    copy_full_path_action->set_enabled(true);
    copy_full_path_action->set_icon(GUI::Icon::default_icon("hard-disk"sv).bitmap_for_size(16));

    return copy_full_path_action;
}

NonnullRefPtr<GUI::Action> HackStudioWidget::create_delete_action()
{
    auto delete_action = GUI::CommonActions::make_delete_action([this](const GUI::Action&) {
        auto files = selected_file_paths();
        if (files.is_empty())
            return;

        ByteString message;
        if (files.size() == 1) {
            LexicalPath file(files[0]);
            message = ByteString::formatted("Really remove \"{}\" from disk?", file.basename());
        } else {
            message = ByteString::formatted("Really remove \"{}\" files from disk?", files.size());
        }

        auto result = GUI::MessageBox::show(window(),
            message,
            "Confirm Deletion"sv,
            GUI::MessageBox::Type::Warning,
            GUI::MessageBox::InputType::OKCancel);
        if (result == GUI::MessageBox::ExecResult::Cancel)
            return;

        for (auto& file : files) {
            struct stat st;
            if (lstat(file.characters(), &st) < 0) {
                GUI::MessageBox::show(window(),
                    ByteString::formatted("lstat ({}) failed: {}", file, strerror(errno)),
                    "Removal Failed"sv,
                    GUI::MessageBox::Type::Error);
                break;
            }

            bool is_directory = S_ISDIR(st.st_mode);
            if (auto result = FileSystem::remove(file, FileSystem::RecursionMode::Allowed); result.is_error()) {
                auto& error = result.error();
                if (is_directory) {
                    GUI::MessageBox::show(window(),
                        ByteString::formatted("Removing directory \"{}\" from the project failed: {}", file, error),
                        "Removal Failed"sv,
                        GUI::MessageBox::Type::Error);
                } else {
                    GUI::MessageBox::show(window(),
                        ByteString::formatted("Removing file \"{}\" from the project failed: {}", file, error),
                        "Removal Failed"sv,
                        GUI::MessageBox::Type::Error);
                }
            }
        }
    },
        m_project_tree_view);
    delete_action->set_enabled(false);
    return delete_action;
}

ErrorOr<NonnullRefPtr<GUI::Action>> HackStudioWidget::create_new_project_action()
{
    auto icon = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/hackstudio-project.png"sv));
    return GUI::Action::create(
        "&Project...", icon,
        [this](const GUI::Action&) {
            if (warn_unsaved_changes("There are unsaved changes. Would you like to save before creating a new project?") == ContinueDecision::No)
                return;
            // If the user wishes to save the changes, this occurs in warn_unsaved_changes. If they do not,
            // we need to mark the documents as clean so open_project works properly without asking again.
            for (auto& editor_wrapper : m_all_editor_wrappers)
                editor_wrapper->editor().document().set_unmodified();
            auto dialog = NewProjectDialog::construct(window());
            dialog->set_icon(window()->icon());
            auto result = dialog->exec();

            if (result == GUI::Dialog::ExecResult::OK && dialog->created_project_path().has_value())
                open_project(dialog->created_project_path().value());
        });
}

NonnullRefPtr<GUI::Action> HackStudioWidget::create_remove_current_editor_tab_widget_action()
{
    return GUI::Action::create("Switch to Next Editor Group", { Mod_Alt | Mod_Shift, Key_Backslash }, [this](auto&) {
        if (m_all_editor_tab_widgets.size() <= 1)
            return;
        auto tab_widget = m_current_editor_tab_widget;
        while (tab_widget->children().size() > 1) {
            auto active_wrapper = tab_widget->active_widget();
            tab_widget->remove_tab(*active_wrapper);
            m_all_editor_wrappers.remove_first_matching([&active_wrapper](auto& entry) { return entry == active_wrapper; });
        }
        tab_widget->on_tab_close_click(*tab_widget->active_widget());
    });
}

void HackStudioWidget::add_new_editor_tab_widget(GUI::Widget& parent)
{
    auto tab_widget = GUI::TabWidget::construct();
    if (m_action_tab_widget) {
        parent.insert_child_before(tab_widget, *m_action_tab_widget);
    } else {
        parent.add_child(tab_widget);
    }

    m_current_editor_tab_widget = tab_widget;
    m_all_editor_tab_widgets.append(tab_widget);

    tab_widget->set_reorder_allowed(true);

    if (m_all_editor_tab_widgets.size() > 1) {
        for (auto& widget : m_all_editor_tab_widgets)
            widget->set_close_button_enabled(true);
    }

    tab_widget->on_change = [&](auto& widget) {
        auto& wrapper = static_cast<EditorWrapper&>(widget);
        set_current_editor_wrapper(wrapper);
        current_editor().set_focus(true);
    };

    tab_widget->on_middle_click = [](auto& widget) {
        auto& wrapper = static_cast<EditorWrapper&>(widget);
        wrapper.on_tab_close_request(wrapper);
    };

    tab_widget->on_tab_close_click = [](auto& widget) {
        auto& wrapper = static_cast<EditorWrapper&>(widget);
        wrapper.on_tab_close_request(wrapper);
    };

    add_new_editor(*m_current_editor_tab_widget);
}

void HackStudioWidget::add_new_editor(GUI::TabWidget& parent)
{
    auto& wrapper = parent.add_tab<EditorWrapper>("(Untitled)"_string);
    parent.set_active_widget(&wrapper);
    if (parent.children().size() > 1 || m_all_editor_tab_widgets.size() > 1)
        parent.set_close_button_enabled(true);

    auto previous_editor_wrapper = m_current_editor_wrapper;
    m_current_editor_wrapper = wrapper;
    m_all_editor_wrappers.append(wrapper);
    wrapper.editor().set_focus(true);
    wrapper.editor().set_font(*m_editor_font);
    wrapper.editor().set_wrapping_mode(m_wrapping_mode);
    wrapper.set_project_root(m_project->root_path());
    wrapper.editor().on_cursor_change = [this] { on_cursor_change(); };
    wrapper.on_change = [this] { update_gml_preview(); };
    set_edit_mode(EditMode::Text);
    if (previous_editor_wrapper && previous_editor_wrapper->editor().editing_engine()->is_regular())
        wrapper.editor().set_editing_engine(make<GUI::RegularEditingEngine>());
    else if (previous_editor_wrapper && previous_editor_wrapper->editor().editing_engine()->is_vim())
        wrapper.editor().set_editing_engine(make<GUI::VimEditingEngine>());
    else
        wrapper.editor().set_editing_engine(make<GUI::RegularEditingEngine>());

    wrapper.on_tab_close_request = [this, &parent](auto& tab) {
        parent.deferred_invoke([this, &parent, &tab] {
            tab.editor().document().unregister_client(tab.editor());
            set_current_editor_wrapper(tab);
            parent.remove_tab(tab);
            m_all_editor_wrappers.remove_first_matching([&tab](auto& entry) { return entry == &tab; });
            if (parent.children().is_empty() && m_all_editor_tab_widgets.size() > 1) {
                m_switch_to_next_editor_tab_widget->activate();
                m_editors_splitter->remove_child(parent);
                m_all_editor_tab_widgets.remove_first_matching([&parent](auto& entry) { return entry == &parent; });
            }
            update_actions();
        });
    };
}

NonnullRefPtr<GUI::Action> HackStudioWidget::create_switch_to_next_editor_tab_widget_action()
{
    return GUI::Action::create("Switch to Next Editor Group", { Mod_Ctrl | Mod_Shift, Key_T }, [this](auto&) {
        if (m_all_editor_tab_widgets.size() <= 1)
            return;
        Vector<GUI::TabWidget&> tab_widgets;
        m_editors_splitter->for_each_child_of_type<GUI::TabWidget>([&tab_widgets](auto& child) {
            tab_widgets.append(child);
            return IterationDecision::Continue;
        });
        for (size_t i = 0; i < tab_widgets.size(); ++i) {
            if (m_current_editor_tab_widget.ptr() == &tab_widgets[i]) {
                if (i == tab_widgets.size() - 1)
                    m_current_editor_tab_widget = tab_widgets[0];
                else
                    m_current_editor_tab_widget = tab_widgets[i + 1];
                auto wrapper = static_cast<EditorWrapper*>(m_current_editor_tab_widget->active_widget());
                set_current_editor_wrapper(wrapper);
                current_editor().set_focus(true);
                break;
            }
        }
    });
}

NonnullRefPtr<GUI::Action> HackStudioWidget::create_switch_to_next_editor_action()
{
    return GUI::Action::create("Switch to &Next Editor", { Mod_Ctrl, Key_E }, [this](auto&) {
        m_current_editor_tab_widget->activate_next_tab();
    });
}

NonnullRefPtr<GUI::Action> HackStudioWidget::create_switch_to_previous_editor_action()
{
    return GUI::Action::create("Switch to &Previous Editor", { Mod_Ctrl | Mod_Shift, Key_E }, [this](auto&) {
        m_current_editor_tab_widget->activate_previous_tab();
    });
}

ErrorOr<NonnullRefPtr<GUI::Action>> HackStudioWidget::create_remove_current_editor_action()
{
    auto icon = TRY(Gfx::Bitmap::load_from_file("/res/icons/hackstudio/remove-editor.png"sv));
    return GUI::Action::create("&Remove Current Editor", { Mod_Alt | Mod_Shift, Key_E }, icon, [this](auto&) {
        if (m_all_editor_wrappers.size() <= 1)
            return;
        auto tab_widget = m_current_editor_tab_widget;
        auto* active_wrapper = tab_widget->active_widget();
        VERIFY(active_wrapper);
        tab_widget->on_tab_close_click(*active_wrapper);
        update_actions();
    });
}

ErrorOr<NonnullRefPtr<GUI::Action>> HackStudioWidget::create_toggle_open_file_in_single_click_action()
{
    return GUI::Action::create_checkable("&Open File with Single Click", [this](auto&) {
        m_project_tree_view->set_activates_on_selection(!m_project_tree_view->activates_on_selection());
    });
}

ErrorOr<NonnullRefPtr<GUI::Action>> HackStudioWidget::create_open_action()
{
    auto icon = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/open.png"sv));
    return GUI::Action::create("&Open Project...", { Mod_Ctrl | Mod_Shift, Key_O }, icon, [this](auto&) {
        auto open_path = GUI::FilePicker::get_open_filepath(window(), "Open Project", m_project->root_path(), true);
        if (!open_path.has_value())
            return;
        open_project(open_path.value());
        update_actions();
    });
}

NonnullRefPtr<GUI::Action> HackStudioWidget::create_save_action()
{
    return GUI::CommonActions::make_save_action([&](auto&) {
        if (active_file().is_empty())
            m_save_as_action->activate();

        // NOTE active_file() could still be empty after a cancelled save_as_action
        if (!active_file().is_empty())
            current_editor_wrapper().save();

        if (m_git_widget->initialized())
            m_git_widget->refresh();
    });
}

NonnullRefPtr<GUI::Action> HackStudioWidget::create_save_as_action()
{
    return GUI::CommonActions::make_save_as_action([&](auto&) {
        auto const old_filename = current_editor_wrapper().filename();
        LexicalPath const old_path(old_filename);

        auto suggested_path = FileSystem::absolute_path(old_path.string()).release_value_but_fixme_should_propagate_errors();
        Optional<ByteString> save_path = GUI::FilePicker::get_save_filepath(window(),
            old_filename.is_empty() ? "Untitled"sv : old_path.title(),
            old_filename.is_empty() ? "txt"sv : old_path.extension(),
            suggested_path);
        if (!save_path.has_value()) {
            return;
        }

        ByteString const relative_file_path = LexicalPath::relative_path(save_path.value(), m_project->root_path());
        if (current_editor_wrapper().filename().is_empty()) {
            current_editor_wrapper().set_filename(relative_file_path);
        } else {
            for (auto& editor_wrapper : m_all_editor_wrappers) {
                if (editor_wrapper->filename() == old_filename)
                    editor_wrapper->set_filename(relative_file_path);
            }
        }
        current_editor_wrapper().save();

        auto new_project_file = m_project->create_file(relative_file_path);
        m_open_files.set(relative_file_path, *new_project_file);
        m_open_files.remove(old_filename);

        m_open_files_vector.append(relative_file_path);
        m_open_files_vector.remove_all_matching([&old_filename](auto const& element) { return element == old_filename; });

        update_window_title();
        update_current_editor_title();

        m_project->model().invalidate();
        update_tree_view();
    });
}

ErrorOr<NonnullRefPtr<GUI::Action>> HackStudioWidget::create_remove_current_terminal_action()
{
    auto icon = TRY(Gfx::Bitmap::load_from_file("/res/icons/hackstudio/remove-terminal.png"sv));
    return GUI::Action::create("Remove &Current Terminal", { Mod_Alt | Mod_Shift, Key_T }, icon, [this](auto&) {
        auto widget = m_action_tab_widget->active_widget();
        if (!widget)
            return;
        if (!is<TerminalWrapper>(widget))
            return;
        auto& terminal = *static_cast<TerminalWrapper*>(widget);
        if (!terminal.user_spawned())
            return;
        m_action_tab_widget->remove_tab(terminal);
        update_actions();
    });
}

NonnullRefPtr<GUI::Action> HackStudioWidget::create_add_editor_tab_widget_action()
{
    return GUI::Action::create("Add New Editor Group", { Mod_Ctrl | Mod_Alt, Key_Backslash },
        [this](auto&) {
            add_new_editor_tab_widget(*m_editors_splitter);
            update_actions();
        });
}

ErrorOr<NonnullRefPtr<GUI::Action>> HackStudioWidget::create_add_editor_action()
{
    auto icon = TRY(Gfx::Bitmap::load_from_file("/res/icons/hackstudio/add-editor.png"sv));
    return GUI::Action::create("Add New &Editor", { Mod_Ctrl | Mod_Alt, Key_E },
        icon,
        [this](auto&) {
            add_new_editor(*m_current_editor_tab_widget);
            update_actions();
        });
}

ErrorOr<NonnullRefPtr<GUI::Action>> HackStudioWidget::create_add_terminal_action()
{
    auto icon = TRY(Gfx::Bitmap::load_from_file("/res/icons/hackstudio/add-terminal.png"sv));
    return GUI::Action::create("Add New &Terminal", { Mod_Ctrl | Mod_Alt, Key_T },
        icon,
        [this](auto&) {
            auto& terminal_wrapper = m_action_tab_widget->add_tab<TerminalWrapper>("Terminal"_string);
            terminal_wrapper.on_command_exit = [&]() {
                deferred_invoke([this]() {
                    m_action_tab_widget->remove_tab(*m_action_tab_widget->active_widget());
                });
            };
            reveal_action_tab(terminal_wrapper);
            update_actions();
            terminal_wrapper.terminal().set_focus(true);
        });
}

void HackStudioWidget::reveal_action_tab(GUI::Widget& widget)
{
    if (m_action_tab_widget->effective_min_size().height().as_int() < 200)
        m_action_tab_widget->set_preferred_height(200);
    m_action_tab_widget->set_active_widget(&widget);
}

ErrorOr<NonnullRefPtr<GUI::Action>> HackStudioWidget::create_debug_action()
{
    auto icon = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/debug-run.png"sv));
    return GUI::Action::create("&Debug", icon, [this](auto&) {
        if (!FileSystem::exists(get_project_executable_path())) {
            GUI::MessageBox::show(window(), ByteString::formatted("Could not find file: {}. (did you build the project?)", get_project_executable_path()), "Error"sv, GUI::MessageBox::Type::Error);
            return;
        }
        if (Debugger::the().session()) {
            GUI::MessageBox::show(window(), "Debugger is already running"sv, "Error"sv, GUI::MessageBox::Type::Error);
            return;
        }

        Debugger::the().set_executable_path(get_project_executable_path());

        m_terminal_wrapper->clear_including_history();

        // The debugger calls wait() on the debuggee, so the TerminalWrapper can't do that.
        auto ptm_res = m_terminal_wrapper->setup_master_pseudoterminal(TerminalWrapper::WaitForChildOnExit::No);
        if (ptm_res.is_error()) {
            warnln("Failed to set up master pseudoterminal: {}", ptm_res.release_error());
            return;
        }

        Debugger::the().set_child_setup_callback([this, ptm_res = ptm_res.release_value()]() {
            return m_terminal_wrapper->setup_slave_pseudoterminal(ptm_res);
        });

        m_debugger_thread = Threading::Thread::construct(Debugger::start_static);
        m_debugger_thread->start();
        m_stop_action->set_enabled(true);
        m_run_action->set_enabled(false);

        for (auto& editor_wrapper : m_all_editor_wrappers) {
            editor_wrapper->set_debug_mode(true);
        }
    });
}

void HackStudioWidget::initialize_debugger()
{
    Debugger::initialize(
        m_project->root_path(),
        [this](PtraceRegisters const& regs) {
            VERIFY(Debugger::the().session());
            auto const& debug_session = *Debugger::the().session();
            auto source_position = debug_session.get_source_position(regs.ip());
            if (!source_position.has_value()) {
                dbgln("Could not find source position for address: {:p}", regs.ip());
                return Debugger::HasControlPassedToUser::No;
            }
            dbgln("Debugger stopped at source position: {}:{}", source_position.value().file_path, source_position.value().line_number);

            GUI::Application::the()->event_loop().deferred_invoke([this, source_position, &regs] {
                m_current_editor_in_execution = get_editor_of_file(source_position.value().file_path);
                if (m_current_editor_in_execution)
                    m_current_editor_in_execution->editor().set_execution_position(source_position.value().line_number - 1);
                m_debug_info_widget->update_state(*Debugger::the().session(), regs);
                m_debug_info_widget->set_debug_actions_enabled(true, DebugInfoWidget::DebugActionsState::DebuggeeStopped);
                m_disassembly_widget->update_state(*Debugger::the().session(), regs);
                HackStudioWidget::reveal_action_tab(*m_debug_info_widget);
            });
            GUI::Application::the()->event_loop().wake();

            return Debugger::HasControlPassedToUser::Yes;
        },
        [this]() {
            GUI::Application::the()->event_loop().deferred_invoke([this] {
                m_debug_info_widget->set_debug_actions_enabled(true, DebugInfoWidget::DebugActionsState::DebuggeeRunning);
                if (m_current_editor_in_execution)
                    m_current_editor_in_execution->editor().clear_execution_position();
            });
            GUI::Application::the()->event_loop().wake();
        },
        [this]() {
            GUI::Application::the()->event_loop().deferred_invoke([this] {
                m_debug_info_widget->set_debug_actions_enabled(false, {});
                if (m_current_editor_in_execution)
                    m_current_editor_in_execution->editor().clear_execution_position();
                m_debug_info_widget->program_stopped();
                m_disassembly_widget->program_stopped();
                m_stop_action->set_enabled(false);
                m_run_action->set_enabled(true);
                m_debugger_thread.clear();

                for (auto& editor_wrapper : m_all_editor_wrappers) {
                    editor_wrapper->set_debug_mode(false);
                }

                HackStudioWidget::hide_action_tabs();
                GUI::MessageBox::show(window(), "Program Exited"sv, "Debugger"sv, GUI::MessageBox::Type::Information);
            });
            GUI::Application::the()->event_loop().wake();
        },
        [](float progress) {
            if (GUI::Application::the()->active_window())
                GUI::Application::the()->active_window()->set_progress(progress * 100);
        });
}

ByteString HackStudioWidget::get_full_path_of_serenity_source(ByteString const& file)
{
    auto path_parts = LexicalPath(file).parts();
    while (!path_parts.is_empty() && path_parts[0] == "..") {
        path_parts.remove(0);
    }
    StringBuilder relative_path_builder;
    relative_path_builder.join('/', path_parts);
    constexpr char SERENITY_LIBS_PREFIX[] = "/usr/src/serenity";
    LexicalPath serenity_sources_base(SERENITY_LIBS_PREFIX);
    return ByteString::formatted("{}/{}", serenity_sources_base, relative_path_builder.to_byte_string());
}

ByteString HackStudioWidget::get_absolute_path(ByteString const& path) const
{
    // TODO: We can probably do a more specific condition here, something like
    // "if (file.starts_with("../Libraries/") || file.starts_with("../AK/"))"
    if (path.starts_with(".."sv)) {
        return get_full_path_of_serenity_source(path);
    }
    return m_project->to_absolute_path(path);
}

RefPtr<EditorWrapper> HackStudioWidget::get_editor_of_file(ByteString const& filename)
{
    ByteString file_path = filename;

    if (filename.starts_with("../"sv)) {
        file_path = get_full_path_of_serenity_source(filename);
    }

    if (!open_file(file_path))
        return nullptr;
    return current_editor_wrapper();
}

ByteString HackStudioWidget::get_project_executable_path() const
{
    // FIXME: Dumb heuristic ahead!
    // e.g /my/project => /my/project/project
    // TODO: Perhaps a Makefile rule for getting the value of $(PROGRAM) would be better?
    return ByteString::formatted("{}/{}", m_project->root_path(), LexicalPath::basename(m_project->root_path()));
}

void HackStudioWidget::build()
{
    auto result = m_project_builder->build(active_file());
    if (result.is_error()) {
        GUI::MessageBox::show(window(), ByteString::formatted("{}", result.error()), "Build Failed"sv, GUI::MessageBox::Type::Error);
        m_build_action->set_enabled(true);
        m_stop_action->set_enabled(false);
    } else {
        m_stop_action->set_enabled(true);
    }
}

void HackStudioWidget::run()
{
    auto result = m_project_builder->run(active_file());
    if (result.is_error()) {
        GUI::MessageBox::show(window(), ByteString::formatted("{}", result.error()), "Run Failed"sv, GUI::MessageBox::Type::Error);
        m_run_action->set_enabled(true);
        m_stop_action->set_enabled(false);
    } else {
        m_stop_action->set_enabled(true);
    }
}

void HackStudioWidget::hide_action_tabs()
{
    m_action_tab_widget->set_preferred_height(24);
}

Project& HackStudioWidget::project()
{
    return *m_project;
}

void HackStudioWidget::set_current_editor_tab_widget(RefPtr<GUI::TabWidget> tab_widget)
{
    m_current_editor_tab_widget = tab_widget;
}

void HackStudioWidget::set_current_editor_wrapper(RefPtr<EditorWrapper> editor_wrapper)
{
    if (m_current_editor_wrapper)
        m_current_editor_wrapper->editor().hide_autocomplete();

    m_current_editor_wrapper = editor_wrapper;
    update_window_title();
    update_current_editor_title();
    update_tree_view();
    update_toolbar_actions();
    set_current_editor_tab_widget(static_cast<GUI::TabWidget*>(m_current_editor_wrapper->parent()));
    m_current_editor_tab_widget->set_active_widget(editor_wrapper);
    update_statusbar();
}

void HackStudioWidget::file_renamed(ByteString const& old_name, ByteString const& new_name)
{
    auto editor_or_none = m_all_editor_wrappers.first_matching([&old_name](auto const& editor) {
        return editor->filename() == old_name;
    });
    if (editor_or_none.has_value()) {
        (*editor_or_none)->set_filename(new_name);
        (*editor_or_none)->set_name(new_name);
    }

    if (m_open_files.contains(old_name)) {
        VERIFY(m_open_files_vector.remove_first_matching([&old_name](auto const& file) { return file == old_name; }));
        m_open_files_vector.append(new_name);

        ProjectFile* f = m_open_files.get(old_name).release_value();
        m_open_files.set(new_name, *f);
        m_open_files.remove(old_name);
        m_open_files_view->model()->invalidate();
    }

    if (m_file_watcher->is_watching(old_name)) {
        VERIFY(!m_file_watcher->remove_watch(old_name).is_error());
        VERIFY(!m_file_watcher->add_watch(new_name, Core::FileWatcherEvent::Type::Deleted).is_error());
    }

    update_window_title();
    update_current_editor_title();
}

void HackStudioWidget::configure_project_tree_view()
{
    m_project_tree_view->set_model(m_project->model());
    m_project_tree_view->set_selection_mode(GUI::AbstractView::SelectionMode::MultiSelection);
    m_project_tree_view->set_editable(true);
    m_project_tree_view->aid_create_editing_delegate = [](auto&) {
        return make<GUI::StringModelEditingDelegate>();
    };

    for (int column_index = 0; column_index < m_project->model().column_count(); ++column_index)
        m_project_tree_view->set_column_visible(column_index, false);

    m_project_tree_view->set_column_visible(GUI::FileSystemModel::Column::Name, true);

    m_project_tree_view->on_context_menu_request = [this](const GUI::ModelIndex& index, const GUI::ContextMenuEvent& event) {
        if (index.is_valid()) {
            m_project_tree_view_context_menu->popup(event.screen_position(), m_open_selected_action);
        }
    };

    m_project_tree_view->on_selection_change = [this] {
        m_open_selected_action->set_enabled(!m_project_tree_view->selection().is_empty());

        auto selections = m_project_tree_view->selection().indices();
        auto it = selections.find_if([&](auto selected_file) {
            return FileSystem::can_delete_or_move(m_project->model().full_path(selected_file));
        });
        bool has_permissions = it != selections.end();
        m_tree_view_rename_action->set_enabled(has_permissions);
        m_delete_action->set_enabled(has_permissions);
    };

    m_project_tree_view->on_activation = [this](auto& index) {
        auto full_path_to_file = m_project->model().full_path(index);
        open_file(full_path_to_file);
    };
}

void HackStudioWidget::create_open_files_view(GUI::Widget& parent)
{
    m_open_files_view = parent.add<GUI::ListView>();
    auto open_files_model = GUI::ItemListModel<ByteString>::create(m_open_files_vector);
    m_open_files_view->set_model(open_files_model);

    m_open_files_view->on_activation = [this](auto& index) {
        open_file(index.data().to_byte_string());
    };
}

void HackStudioWidget::create_toolbar(GUI::Widget& parent)
{
    auto& toolbar = parent.add<GUI::Toolbar>();
    toolbar.add_action(*m_new_plain_file_action);
    toolbar.add_action(*m_new_directory_action);
    toolbar.add_action(*m_save_action);
    toolbar.add_action(*m_delete_action);
    toolbar.add_separator();

    m_cut_button = toolbar.add_action(current_editor().cut_action());
    m_copy_button = toolbar.add_action(current_editor().copy_action());
    m_paste_button = toolbar.add_action(current_editor().paste_action());
    toolbar.add_separator();
    toolbar.add_action(GUI::CommonActions::make_undo_action([this](auto&) { current_editor().undo_action().activate(); }, m_editors_splitter));
    toolbar.add_action(GUI::CommonActions::make_redo_action([this](auto&) { current_editor().redo_action().activate(); }, m_editors_splitter));
    toolbar.add_separator();

    toolbar.add_action(*m_build_action);
    toolbar.add_separator();

    toolbar.add_action(*m_run_action);
    toolbar.add_action(*m_stop_action);
    toolbar.add_separator();

    toolbar.add_action(*m_debug_action);
}

ErrorOr<NonnullRefPtr<GUI::Action>> HackStudioWidget::create_build_action()
{
    auto icon = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/build.png"sv));
    return GUI::Action::create("&Build", { Mod_Ctrl, Key_B }, icon, [this](auto&) {
        if (m_auto_save_before_build_or_run) {
            if (!save_file_changes())
                return;
        } else {
            if (warn_unsaved_changes("There are unsaved changes, do you want to save before building?") == ContinueDecision::No)
                return;
        }

        reveal_action_tab(*m_terminal_wrapper);
        build();
    });
}

ErrorOr<NonnullRefPtr<GUI::Action>> HackStudioWidget::create_run_action()
{
    auto icon = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/program-run.png"sv));
    return GUI::Action::create("&Run", { Mod_Ctrl, Key_R }, icon, [this](auto&) {
        if (m_auto_save_before_build_or_run) {
            if (!save_file_changes())
                return;
        } else {
            if (warn_unsaved_changes("There are unsaved changes, do you want to save before running?") == ContinueDecision::No)
                return;
        }

        reveal_action_tab(*m_terminal_wrapper);
        run();
    });
}

ErrorOr<void> HackStudioWidget::create_action_tab(GUI::Widget& parent)
{
    m_action_tab_widget = parent.add<GUI::TabWidget>();

    m_action_tab_widget->set_preferred_height(24);
    m_action_tab_widget->on_change = [this](auto&) {
        on_action_tab_change();

        static bool first_time = true;
        if (!first_time)
            m_action_tab_widget->set_preferred_height(200);
        first_time = false;
    };

    m_find_in_files_widget = m_action_tab_widget->add_tab<FindInFilesWidget>("Find"_string);
    m_todo_entries_widget = m_action_tab_widget->add_tab<ToDoEntriesWidget>("TODO"_string);
    m_terminal_wrapper = m_action_tab_widget->add_tab<TerminalWrapper>("Console"_string, false);
    auto debug_info_widget = TRY(DebugInfoWidget::create());
    m_action_tab_widget->add_tab(debug_info_widget, "Debug"_string);
    m_debug_info_widget = debug_info_widget;

    m_debug_info_widget->on_backtrace_frame_selection = [this](Debug::DebugInfo::SourcePosition const& source_position) {
        open_file(get_absolute_path(source_position.file_path), source_position.line_number - 1);
    };

    m_disassembly_widget = m_action_tab_widget->add_tab<DisassemblyWidget>("Disassembly"_string);
    m_git_widget = m_action_tab_widget->add_tab<GitWidget>("Git"_string);
    m_git_widget->set_view_diff_callback([this](auto const& original_content, auto const& diff, auto const& file_path) {
        m_diff_viewer->set_content(original_content, diff);
        set_edit_mode(EditMode::Diff);
        StringBuilder diff_title;
        diff_title.append("Diff:", 5);
        diff_title.append(file_path.view());
        m_diff_viewer->window()->set_title(diff_title.to_byte_string());
    });
    m_gml_preview_widget = m_action_tab_widget->add_tab<GMLPreviewWidget>("GML Preview"_string, "");

    ToDoEntries::the().on_update = [this]() {
        m_todo_entries_widget->refresh();
    };

    return {};
}

void HackStudioWidget::create_project_tab(GUI::Widget& parent)
{
    m_project_tab = parent.add<GUI::TabWidget>();
    m_project_tab->set_tab_position(TabPosition::Bottom);

    auto& tree_view_container = m_project_tab->add_tab<GUI::Widget>("Files"_string);
    tree_view_container.set_layout<GUI::VerticalBoxLayout>();

    m_project_tree_view = tree_view_container.add<GUI::TreeView>();
    configure_project_tree_view();

    auto& class_view_container = m_project_tab->add_tab<GUI::Widget>("Classes"_string);
    class_view_container.set_layout<GUI::VerticalBoxLayout>();

    m_class_view = class_view_container.add<ClassViewWidget>();

    ProjectDeclarations::the().on_update = [this]() {
        m_class_view->refresh();
    };
}

ErrorOr<void> HackStudioWidget::create_file_menu(GUI::Window& window)
{
    auto file_menu = window.add_menu("&File"_string);

    auto new_submenu = file_menu->add_submenu("&New..."_string);
    new_submenu->add_action(*m_new_project_action);
    new_submenu->add_separator();
    for (auto& new_file_action : m_new_file_actions) {
        new_submenu->add_action(new_file_action);
    }

    {
        auto icon = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/new.png"sv));
        new_submenu->set_icon(icon);
    }
    new_submenu->add_action(*m_new_plain_file_action);
    new_submenu->add_separator();
    new_submenu->add_action(*m_new_directory_action);

    file_menu->add_action(*m_open_action);
    m_recent_projects_submenu = file_menu->add_submenu("Open &Recent"_string);
    {
        auto icon = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/open-recent.png"sv));
        m_recent_projects_submenu->set_icon(icon);
        m_recent_projects_submenu->add_recent_files_list(
            [this](GUI::Action& action) { open_project(action.text()); },
            GUI::Menu::AddTrailingSeparator::No);
    }
    file_menu->add_action(*m_save_action);
    file_menu->add_action(*m_save_as_action);
    file_menu->add_separator();
    file_menu->add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the()->quit();
    }));
    return {};
}

ErrorOr<void> HackStudioWidget::create_edit_menu(GUI::Window& window)
{
    auto edit_menu = window.add_menu("&Edit"_string);
    auto icon = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/find.png"sv));
    edit_menu->add_action(GUI::Action::create("&Find in Files...", { Mod_Ctrl | Mod_Shift, Key_F }, icon, [this](auto&) {
        reveal_action_tab(*m_find_in_files_widget);
        m_find_in_files_widget->focus_textbox_and_select_all();
    }));

    edit_menu->add_separator();

    auto vim_emulation_setting_action = GUI::Action::create_checkable("&Vim Emulation", { Mod_Ctrl | Mod_Shift | Mod_Alt, Key_V }, [this](auto& action) {
        if (action.is_checked()) {
            for (auto& editor_wrapper : m_all_editor_wrappers)
                editor_wrapper->editor().set_editing_engine(make<GUI::VimEditingEngine>());
        } else {
            for (auto& editor_wrapper : m_all_editor_wrappers)
                editor_wrapper->editor().set_editing_engine(make<GUI::RegularEditingEngine>());
        }
    });
    vim_emulation_setting_action->set_checked(false);
    edit_menu->add_action(vim_emulation_setting_action);

    auto auto_save_before_build_or_run_action = GUI::Action::create_checkable("&Auto Save before Build or Run", [this](auto& action) {
        m_auto_save_before_build_or_run = action.is_checked();
        Config::write_bool("HackStudio"sv, "Global"sv, "AutoSaveBeforeBuildOrRun"sv, m_auto_save_before_build_or_run);
    });
    m_auto_save_before_build_or_run = Config::read_bool("HackStudio"sv, "Global"sv, "AutoSaveBeforeBuildOrRun"sv, false);
    auto_save_before_build_or_run_action->set_checked(m_auto_save_before_build_or_run);
    edit_menu->add_action(auto_save_before_build_or_run_action);

    edit_menu->add_separator();
    edit_menu->add_action(*m_open_project_configuration_action);
    return {};
}

void HackStudioWidget::create_build_menu(GUI::Window& window)
{
    auto build_menu = window.add_menu("&Build"_string);
    build_menu->add_action(*m_build_action);
    build_menu->add_separator();
    build_menu->add_action(*m_run_action);
    build_menu->add_action(*m_stop_action);
    build_menu->add_separator();
    build_menu->add_action(*m_debug_action);
}

ErrorOr<void> HackStudioWidget::create_view_menu(GUI::Window& window)
{
    auto hide_action_tabs_action = GUI::Action::create("&Hide Action Tabs", { Mod_Ctrl | Mod_Shift, Key_X }, [this](auto&) {
        hide_action_tabs();
    });
    auto open_locator_action = GUI::Action::create("Open &Locator", { Mod_Ctrl, Key_K }, [this](auto&) {
        m_locator->open();
    });
    auto show_dotfiles_action = GUI::Action::create_checkable("S&how Dotfiles", { Mod_Ctrl, Key_H }, [&](auto& checked) {
        project().model().set_should_show_dotfiles(checked.is_checked());
        Config::write_bool("HackStudio"sv, "Global"sv, "ShowDotfiles"sv, checked.is_checked());
    });
    show_dotfiles_action->set_checked(Config::read_bool("HackStudio"sv, "Global"sv, "ShowDotfiles"sv, false));

    auto view_menu = window.add_menu("&View"_string);
    view_menu->add_action(hide_action_tabs_action);
    view_menu->add_action(open_locator_action);
    view_menu->add_action(show_dotfiles_action);
    m_toggle_semantic_highlighting_action = TRY(create_toggle_syntax_highlighting_mode_action());
    view_menu->add_action(*m_toggle_semantic_highlighting_action);
    m_toggle_view_file_in_single_click_action = TRY(create_toggle_open_file_in_single_click_action());
    view_menu->add_action(*m_toggle_view_file_in_single_click_action);
    view_menu->add_separator();

    m_wrapping_mode_actions.set_exclusive(true);
    auto wrapping_mode_menu = view_menu->add_submenu("&Wrapping Mode"_string);
    m_no_wrapping_action = GUI::Action::create_checkable("&No Wrapping", [&](auto&) {
        m_wrapping_mode = GUI::TextEditor::WrappingMode::NoWrap;
        for (auto& wrapper : m_all_editor_wrappers)
            wrapper->editor().set_wrapping_mode(GUI::TextEditor::WrappingMode::NoWrap);
    });
    m_wrap_anywhere_action = GUI::Action::create_checkable("Wrap &Anywhere", [&](auto&) {
        m_wrapping_mode = GUI::TextEditor::WrappingMode::WrapAnywhere;
        for (auto& wrapper : m_all_editor_wrappers)
            wrapper->editor().set_wrapping_mode(GUI::TextEditor::WrappingMode::WrapAnywhere);
    });
    m_wrap_at_words_action = GUI::Action::create_checkable("Wrap at &Words", [&](auto&) {
        m_wrapping_mode = GUI::TextEditor::WrappingMode::WrapAtWords;
        for (auto& wrapper : m_all_editor_wrappers)
            wrapper->editor().set_wrapping_mode(GUI::TextEditor::WrappingMode::WrapAtWords);
    });

    m_wrapping_mode_actions.add_action(*m_no_wrapping_action);
    m_wrapping_mode_actions.add_action(*m_wrap_anywhere_action);
    m_wrapping_mode_actions.add_action(*m_wrap_at_words_action);

    wrapping_mode_menu->add_action(*m_no_wrapping_action);
    wrapping_mode_menu->add_action(*m_wrap_anywhere_action);
    wrapping_mode_menu->add_action(*m_wrap_at_words_action);

    switch (m_wrapping_mode) {
    case GUI::TextEditor::NoWrap:
        m_no_wrapping_action->set_checked(true);
        break;
    case GUI::TextEditor::WrapAtWords:
        m_wrap_at_words_action->set_checked(true);
        break;
    case GUI::TextEditor::WrapAnywhere:
        m_wrap_anywhere_action->set_checked(true);
        break;
    }

    auto icon = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-font-editor.png"sv));
    m_editor_font_action = GUI::Action::create("Change &Font...", icon,
        [&](auto&) {
            auto picker = GUI::FontPicker::construct(&window, m_editor_font, false);
            if (picker->exec() == GUI::Dialog::ExecResult::OK) {
                change_editor_font(picker->font());
            }
        });
    view_menu->add_action(*m_editor_font_action);

    view_menu->add_separator();
    view_menu->add_action(*m_add_editor_tab_widget_action);
    view_menu->add_action(*m_add_editor_action);
    view_menu->add_action(*m_remove_current_editor_action);
    view_menu->add_action(*m_add_terminal_action);
    view_menu->add_action(*m_remove_current_terminal_action);

    view_menu->add_separator();

    TRY(create_location_history_actions());
    view_menu->add_action(*m_locations_history_back_action);
    view_menu->add_action(*m_locations_history_forward_action);

    view_menu->add_separator();

    view_menu->add_action(GUI::CommonActions::make_fullscreen_action([&](auto&) {
        window.set_fullscreen(!window.is_fullscreen());
    }));
    return {};
}

void HackStudioWidget::create_help_menu(GUI::Window& window)
{
    auto help_menu = window.add_menu("&Help"_string);
    help_menu->add_action(GUI::CommonActions::make_command_palette_action(&window));
    help_menu->add_action(GUI::CommonActions::make_about_action("Hack Studio"_string, GUI::Icon::default_icon("app-hack-studio"sv), &window));
}

ErrorOr<NonnullRefPtr<GUI::Action>> HackStudioWidget::create_stop_action()
{
    auto icon = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/program-stop.png"sv));
    auto action = GUI::Action::create("&Stop", icon, [this](auto&) {
        if (!Debugger::the().session()) {
            if (auto result = m_terminal_wrapper->kill_running_command(); result.is_error())
                warnln("{}", result.error());
            return;
        }

        Debugger::the().stop();
    });

    action->set_enabled(false);
    return action;
}

ErrorOr<void> HackStudioWidget::initialize_menubar(GUI::Window& window)
{
    TRY(create_file_menu(window));
    TRY(create_edit_menu(window));
    create_build_menu(window);
    TRY(create_view_menu(window));
    create_help_menu(window);
    return {};
}

void HackStudioWidget::update_statusbar()
{
    StringBuilder builder;
    if (current_editor().has_selection()) {
        ByteString selected_text = current_editor().selected_text();
        auto word_count = current_editor().number_of_selected_words();
        builder.appendff("Selected: {:'d} {} ({:'d} {})", selected_text.length(), selected_text.length() == 1 ? "character" : "characters", word_count, word_count != 1 ? "words" : "word");
    }

    m_statusbar->set_text(0, builder.to_string().release_value_but_fixme_should_propagate_errors());
    m_statusbar->set_text(1, String::from_utf8(Syntax::language_to_string(current_editor_wrapper().editor().code_document().language().value_or(Syntax::Language::PlainText))).release_value_but_fixme_should_propagate_errors());
    m_statusbar->set_text(2, String::formatted("Ln {:'d}  Col {:'d}", current_editor().cursor().line() + 1, current_editor().cursor().column()).release_value_but_fixme_should_propagate_errors());
}

void HackStudioWidget::handle_external_file_deletion(ByteString const& filepath)
{
    close_file_in_all_editors(filepath);
}

void HackStudioWidget::stop_debugger_if_running()
{
    if (!m_debugger_thread.is_null()) {
        Debugger::the().stop();
        dbgln("Waiting for debugger thread to terminate");
        auto rc = m_debugger_thread->join();
        if (rc.is_error()) {
            warnln("pthread_join: {}", strerror(rc.error().value()));
            dbgln("error joining debugger thread");
        }
    }
}

void HackStudioWidget::close_current_project()
{
    m_editors_splitter->remove_all_children();
    m_all_editor_tab_widgets.clear();
    m_all_editor_wrappers.clear();
    add_new_editor_tab_widget(*m_editors_splitter);
    m_open_files.clear();
    m_open_files_vector.clear();
    m_find_in_files_widget->reset();
    m_todo_entries_widget->clear();
    m_terminal_wrapper->clear_including_history();
    stop_debugger_if_running();
    update_gml_preview();
}

HackStudioWidget::~HackStudioWidget()
{
    stop_debugger_if_running();
}

HackStudioWidget::ContinueDecision HackStudioWidget::warn_unsaved_changes(ByteString const& prompt)
{
    if (!any_document_is_dirty())
        return ContinueDecision::Yes;

    auto result = GUI::MessageBox::show(window(), prompt, "Unsaved Changes"sv, GUI::MessageBox::Type::Warning, GUI::MessageBox::InputType::YesNoCancel);

    if (result == GUI::MessageBox::ExecResult::Cancel)
        return ContinueDecision::No;

    if (result == GUI::MessageBox::ExecResult::Yes) {
        if (!save_file_changes())
            return ContinueDecision::No;
    }

    return ContinueDecision::Yes;
}

bool HackStudioWidget::save_file_changes()
{
    for (auto& editor_wrapper : m_all_editor_wrappers) {
        if (editor_wrapper->editor().document().is_modified()) {
            if (!editor_wrapper->save())
                return false;
        }
    }

    return true;
}

bool HackStudioWidget::any_document_is_dirty() const
{
    return any_of(m_all_editor_wrappers, [](auto& editor_wrapper) {
        return editor_wrapper->editor().document().is_modified();
    });
}

void HackStudioWidget::update_gml_preview()
{
    auto gml_content = current_editor_wrapper().filename().ends_with(".gml"sv) ? current_editor_wrapper().editor().text() : ByteString::empty();
    m_gml_preview_widget->load_gml(gml_content);
}

void HackStudioWidget::update_tree_view()
{
    auto index = m_project->model().index(m_current_editor_wrapper->filename(), GUI::FileSystemModel::Column::Name);
    if (index.is_valid()) {
        m_project_tree_view->expand_all_parents_of(index);
        m_project_tree_view->set_cursor(index, GUI::AbstractView::SelectionUpdate::Set);
    }
}

void HackStudioWidget::update_toolbar_actions()
{
    m_copy_button->set_action(current_editor().copy_action());
    m_paste_button->set_action(current_editor().paste_action());
    m_cut_button->set_action(current_editor().cut_action());
}

void HackStudioWidget::update_window_title()
{
    window()->set_title(ByteString::formatted("{} - {} - Hack Studio", m_current_editor_wrapper->filename_title(), m_project->name()));
    window()->set_modified(any_document_is_dirty());
}

void HackStudioWidget::update_current_editor_title()
{
    current_editor_tab_widget().set_tab_title(current_editor_wrapper(), String::from_byte_string(current_editor_wrapper().filename_title()).release_value_but_fixme_should_propagate_errors());
}

void HackStudioWidget::on_cursor_change()
{
    update_statusbar();
    if (current_editor_wrapper().filename().is_empty())
        return;

    auto current_location = current_project_location();

    if (m_locations_history_end_index != 0) {
        auto last = m_locations_history[m_locations_history_end_index - 1];
        if (current_location.filename == last.filename && current_location.line == last.line)
            return;
    }

    // Clear "Go Forward" locations
    VERIFY(m_locations_history_end_index <= m_locations_history.size());
    m_locations_history.remove(m_locations_history_end_index, m_locations_history.size() - m_locations_history_end_index);

    m_locations_history.append(current_location);

    constexpr size_t max_locations = 30;
    if (m_locations_history.size() > max_locations)
        m_locations_history.take_first();

    m_locations_history_end_index = m_locations_history.size();

    update_history_actions();
}

ErrorOr<void> HackStudioWidget::create_location_history_actions()
{
    {
        auto icon = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/go-back.png"sv));
        m_locations_history_back_action = GUI::Action::create("Go Back", { Mod_Alt | Mod_Shift, Key_Left }, icon, [this](auto&) {
            if (m_locations_history_end_index <= 1)
                return;

            auto location = m_locations_history[m_locations_history_end_index - 2];
            --m_locations_history_end_index;

            m_locations_history_disabled = true;
            open_file(location.filename, location.line, location.column);
            m_locations_history_disabled = false;

            update_history_actions();
        });
    }

    {
        auto icon = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/go-forward.png"sv));
        m_locations_history_forward_action = GUI::Action::create("Go Forward", { Mod_Alt | Mod_Shift, Key_Right }, icon, [this](auto&) {
            if (m_locations_history_end_index == m_locations_history.size())
                return;

            auto location = m_locations_history[m_locations_history_end_index];
            ++m_locations_history_end_index;

            m_locations_history_disabled = true;
            open_file(location.filename, location.line, location.column);
            m_locations_history_disabled = false;

            update_history_actions();
        });
    }
    m_locations_history_forward_action->set_enabled(false);
    return {};
}

ErrorOr<NonnullRefPtr<GUI::Action>> HackStudioWidget::create_open_project_configuration_action()
{
    auto icon = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/settings.png"sv));
    return GUI::Action::create("Project Configuration", icon, [&](auto&) {
        auto parent_directory = LexicalPath::dirname(Project::config_file_path);
        auto absolute_config_file_path = LexicalPath::absolute_path(m_project->root_path(), Project::config_file_path);

        ByteString formatted_error_string_holder;
        auto save_configuration_or_error = [&]() -> ErrorOr<void> {
            if (FileSystem::exists(absolute_config_file_path))
                return {};

            if (FileSystem::exists(parent_directory) && !FileSystem::is_directory(parent_directory)) {
                formatted_error_string_holder = ByteString::formatted("Cannot create the '{}' directory because there is already a file with that name", parent_directory);
                return Error::from_string_view(formatted_error_string_holder.view());
            }

            auto maybe_error = Core::System::mkdir(LexicalPath::absolute_path(m_project->root_path(), parent_directory), 0755);
            if (maybe_error.is_error() && maybe_error.error().code() != EEXIST)
                return maybe_error.release_error();

            auto file = TRY(Core::File::open(absolute_config_file_path, Core::File::OpenMode::Write));
            TRY(file->write_until_depleted(
                "{\n"
                "    \"build_command\": \"your build command here\",\n"
                "    \"run_command\": \"your run command here\"\n"
                "}\n"sv.bytes()));
            return {};
        }();
        if (save_configuration_or_error.is_error()) {
            GUI::MessageBox::show_error(window(), ByteString::formatted("Saving configuration failed: {}.", save_configuration_or_error.error()));
            return;
        }

        open_file(Project::config_file_path);
    });
}

HackStudioWidget::ProjectLocation HackStudioWidget::current_project_location() const
{
    return ProjectLocation { current_editor_wrapper().filename(), current_editor().cursor().line(), current_editor().cursor().column() };
}

void HackStudioWidget::update_history_actions()
{
    if (m_locations_history_end_index <= 1)
        m_locations_history_back_action->set_enabled(false);
    else
        m_locations_history_back_action->set_enabled(true);

    if (m_locations_history_end_index == m_locations_history.size())
        m_locations_history_forward_action->set_enabled(false);
    else
        m_locations_history_forward_action->set_enabled(true);
}

RefPtr<Gfx::Font const> HackStudioWidget::read_editor_font_from_config()
{
    auto font_family = Config::read_string("HackStudio"sv, "EditorFont"sv, "Family"sv);
    auto font_variant = Config::read_string("HackStudio"sv, "EditorFont"sv, "Variant"sv);
    auto font_size = Config::read_i32("HackStudio"sv, "EditorFont"sv, "Size"sv);

    auto font = Gfx::FontDatabase::the().get(MUST(FlyString::from_deprecated_fly_string(font_family)), FlyString::from_deprecated_fly_string(font_variant).release_value_but_fixme_should_propagate_errors(), font_size);
    if (font.is_null())
        return Gfx::FontDatabase::the().default_fixed_width_font();

    return font;
}

void HackStudioWidget::change_editor_font(RefPtr<Gfx::Font const> font)
{
    m_editor_font = move(font);
    for (auto& editor_wrapper : m_all_editor_wrappers) {
        editor_wrapper->editor().set_font(*m_editor_font);
    }

    Config::write_string("HackStudio"sv, "EditorFont"sv, "Family"sv, m_editor_font->family());
    Config::write_string("HackStudio"sv, "EditorFont"sv, "Variant"sv, m_editor_font->variant());
    Config::write_i32("HackStudio"sv, "EditorFont"sv, "Size"sv, m_editor_font->presentation_size());
}

void HackStudioWidget::open_coredump(StringView coredump_path)
{
    open_project("/usr/src/serenity");
    m_mode = Mode::Coredump;

    m_coredump_inspector = Coredump::Inspector::create(coredump_path, [this](float progress) {
        window()->set_progress(progress * 100);
    });
    window()->set_progress(0);

    if (m_coredump_inspector) {
        m_debug_info_widget->update_state(*m_coredump_inspector, m_coredump_inspector->get_registers());
        reveal_action_tab(*m_debug_info_widget);
    }
}

void HackStudioWidget::debug_process(pid_t pid)
{
    open_project("/usr/src/serenity");
    Debugger::the().set_pid_to_attach(pid);

    m_debugger_thread = Threading::Thread::construct(Debugger::start_static);
    m_debugger_thread->start();
    m_stop_action->set_enabled(true);
    m_run_action->set_enabled(false);

    for (auto& editor_wrapper : m_all_editor_wrappers) {
        editor_wrapper->set_debug_mode(true);
    }
}

void HackStudioWidget::for_each_open_file(Function<void(ProjectFile const&)> func)
{
    for (auto& open_file : m_open_files) {
        func(*open_file.value);
    }
}

ErrorOr<NonnullRefPtr<GUI::Action>> HackStudioWidget::create_toggle_syntax_highlighting_mode_action()
{
    auto icon = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/filetype-cplusplus.png"sv));
    auto action = GUI::Action::create_checkable("&Semantic Highlighting", icon, [this](auto& action) {
        for (auto& editor_wrapper : m_all_editor_wrappers)
            editor_wrapper->editor().set_semantic_syntax_highlighting(action.is_checked());
    });

    return action;
}

bool HackStudioWidget::semantic_syntax_highlighting_is_enabled() const
{
    return m_toggle_semantic_highlighting_action->is_checked();
}
}
