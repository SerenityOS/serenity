/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 * Copyright (c) 2020-2021, the SerenityOS developers.
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
#include "HackStudioWidget.h"
#include "Locator.h"
#include "Project.h"
#include "ProjectDeclarations.h"
#include "TerminalWrapper.h"
#include "ToDoEntries.h"
#include <AK/LexicalPath.h>
#include <AK/StringBuilder.h>
#include <Kernel/API/InodeWatcherEvent.h>
#include <LibConfig/Client.h>
#include <LibCore/Event.h>
#include <LibCore/EventLoop.h>
#include <LibCore/File.h>
#include <LibCore/FileWatcher.h>
#include <LibDebug/DebugSession.h>
#include <LibDesktop/Launcher.h>
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
#include <LibGfx/FontDatabase.h>
#include <LibGfx/Palette.h>
#include <LibThreading/Mutex.h>
#include <LibThreading/Thread.h>
#include <LibVT/TerminalWidget.h>
#include <fcntl.h>
#include <spawn.h>
#include <stdio.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>

namespace HackStudio {

HackStudioWidget::HackStudioWidget(String path_to_project)
    : m_editor_font(read_editor_font_from_config())
{
    set_fill_with_background_color(true);
    set_layout<GUI::VerticalBoxLayout>();
    layout()->set_spacing(2);

    open_project(path_to_project);

    auto& toolbar_container = add<GUI::ToolbarContainer>();

    auto& outer_splitter = add<GUI::HorizontalSplitter>();
    outer_splitter.layout()->set_spacing(5);

    auto& left_hand_splitter = outer_splitter.add<GUI::VerticalSplitter>();
    left_hand_splitter.layout()->set_spacing(5);
    left_hand_splitter.set_fixed_width(150);
    create_project_tab(left_hand_splitter);
    m_project_tree_view_context_menu = create_project_tree_view_context_menu();

    create_open_files_view(left_hand_splitter);

    m_right_hand_splitter = outer_splitter.add<GUI::VerticalSplitter>();
    m_right_hand_stack = m_right_hand_splitter->add<GUI::StackWidget>();

    // Put a placeholder widget front & center since we don't have a file open yet.
    m_right_hand_stack->add<GUI::Widget>();

    m_diff_viewer = m_right_hand_stack->add<DiffViewer>();

    m_editors_splitter = m_right_hand_stack->add<GUI::VerticalSplitter>();
    m_editors_splitter->layout()->set_spacing(5);
    m_editors_splitter->layout()->set_margins({ 3, 0, 0 });
    add_new_editor(*m_editors_splitter);

    m_switch_to_next_editor = create_switch_to_next_editor_action();
    m_switch_to_previous_editor = create_switch_to_previous_editor_action();

    m_remove_current_editor_action = create_remove_current_editor_action();
    m_open_action = create_open_action();
    m_save_action = create_save_action();
    m_save_as_action = create_save_as_action();
    m_new_project_action = create_new_project_action();

    create_action_tab(*m_right_hand_splitter);

    m_add_editor_action = create_add_editor_action();
    m_add_terminal_action = create_add_terminal_action();
    m_remove_current_terminal_action = create_remove_current_terminal_action();

    m_locator = add<Locator>();

    m_terminal_wrapper->on_command_exit = [this] {
        m_stop_action->set_enabled(false);
    };

    m_build_action = create_build_action();
    m_run_action = create_run_action();
    m_stop_action = create_stop_action();
    m_debug_action = create_debug_action();

    initialize_debugger();

    create_toolbar(toolbar_container);

    m_statusbar = add<GUI::Statusbar>(3);

    auto maybe_watcher = Core::FileWatcher::create();
    if (maybe_watcher.is_error()) {
        warnln("Couldn't create a file watcher, deleted files won't be noticed! Error: {}", maybe_watcher.error());
    } else {
        m_file_watcher = maybe_watcher.release_value();
        m_file_watcher->on_change = [this](Core::FileWatcherEvent const& event) {
            if (event.type != Core::FileWatcherEvent::Type::Deleted)
                return;

            if (event.event_path.starts_with(project().root_path())) {
                String relative_path = LexicalPath::relative_path(event.event_path, project().root_path());
                handle_external_file_deletion(relative_path);
            } else {
                handle_external_file_deletion(event.event_path);
            }
        };
    }

    m_project_builder = make<ProjectBuilder>(*m_terminal_wrapper, *m_project);
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

void HackStudioWidget::open_project(const String& root_path)
{
    if (warn_unsaved_changes("There are unsaved changes, do you want to save before closing current project?") == ContinueDecision::No)
        return;
    if (chdir(root_path.characters()) < 0) {
        perror("chdir");
        exit(1);
    }
    if (m_project) {
        close_current_project();
    }
    m_project = Project::open_with_root_path(root_path);
    VERIFY(m_project);
    if (m_project_tree_view) {
        m_project_tree_view->set_model(m_project->model());
        m_project_tree_view->update();
    }
    if (m_git_widget && m_git_widget->initialized()) {
        m_git_widget->change_repo(root_path);
        m_git_widget->refresh();
    }
    if (Debugger::is_initialized()) {
        auto& debugger = Debugger::the();
        debugger.reset_breakpoints();
        debugger.set_source_root(m_project->root_path());
    }
    for (auto& editor_wrapper : m_all_editor_wrappers)
        editor_wrapper.set_project_root(m_project->root_path());

    m_locations_history.clear();
    m_locations_history_end_index = 0;

    m_project->model().on_rename_successful = [this](auto& absolute_old_path, auto& absolute_new_path) {
        file_renamed(
            LexicalPath::relative_path(absolute_old_path, m_project->root_path()),
            LexicalPath::relative_path(absolute_new_path, m_project->root_path()));
    };
}

Vector<String> HackStudioWidget::selected_file_paths() const
{
    Vector<String> files;
    m_project_tree_view->selection().for_each_index([&](const GUI::ModelIndex& index) {
        String sub_path = index.data().as_string();

        GUI::ModelIndex parent_or_invalid = index.parent();

        while (parent_or_invalid.is_valid()) {
            sub_path = String::formatted("{}/{}", parent_or_invalid.data().as_string(), sub_path);

            parent_or_invalid = parent_or_invalid.parent();
        }

        files.append(sub_path);
    });
    return files;
}

bool HackStudioWidget::open_file(const String& full_filename, size_t line, size_t column)
{
    String filename = full_filename;
    if (full_filename.starts_with(project().root_path())) {
        filename = LexicalPath::relative_path(full_filename, project().root_path());
    }
    if (Core::File::is_directory(filename) || !Core::File::exists(filename))
        return false;

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

    String relative_file_path = filename;
    if (filename.starts_with(m_project->root_path()))
        relative_file_path = filename.substring(m_project->root_path().length() + 1);

    m_project_tree_view->update();

    current_editor_wrapper().set_filename(filename);

    current_editor().set_focus(true);

    current_editor().on_cursor_change = [this] { on_cursor_change(); };
    current_editor_wrapper().on_change = [this] { update_gml_preview(); };
    current_editor().set_cursor(line, column);
    update_gml_preview();

    return true;
}

void HackStudioWidget::close_file_in_all_editors(String const& filename)
{
    m_open_files.remove(filename);
    m_open_files_vector.remove_all_matching(
        [&filename](String const& element) { return element == filename; });

    for (auto& editor_wrapper : m_all_editor_wrappers) {
        Editor& editor = editor_wrapper.editor();
        String editor_file_path = editor.code_document().file_path();
        String relative_editor_file_path = LexicalPath::relative_path(editor_file_path, project().root_path());

        if (relative_editor_file_path == filename) {
            if (m_open_files_vector.is_empty()) {
                editor.set_document(CodeDocument::create());
                editor_wrapper.set_filename("");
            } else {
                auto& first_path = m_open_files_vector[0];
                auto& document = m_open_files.get(first_path).value()->code_document();
                editor.set_document(document);
                editor_wrapper.set_filename(first_path);
            }
        }
    }

    m_open_files_view->model()->invalidate();
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

NonnullRefPtr<GUI::Menu> HackStudioWidget::create_project_tree_view_context_menu()
{
    m_new_file_actions.append(create_new_file_action("C++ Source File", "/res/icons/16x16/filetype-cplusplus.png", "cpp"));
    m_new_file_actions.append(create_new_file_action("C++ Header File", "/res/icons/16x16/filetype-header.png", "h"));
    // FIXME: Create a file icon for GML files
    m_new_file_actions.append(create_new_file_action("GML File", "/res/icons/16x16/new.png", "gml"));
    m_new_file_actions.append(create_new_file_action("JavaScript Source File", "/res/icons/16x16/filetype-javascript.png", "js"));
    m_new_file_actions.append(create_new_file_action("HTML File", "/res/icons/16x16/filetype-html.png", "html"));
    // FIXME: Create a file icon for CSS files
    m_new_file_actions.append(create_new_file_action("CSS File", "/res/icons/16x16/new.png", "css"));

    m_new_plain_file_action = create_new_file_action("Plain File", "/res/icons/16x16/new.png", "");

    m_open_selected_action = create_open_selected_action();
    m_show_in_file_manager_action = create_show_in_file_manager_action();

    m_new_directory_action = create_new_directory_action();
    m_delete_action = create_delete_action();
    m_tree_view_rename_action = GUI::CommonActions::make_rename_action([this](GUI::Action const&) {
        m_project_tree_view->begin_editing(m_project_tree_view->cursor_index());
    });
    auto project_tree_view_context_menu = GUI::Menu::construct("Project Files");

    auto& new_file_submenu = project_tree_view_context_menu->add_submenu("New");
    for (auto& new_file_action : m_new_file_actions) {
        new_file_submenu.add_action(new_file_action);
    }
    new_file_submenu.add_action(*m_new_plain_file_action);
    new_file_submenu.add_separator();
    new_file_submenu.add_action(*m_new_directory_action);

    project_tree_view_context_menu->add_action(*m_open_selected_action);
    project_tree_view_context_menu->add_action(*m_show_in_file_manager_action);
    // TODO: Cut, copy, duplicate with new name...
    project_tree_view_context_menu->add_separator();
    project_tree_view_context_menu->add_action(*m_tree_view_rename_action);
    project_tree_view_context_menu->add_action(*m_delete_action);
    return project_tree_view_context_menu;
}

NonnullRefPtr<GUI::Action> HackStudioWidget::create_new_file_action(String const& label, String const& icon, String const& extension)
{
    return GUI::Action::create(label, Gfx::Bitmap::try_load_from_file(icon).release_value_but_fixme_should_propagate_errors(), [this, extension](const GUI::Action&) {
        String filename;
        if (GUI::InputBox::show(window(), filename, "Enter name of new file:", "Add new file to project") != GUI::InputBox::ExecOK)
            return;

        if (!extension.is_empty() && !filename.ends_with(String::formatted(".{}", extension))) {
            filename = String::formatted("{}.{}", filename, extension);
        }

        auto path_to_selected = selected_file_paths();

        String filepath;

        if (!path_to_selected.is_empty()) {
            VERIFY(Core::File::exists(path_to_selected.first()));

            LexicalPath selected(path_to_selected.first());

            String dir_path;

            if (Core::File::is_directory(selected.string()))
                dir_path = selected.string();
            else
                dir_path = selected.dirname();

            filepath = String::formatted("{}/", dir_path);
        }

        filepath = String::formatted("{}{}", filepath, filename);

        auto file = Core::File::construct(filepath);
        if (!file->open((Core::OpenMode)(Core::OpenMode::WriteOnly | Core::OpenMode::MustBeNew))) {
            GUI::MessageBox::show(window(), String::formatted("Failed to create '{}'", filepath), "Error", GUI::MessageBox::Type::Error);
            return;
        }
        open_file(filepath);
    });
}

NonnullRefPtr<GUI::Action> HackStudioWidget::create_new_directory_action()
{
    return GUI::Action::create("&New Directory...", { Mod_Ctrl | Mod_Shift, Key_N }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/mkdir.png").release_value_but_fixme_should_propagate_errors(), [this](const GUI::Action&) {
        String directory_name;
        if (GUI::InputBox::show(window(), directory_name, "Enter name of new directory:", "Add new folder to project") != GUI::InputBox::ExecOK)
            return;

        auto path_to_selected = selected_file_paths();

        if (!path_to_selected.is_empty()) {
            LexicalPath selected(path_to_selected.first());

            String dir_path;

            if (Core::File::is_directory(selected.string()))
                dir_path = selected.string();
            else
                dir_path = selected.dirname();

            directory_name = String::formatted("{}/{}", dir_path, directory_name);
        }

        auto formatted_dir_name = LexicalPath::canonicalized_path(String::formatted("{}/{}", m_project->model().root_path(), directory_name));
        int rc = mkdir(formatted_dir_name.characters(), 0755);
        if (rc < 0) {
            GUI::MessageBox::show(window(), "Failed to create new directory", "Error", GUI::MessageBox::Type::Error);
            return;
        }
    });
}

NonnullRefPtr<GUI::Action> HackStudioWidget::create_open_selected_action()
{
    auto open_selected_action = GUI::Action::create("Open", [this](const GUI::Action&) {
        auto files = selected_file_paths();
        for (auto& file : files)
            open_file(file);
    });
    open_selected_action->set_enabled(true);
    return open_selected_action;
}

NonnullRefPtr<GUI::Action> HackStudioWidget::create_show_in_file_manager_action()
{
    auto show_in_file_manager_action = GUI::Action::create("Show in File Manager", [this](const GUI::Action&) {
        auto files = selected_file_paths();
        for (auto& file : files)
            Desktop::Launcher::open(URL::create_with_file_protocol(m_project->root_path(), file));
    });
    show_in_file_manager_action->set_enabled(true);
    show_in_file_manager_action->set_icon(GUI::Icon::default_icon("app-file-manager").bitmap_for_size(16));

    return show_in_file_manager_action;
}

NonnullRefPtr<GUI::Action> HackStudioWidget::create_delete_action()
{
    auto delete_action = GUI::CommonActions::make_delete_action([this](const GUI::Action&) {
        auto files = selected_file_paths();
        if (files.is_empty())
            return;

        String message;
        if (files.size() == 1) {
            LexicalPath file(files[0]);
            message = String::formatted("Really remove {} from disk?", file.basename());
        } else {
            message = String::formatted("Really remove {} files from disk?", files.size());
        }

        auto result = GUI::MessageBox::show(window(),
            message,
            "Confirm deletion",
            GUI::MessageBox::Type::Warning,
            GUI::MessageBox::InputType::OKCancel);
        if (result == GUI::MessageBox::ExecCancel)
            return;

        for (auto& file : files) {
            struct stat st;
            if (lstat(file.characters(), &st) < 0) {
                GUI::MessageBox::show(window(),
                    String::formatted("lstat ({}) failed: {}", file, strerror(errno)),
                    "Removal failed",
                    GUI::MessageBox::Type::Error);
                break;
            }

            bool is_directory = S_ISDIR(st.st_mode);
            if (auto result = Core::File::remove(file, Core::File::RecursionMode::Allowed, false); !result.is_error()) {
                auto& error = result.error();
                if (is_directory) {
                    GUI::MessageBox::show(window(),
                        String::formatted("Removing directory {} from the project failed: {}", error.file, static_cast<Error const&>(error)),
                        "Removal failed",
                        GUI::MessageBox::Type::Error);
                } else {
                    GUI::MessageBox::show(window(),
                        String::formatted("Removing file {} from the project failed: {}", error.file, static_cast<Error const&>(error)),
                        "Removal failed",
                        GUI::MessageBox::Type::Error);
                }
            }
        }
    },
        m_project_tree_view);
    delete_action->set_enabled(false);
    return delete_action;
}

NonnullRefPtr<GUI::Action> HackStudioWidget::create_new_project_action()
{
    return GUI::Action::create("&New Project...", { Mod_Ctrl | Mod_Shift, Key_N }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/hackstudio-project.png").release_value_but_fixme_should_propagate_errors(), [this](const GUI::Action&) {
        auto dialog = NewProjectDialog::construct(window());
        dialog->set_icon(window()->icon());
        auto result = dialog->exec();

        if (result == GUI::Dialog::ExecResult::ExecOK && dialog->created_project_path().has_value())
            open_project(dialog->created_project_path().value());
    });
}

void HackStudioWidget::add_new_editor(GUI::Widget& parent)
{
    auto wrapper = EditorWrapper::construct();
    if (m_action_tab_widget) {
        parent.insert_child_before(wrapper, *m_action_tab_widget);
    } else {
        parent.add_child(wrapper);
    }
    auto previous_editor_wrapper = m_current_editor_wrapper;
    m_current_editor_wrapper = wrapper;
    m_all_editor_wrappers.append(wrapper);
    wrapper->editor().set_focus(true);
    wrapper->editor().set_font(*m_editor_font);
    wrapper->set_project_root(m_project->root_path());
    wrapper->editor().on_cursor_change = [this] { on_cursor_change(); };
    wrapper->on_change = [this] { update_gml_preview(); };
    set_edit_mode(EditMode::Text);
    if (previous_editor_wrapper && previous_editor_wrapper->editor().editing_engine()->is_regular())
        wrapper->editor().set_editing_engine(make<GUI::RegularEditingEngine>());
    else if (previous_editor_wrapper && previous_editor_wrapper->editor().editing_engine()->is_vim())
        wrapper->editor().set_editing_engine(make<GUI::VimEditingEngine>());
    else
        wrapper->editor().set_editing_engine(make<GUI::RegularEditingEngine>());
}

NonnullRefPtr<GUI::Action> HackStudioWidget::create_switch_to_next_editor_action()
{
    return GUI::Action::create("Switch to &Next Editor", { Mod_Ctrl, Key_E }, [this](auto&) {
        if (m_all_editor_wrappers.size() <= 1)
            return;
        Vector<EditorWrapper&> wrappers;
        m_editors_splitter->for_each_child_of_type<EditorWrapper>([&wrappers](auto& child) {
            wrappers.append(child);
            return IterationDecision::Continue;
        });
        for (size_t i = 0; i < wrappers.size(); ++i) {
            if (m_current_editor_wrapper.ptr() == &wrappers[i]) {
                if (i == wrappers.size() - 1)
                    wrappers[0].editor().set_focus(true);
                else
                    wrappers[i + 1].editor().set_focus(true);
            }
        }
    });
}

NonnullRefPtr<GUI::Action> HackStudioWidget::create_switch_to_previous_editor_action()
{
    return GUI::Action::create("Switch to &Previous Editor", { Mod_Ctrl | Mod_Shift, Key_E }, [this](auto&) {
        if (m_all_editor_wrappers.size() <= 1)
            return;
        Vector<EditorWrapper&> wrappers;
        m_editors_splitter->for_each_child_of_type<EditorWrapper>([&wrappers](auto& child) {
            wrappers.append(child);
            return IterationDecision::Continue;
        });
        for (int i = wrappers.size() - 1; i >= 0; --i) {
            if (m_current_editor_wrapper.ptr() == &wrappers[i]) {
                if (i == 0)
                    wrappers.last().editor().set_focus(true);
                else
                    wrappers[i - 1].editor().set_focus(true);
            }
        }
    });
}

NonnullRefPtr<GUI::Action> HackStudioWidget::create_remove_current_editor_action()
{
    return GUI::Action::create("&Remove Current Editor", { Mod_Alt | Mod_Shift, Key_E }, [this](auto&) {
        if (m_all_editor_wrappers.size() <= 1)
            return;
        auto wrapper = m_current_editor_wrapper;
        m_switch_to_next_editor->activate();
        m_editors_splitter->remove_child(*wrapper);

        auto child_editors = m_editors_splitter->child_widgets();
        bool has_child_to_fill_space = false;
        for (auto& editor : child_editors) {
            if (editor.max_height() == -1) {
                has_child_to_fill_space = true;
                break;
            }
        }
        if (!has_child_to_fill_space)
            child_editors.last().set_max_height(-1);

        m_all_editor_wrappers.remove_first_matching([&wrapper](auto& entry) { return entry == wrapper.ptr(); });
        update_actions();
    });
}

NonnullRefPtr<GUI::Action> HackStudioWidget::create_open_action()
{
    return GUI::Action::create("&Open Project...", { Mod_Ctrl | Mod_Shift, Key_O }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/open.png").release_value_but_fixme_should_propagate_errors(), [this](auto&) {
        auto open_path = GUI::FilePicker::get_open_filepath(window(), "Open project", m_project->root_path(), true);
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

        Optional<String> save_path = GUI::FilePicker::get_save_filepath(window(),
            old_filename.is_null() ? "Untitled" : old_path.title(),
            old_filename.is_null() ? "txt" : old_path.extension(),
            Core::File::absolute_path(old_path.dirname()));
        if (!save_path.has_value()) {
            return;
        }

        String const relative_file_path = LexicalPath::relative_path(save_path.value(), m_project->root_path());
        if (current_editor_wrapper().filename().is_null()) {
            current_editor_wrapper().set_filename(relative_file_path);
        } else {
            for (auto& editor_wrapper : m_all_editor_wrappers) {
                if (editor_wrapper.filename() == old_filename)
                    editor_wrapper.set_filename(relative_file_path);
            }
        }
        current_editor_wrapper().save();

        auto new_project_file = m_project->create_file(relative_file_path);
        m_open_files.set(relative_file_path, *new_project_file);
        m_open_files.remove(old_filename);

        m_open_files_vector.append(relative_file_path);
        m_open_files_vector.remove_all_matching([&old_filename](auto const& element) { return element == old_filename; });

        update_window_title();

        m_project->model().invalidate();
        update_tree_view();
    });
}

NonnullRefPtr<GUI::Action> HackStudioWidget::create_remove_current_terminal_action()
{
    return GUI::Action::create("Remove &Current Terminal", { Mod_Alt | Mod_Shift, Key_T }, [this](auto&) {
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

NonnullRefPtr<GUI::Action> HackStudioWidget::create_add_editor_action()
{
    return GUI::Action::create("Add New &Editor", { Mod_Ctrl | Mod_Alt, Key_E },
        Gfx::Bitmap::try_load_from_file("/res/icons/16x16/app-text-editor.png").release_value_but_fixme_should_propagate_errors(),
        [this](auto&) {
            add_new_editor(*m_editors_splitter);
            update_actions();
        });
}

NonnullRefPtr<GUI::Action> HackStudioWidget::create_add_terminal_action()
{
    return GUI::Action::create("Add New &Terminal", { Mod_Ctrl | Mod_Alt, Key_T },
        Gfx::Bitmap::try_load_from_file("/res/icons/16x16/app-terminal.png").release_value_but_fixme_should_propagate_errors(),
        [this](auto&) {
            auto& terminal_wrapper = m_action_tab_widget->add_tab<TerminalWrapper>("Terminal");
            reveal_action_tab(terminal_wrapper);
            update_actions();
            terminal_wrapper.terminal().set_focus(true);
        });
}

void HackStudioWidget::reveal_action_tab(GUI::Widget& widget)
{
    if (m_action_tab_widget->min_height() < 200)
        m_action_tab_widget->set_fixed_height(200);
    m_action_tab_widget->set_active_widget(&widget);
}

NonnullRefPtr<GUI::Action> HackStudioWidget::create_debug_action()
{
    return GUI::Action::create("&Debug", Gfx::Bitmap::try_load_from_file("/res/icons/16x16/debug-run.png").release_value_but_fixme_should_propagate_errors(), [this](auto&) {
        if (!Core::File::exists(get_project_executable_path())) {
            GUI::MessageBox::show(window(), String::formatted("Could not find file: {}. (did you build the project?)", get_project_executable_path()), "Error", GUI::MessageBox::Type::Error);
            return;
        }
        if (Debugger::the().session()) {
            GUI::MessageBox::show(window(), "Debugger is already running", "Error", GUI::MessageBox::Type::Error);
            return;
        }

        Debugger::the().set_executable_path(get_project_executable_path());

        m_terminal_wrapper->clear_including_history();

        // The debugger calls wait() on the debugee, so the TerminalWrapper can't do that.
        auto ptm_res = m_terminal_wrapper->setup_master_pseudoterminal(TerminalWrapper::WaitForChildOnExit::No);
        if (ptm_res.is_error()) {
            perror("setup_master_pseudoterminal");
            return;
        }

        Debugger::the().set_child_setup_callback([this, ptm_res]() {
            return m_terminal_wrapper->setup_slave_pseudoterminal(ptm_res.value());
        });

        m_debugger_thread = Threading::Thread::construct(Debugger::start_static);
        m_debugger_thread->start();
        m_stop_action->set_enabled(true);
        m_run_action->set_enabled(false);

        for (auto& editor_wrapper : m_all_editor_wrappers) {
            editor_wrapper.set_debug_mode(true);
        }
    });
}

void HackStudioWidget::initialize_debugger()
{
    Debugger::initialize(
        m_project->root_path(),
        [this](const PtraceRegisters& regs) {
            VERIFY(Debugger::the().session());
            const auto& debug_session = *Debugger::the().session();
            auto source_position = debug_session.get_source_position(regs.ip());
            if (!source_position.has_value()) {
                dbgln("Could not find source position for address: {:p}", regs.ip());
                return Debugger::HasControlPassedToUser::No;
            }
            dbgln("Debugger stopped at source position: {}:{}", source_position.value().file_path, source_position.value().line_number);

            deferred_invoke([this, source_position, &regs] {
                m_current_editor_in_execution = get_editor_of_file(source_position.value().file_path);
                if (m_current_editor_in_execution)
                    m_current_editor_in_execution->editor().set_execution_position(source_position.value().line_number - 1);
                m_debug_info_widget->update_state(*Debugger::the().session(), regs);
                m_debug_info_widget->set_debug_actions_enabled(true);
                m_disassembly_widget->update_state(*Debugger::the().session(), regs);
                HackStudioWidget::reveal_action_tab(*m_debug_info_widget);
            });
            Core::EventLoop::wake();

            return Debugger::HasControlPassedToUser::Yes;
        },
        [this]() {
            deferred_invoke([this] {
                m_debug_info_widget->set_debug_actions_enabled(false);
                if (m_current_editor_in_execution)
                    m_current_editor_in_execution->editor().clear_execution_position();
            });
            Core::EventLoop::wake();
        },
        [this]() {
            deferred_invoke([this] {
                m_debug_info_widget->set_debug_actions_enabled(false);
                if (m_current_editor_in_execution)
                    m_current_editor_in_execution->editor().clear_execution_position();
                m_debug_info_widget->program_stopped();
                m_disassembly_widget->program_stopped();
                m_stop_action->set_enabled(false);
                m_run_action->set_enabled(true);
                m_debugger_thread.clear();

                for (auto& editor_wrapper : m_all_editor_wrappers) {
                    editor_wrapper.set_debug_mode(false);
                }

                HackStudioWidget::hide_action_tabs();
                GUI::MessageBox::show(window(), "Program Exited", "Debugger", GUI::MessageBox::Type::Information);
            });
            Core::EventLoop::wake();
        });
}

String HackStudioWidget::get_full_path_of_serenity_source(const String& file)
{
    auto path_parts = LexicalPath(file).parts();
    while (!path_parts.is_empty() && path_parts[0] == "..") {
        path_parts.remove(0);
    }
    StringBuilder relative_path_builder;
    relative_path_builder.join("/", path_parts);
    constexpr char SERENITY_LIBS_PREFIX[] = "/usr/src/serenity";
    LexicalPath serenity_sources_base(SERENITY_LIBS_PREFIX);
    return String::formatted("{}/{}", serenity_sources_base, relative_path_builder.to_string());
}

String HackStudioWidget::get_absolute_path(const String& path) const
{
    // TODO: We can probably do a more specific condition here, something like
    // "if (file.starts_with("../Libraries/") || file.starts_with("../AK/"))"
    if (path.starts_with("..")) {
        return get_full_path_of_serenity_source(path);
    }
    return m_project->to_absolute_path(path);
}

RefPtr<EditorWrapper> HackStudioWidget::get_editor_of_file(const String& filename)
{
    String file_path = filename;

    if (filename.starts_with("../")) {
        file_path = get_full_path_of_serenity_source(filename);
    }

    if (!open_file(file_path))
        return nullptr;
    return current_editor_wrapper();
}

String HackStudioWidget::get_project_executable_path() const
{
    // FIXME: Dumb heuristic ahead!
    // e.g /my/project => /my/project/project
    // TODO: Perhaps a Makefile rule for getting the value of $(PROGRAM) would be better?
    return String::formatted("{}/{}", m_project->root_path(), LexicalPath::basename(m_project->root_path()));
}

void HackStudioWidget::build()
{
    auto result = m_project_builder->build(active_file());
    if (result.is_error()) {
        GUI::MessageBox::show(window(), String::formatted("{}", result.error()), "Build failed", GUI::MessageBox::Type::Error);
    }
}

void HackStudioWidget::run()
{
    auto result = m_project_builder->run(active_file());
    if (result.is_error()) {
        GUI::MessageBox::show(window(), String::formatted("{}", result.error()), "Run failed", GUI::MessageBox::Type::Error);
    }
}

void HackStudioWidget::hide_action_tabs()
{
    m_action_tab_widget->set_fixed_height(24);
};

Project& HackStudioWidget::project()
{
    return *m_project;
}

void HackStudioWidget::set_current_editor_wrapper(RefPtr<EditorWrapper> editor_wrapper)
{
    m_current_editor_wrapper = editor_wrapper;
    update_window_title();
    update_tree_view();
}

void HackStudioWidget::file_renamed(String const& old_name, String const& new_name)
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
            return access(m_project->model().full_path(selected_file.parent()).characters(), W_OK) == 0;
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
    auto open_files_model = GUI::ItemListModel<String>::create(m_open_files_vector);
    m_open_files_view->set_model(open_files_model);

    m_open_files_view->on_activation = [this](auto& index) {
        open_file(index.data().to_string());
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

    toolbar.add_action(GUI::CommonActions::make_cut_action([this](auto&) { current_editor().cut_action().activate(); }));
    toolbar.add_action(GUI::CommonActions::make_copy_action([this](auto&) { current_editor().copy_action().activate(); }));
    toolbar.add_action(GUI::CommonActions::make_paste_action([this](auto&) { current_editor().paste_action().activate(); }));
    toolbar.add_separator();
    toolbar.add_action(GUI::CommonActions::make_undo_action([this](auto&) { current_editor().undo_action().activate(); }));
    toolbar.add_action(GUI::CommonActions::make_redo_action([this](auto&) { current_editor().redo_action().activate(); }));
    toolbar.add_separator();

    toolbar.add_action(*m_build_action);
    toolbar.add_separator();

    toolbar.add_action(*m_run_action);
    toolbar.add_action(*m_stop_action);
    toolbar.add_separator();

    toolbar.add_action(*m_debug_action);
}

NonnullRefPtr<GUI::Action> HackStudioWidget::create_build_action()
{
    return GUI::Action::create("&Build", { Mod_Ctrl, Key_B }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/build.png").release_value_but_fixme_should_propagate_errors(), [this](auto&) {
        if (warn_unsaved_changes("There are unsaved changes, do you want to save before building?") == ContinueDecision::No)
            return;

        reveal_action_tab(*m_terminal_wrapper);
        build();
        m_stop_action->set_enabled(true);
    });
}

NonnullRefPtr<GUI::Action> HackStudioWidget::create_run_action()
{
    return GUI::Action::create("&Run", { Mod_Ctrl, Key_R }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/program-run.png").release_value_but_fixme_should_propagate_errors(), [this](auto&) {
        reveal_action_tab(*m_terminal_wrapper);
        run();
        m_stop_action->set_enabled(true);
    });
}

void HackStudioWidget::create_action_tab(GUI::Widget& parent)
{
    m_action_tab_widget = parent.add<GUI::TabWidget>();

    m_action_tab_widget->set_fixed_height(24);
    m_action_tab_widget->on_change = [this](auto&) {
        on_action_tab_change();

        static bool first_time = true;
        if (!first_time)
            m_action_tab_widget->set_fixed_height(200);
        first_time = false;
    };

    m_find_in_files_widget = m_action_tab_widget->add_tab<FindInFilesWidget>("Find in files");
    m_todo_entries_widget = m_action_tab_widget->add_tab<ToDoEntriesWidget>("TODO");
    m_terminal_wrapper = m_action_tab_widget->add_tab<TerminalWrapper>("Console", false);
    m_debug_info_widget = m_action_tab_widget->add_tab<DebugInfoWidget>("Debug");

    m_debug_info_widget->on_backtrace_frame_selection = [this](Debug::DebugInfo::SourcePosition const& source_position) {
        open_file(get_absolute_path(source_position.file_path), source_position.line_number - 1);
    };

    m_disassembly_widget = m_action_tab_widget->add_tab<DisassemblyWidget>("Disassembly");
    m_git_widget = m_action_tab_widget->add_tab<GitWidget>("Git", m_project->root_path());
    m_git_widget->set_view_diff_callback([this](const auto& original_content, const auto& diff) {
        m_diff_viewer->set_content(original_content, diff);
        set_edit_mode(EditMode::Diff);
    });
    m_gml_preview_widget = m_action_tab_widget->add_tab<GMLPreviewWidget>("GML Preview", "");

    ToDoEntries::the().on_update = [this]() {
        m_todo_entries_widget->refresh();
    };
}

void HackStudioWidget::create_project_tab(GUI::Widget& parent)
{
    m_project_tab = parent.add<GUI::TabWidget>();
    m_project_tab->set_tab_position(GUI::TabWidget::TabPosition::Bottom);

    auto& tree_view_container = m_project_tab->add_tab<GUI::Widget>("Files");
    tree_view_container.set_layout<GUI::VerticalBoxLayout>();
    tree_view_container.layout()->set_margins(2);

    m_project_tree_view = tree_view_container.add<GUI::TreeView>();
    configure_project_tree_view();

    auto& class_view_container = m_project_tab->add_tab<GUI::Widget>("Classes");
    class_view_container.set_layout<GUI::VerticalBoxLayout>();
    class_view_container.layout()->set_margins(2);

    m_class_view = class_view_container.add<ClassViewWidget>();

    ProjectDeclarations::the().on_update = [this]() {
        m_class_view->refresh();
    };
}

void HackStudioWidget::create_file_menu(GUI::Window& window)
{
    auto& file_menu = window.add_menu("&File");
    file_menu.add_action(*m_new_project_action);
    file_menu.add_action(*m_open_action);
    file_menu.add_action(*m_save_action);
    file_menu.add_action(*m_save_as_action);
    file_menu.add_separator();
    file_menu.add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the()->quit();
    }));
}

void HackStudioWidget::create_project_menu(GUI::Window& window)
{
    auto& project_menu = window.add_menu("&Project");
    auto& new_submenu = project_menu.add_submenu("New");
    for (auto& new_file_action : m_new_file_actions) {
        new_submenu.add_action(new_file_action);
    }
    new_submenu.add_action(*m_new_plain_file_action);
    new_submenu.add_separator();
    new_submenu.add_action(*m_new_directory_action);
}

void HackStudioWidget::create_edit_menu(GUI::Window& window)
{
    auto& edit_menu = window.add_menu("&Edit");
    edit_menu.add_action(GUI::Action::create("&Find in Files...", { Mod_Ctrl | Mod_Shift, Key_F }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/find.png").release_value_but_fixme_should_propagate_errors(), [this](auto&) {
        reveal_action_tab(*m_find_in_files_widget);
        m_find_in_files_widget->focus_textbox_and_select_all();
    }));

    edit_menu.add_separator();

    auto vim_emulation_setting_action = GUI::Action::create_checkable("&Vim Emulation", { Mod_Ctrl | Mod_Shift | Mod_Alt, Key_V }, [this](auto& action) {
        if (action.is_checked()) {
            for (auto& editor_wrapper : m_all_editor_wrappers)
                editor_wrapper.editor().set_editing_engine(make<GUI::VimEditingEngine>());
        } else {
            for (auto& editor_wrapper : m_all_editor_wrappers)
                editor_wrapper.editor().set_editing_engine(make<GUI::RegularEditingEngine>());
        }
    });
    vim_emulation_setting_action->set_checked(false);
    edit_menu.add_action(vim_emulation_setting_action);
}

void HackStudioWidget::create_build_menu(GUI::Window& window)
{
    auto& build_menu = window.add_menu("&Build");
    build_menu.add_action(*m_build_action);
    build_menu.add_separator();
    build_menu.add_action(*m_run_action);
    build_menu.add_action(*m_stop_action);
    build_menu.add_separator();
    build_menu.add_action(*m_debug_action);
}

void HackStudioWidget::create_view_menu(GUI::Window& window)
{
    auto hide_action_tabs_action = GUI::Action::create("&Hide Action Tabs", { Mod_Ctrl | Mod_Shift, Key_X }, [this](auto&) {
        hide_action_tabs();
    });
    auto open_locator_action = GUI::Action::create("Open &Locator", { Mod_Ctrl, Key_K }, [this](auto&) {
        m_locator->open();
    });
    auto show_dotfiles_action = GUI::Action::create_checkable("S&how Dotfiles", { Mod_Ctrl, Key_H }, [&](auto& checked) {
        project().model().set_should_show_dotfiles(checked.is_checked());
    });

    auto& view_menu = window.add_menu("&View");
    view_menu.add_action(hide_action_tabs_action);
    view_menu.add_action(open_locator_action);
    view_menu.add_action(show_dotfiles_action);
    view_menu.add_separator();

    m_wrapping_mode_actions.set_exclusive(true);
    auto& wrapping_mode_menu = view_menu.add_submenu("&Wrapping Mode");
    m_no_wrapping_action = GUI::Action::create_checkable("&No Wrapping", [&](auto&) {
        for (auto& wrapper : m_all_editor_wrappers)
            wrapper.editor().set_wrapping_mode(GUI::TextEditor::WrappingMode::NoWrap);
    });
    m_wrap_anywhere_action = GUI::Action::create_checkable("Wrap &Anywhere", [&](auto&) {
        for (auto& wrapper : m_all_editor_wrappers)
            wrapper.editor().set_wrapping_mode(GUI::TextEditor::WrappingMode::WrapAnywhere);
    });
    m_wrap_at_words_action = GUI::Action::create_checkable("Wrap at &Words", [&](auto&) {
        for (auto& wrapper : m_all_editor_wrappers)
            wrapper.editor().set_wrapping_mode(GUI::TextEditor::WrappingMode::WrapAtWords);
    });

    m_wrapping_mode_actions.add_action(*m_no_wrapping_action);
    m_wrapping_mode_actions.add_action(*m_wrap_anywhere_action);
    m_wrapping_mode_actions.add_action(*m_wrap_at_words_action);

    wrapping_mode_menu.add_action(*m_no_wrapping_action);
    wrapping_mode_menu.add_action(*m_wrap_anywhere_action);
    wrapping_mode_menu.add_action(*m_wrap_at_words_action);

    m_no_wrapping_action->set_checked(true);

    m_editor_font_action = GUI::Action::create("Editor &Font...", Gfx::Bitmap::try_load_from_file("/res/icons/16x16/app-font-editor.png").release_value_but_fixme_should_propagate_errors(),
        [&](auto&) {
            auto picker = GUI::FontPicker::construct(&window, m_editor_font, false);
            if (picker->exec() == GUI::Dialog::ExecOK) {
                change_editor_font(picker->font());
            }
        });
    view_menu.add_action(*m_editor_font_action);

    view_menu.add_separator();
    view_menu.add_action(*m_add_editor_action);
    view_menu.add_action(*m_remove_current_editor_action);
    view_menu.add_action(*m_add_terminal_action);
    view_menu.add_action(*m_remove_current_terminal_action);

    view_menu.add_separator();

    create_location_history_actions();
    view_menu.add_action(*m_locations_history_back_action);
    view_menu.add_action(*m_locations_history_forward_action);
}

void HackStudioWidget::create_help_menu(GUI::Window& window)
{
    auto& help_menu = window.add_menu("&Help");
    help_menu.add_action(GUI::CommonActions::make_about_action("Hack Studio", GUI::Icon::default_icon("app-hack-studio"), &window));
}

NonnullRefPtr<GUI::Action> HackStudioWidget::create_stop_action()
{
    auto action = GUI::Action::create("&Stop", Gfx::Bitmap::try_load_from_file("/res/icons/16x16/program-stop.png").release_value_but_fixme_should_propagate_errors(), [this](auto&) {
        if (!Debugger::the().session()) {
            m_terminal_wrapper->kill_running_command();
            return;
        }

        Debugger::the().stop();
    });

    action->set_enabled(false);
    return action;
}

void HackStudioWidget::initialize_menubar(GUI::Window& window)
{
    create_file_menu(window);
    create_project_menu(window);
    create_edit_menu(window);
    create_build_menu(window);
    create_view_menu(window);
    create_help_menu(window);
}

void HackStudioWidget::update_statusbar()
{
    m_statusbar->set_text(0, String::formatted("Ln {}, Col {}", current_editor().cursor().line() + 1, current_editor().cursor().column()));

    StringBuilder builder;
    if (current_editor().has_selection()) {
        String selected_text = current_editor().selected_text();
        auto word_count = current_editor().number_of_selected_words();
        builder.appendff("Selected: {} {} ({} {})", selected_text.length(), selected_text.length() == 1 ? "character" : "characters", word_count, word_count != 1 ? "words" : "word");
    }

    m_statusbar->set_text(1, builder.to_string());
    m_statusbar->set_text(2, current_editor_wrapper().editor().code_document().language_name());
}

void HackStudioWidget::handle_external_file_deletion(const String& filepath)
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
    m_all_editor_wrappers.clear();
    m_open_files.clear();
    m_open_files_vector.clear();
    add_new_editor(*m_editors_splitter);
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

HackStudioWidget::ContinueDecision HackStudioWidget::warn_unsaved_changes(const String& prompt)
{
    if (!any_document_is_dirty())
        return ContinueDecision::Yes;

    auto result = GUI::MessageBox::show(window(), prompt, "Unsaved changes", GUI::MessageBox::Type::Warning, GUI::MessageBox::InputType::YesNoCancel);

    if (result == GUI::MessageBox::ExecCancel)
        return ContinueDecision::No;

    if (result == GUI::MessageBox::ExecYes) {
        for (auto& editor_wrapper : m_all_editor_wrappers) {
            if (editor_wrapper.editor().document().is_modified()) {
                editor_wrapper.save();
            }
        }
    }

    return ContinueDecision::Yes;
}

bool HackStudioWidget::any_document_is_dirty() const
{
    return any_of(m_all_editor_wrappers, [](auto& editor_wrapper) {
        return editor_wrapper.editor().document().is_modified();
    });
}

void HackStudioWidget::update_gml_preview()
{
    auto gml_content = current_editor_wrapper().filename().ends_with(".gml") ? current_editor_wrapper().editor().text() : "";
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

void HackStudioWidget::update_window_title()
{
    window()->set_title(String::formatted("{} - {} - Hack Studio", m_current_editor_wrapper->filename_label().text(), m_project->name()));
}

void HackStudioWidget::on_cursor_change()
{
    update_statusbar();
    if (current_editor_wrapper().filename().is_null())
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

void HackStudioWidget::create_location_history_actions()
{
    m_locations_history_back_action = GUI::Action::create("Go Back", { Mod_Alt | Mod_Shift, Key_Left }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/go-back.png").release_value_but_fixme_should_propagate_errors(), [this](auto&) {
        if (m_locations_history_end_index <= 1)
            return;

        auto location = m_locations_history[m_locations_history_end_index - 2];
        --m_locations_history_end_index;

        m_locations_history_disabled = true;
        open_file(location.filename, location.line, location.column);
        m_locations_history_disabled = false;

        update_history_actions();
    });

    m_locations_history_forward_action = GUI::Action::create("Go Forward", { Mod_Alt | Mod_Shift, Key_Right }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/go-forward.png").release_value_but_fixme_should_propagate_errors(), [this](auto&) {
        if (m_locations_history_end_index == m_locations_history.size())
            return;

        auto location = m_locations_history[m_locations_history_end_index];
        ++m_locations_history_end_index;

        m_locations_history_disabled = true;
        open_file(location.filename, location.line, location.column);
        m_locations_history_disabled = false;

        update_history_actions();
    });
    m_locations_history_forward_action->set_enabled(false);
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

RefPtr<Gfx::Font> HackStudioWidget::read_editor_font_from_config()
{
    auto font_family = Config::read_string("HackStudio", "EditorFont", "Family", "Csilla");
    auto font_variant = Config::read_string("HackStudio", "EditorFont", "Variant", "Regular");
    auto font_size = Config::read_i32("HackStudio", "EditorFont", "Size", 10);

    auto font = Gfx::FontDatabase::the().get(font_family, font_variant, font_size);
    if (font.is_null())
        return Gfx::FontDatabase::the().default_fixed_width_font();

    return font;
}

void HackStudioWidget::change_editor_font(RefPtr<Gfx::Font> font)
{
    m_editor_font = move(font);
    for (auto& editor_wrapper : m_all_editor_wrappers) {
        editor_wrapper.editor().set_font(*m_editor_font);
    }

    Config::write_string("HackStudio", "EditorFont", "Family", m_editor_font->family());
    Config::write_string("HackStudio", "EditorFont", "Variant", m_editor_font->variant());
    Config::write_i32("HackStudio", "EditorFont", "Size", m_editor_font->presentation_size());
}

void HackStudioWidget::open_coredump(String const& coredump_path)
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

void HackStudioWidget::for_each_open_file(Function<void(ProjectFile const&)> func)
{
    for (auto& open_file : m_open_files) {
        func(*open_file.value);
    }
}

}
