/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2022, Itamar S. <itamar8910@gmail.com>
 * Copyright (c) 2020-2021, the SerenityOS developers.
 * Copyright (c) 2024, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "ClassViewWidget.h"
#include "Debugger/DebugInfoWidget.h"
#include "Debugger/DisassemblyWidget.h"
#include "EditorWrapper.h"
#include "FindInFilesWidget.h"
#include "GMLPreviewWidget.h"
#include "Git/DiffViewer.h"
#include "Git/GitWidget.h"
#include "Locator.h"
#include "Project.h"
#include "ProjectBuilder.h"
#include "ProjectFile.h"
#include "TerminalWrapper.h"
#include "ToDoEntriesWidget.h"
#include <LibCoredump/Inspector.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Button.h>
#include <LibGUI/Scrollbar.h>
#include <LibGUI/Splitter.h>
#include <LibGUI/Widget.h>
#include <LibGfx/Font/Font.h>
#include <LibThreading/Thread.h>

namespace HackStudio {

class HackStudioWidget : public GUI::Widget {
    C_OBJECT_ABSTRACT(HackStudioWidget)

public:
    static ErrorOr<NonnullRefPtr<HackStudioWidget>> create(ByteString path_to_project);
    virtual ~HackStudioWidget() override;

    bool open_file(ByteString const& filename, size_t line = 0, size_t column = 0);
    void close_file_in_all_editors(ByteString const& filename);

    void update_actions();
    Project& project();
    GUI::TextEditor& current_editor();
    GUI::TextEditor const& current_editor() const;
    EditorWrapper& current_editor_wrapper();
    EditorWrapper const& current_editor_wrapper() const;
    void set_current_editor_wrapper(RefPtr<EditorWrapper>);
    void set_current_editor_tab_widget(RefPtr<GUI::TabWidget>);

    GUI::TabWidget& current_editor_tab_widget();
    GUI::TabWidget const& current_editor_tab_widget() const;

    ByteString const& active_file() const { return m_current_editor_wrapper->filename(); }
    ErrorOr<void> initialize_menubar(GUI::Window&);

    Locator& locator()
    {
        VERIFY(m_locator);
        return *m_locator;
    }

    enum class ContinueDecision {
        No,
        Yes
    };
    ContinueDecision warn_unsaved_changes(ByteString const& prompt);

    enum class Mode {
        Code,
        Coredump
    };

    void open_coredump(StringView coredump_path);
    void debug_process(pid_t pid);
    void for_each_open_file(Function<void(ProjectFile const&)>);
    bool semantic_syntax_highlighting_is_enabled() const;

    static Vector<ByteString> read_recent_projects();

    void update_current_editor_title();
    void update_window_title();

private:
    static ByteString get_full_path_of_serenity_source(ByteString const& file);
    ByteString get_absolute_path(ByteString const&) const;
    Vector<ByteString> selected_file_paths() const;

    void open_project(ByteString const& root_path);

    enum class EditMode {
        Text,
        Diff,
    };

    void set_edit_mode(EditMode);

    ErrorOr<NonnullRefPtr<GUI::Menu>> create_project_tree_view_context_menu();
    ErrorOr<NonnullRefPtr<GUI::Action>> create_new_file_action(ByteString const& label, ByteString const& icon, ByteString const& extension);
    ErrorOr<NonnullRefPtr<GUI::Action>> create_new_directory_action();
    ErrorOr<NonnullRefPtr<GUI::Action>> create_open_selected_action();
    NonnullRefPtr<GUI::Action> create_delete_action();
    ErrorOr<NonnullRefPtr<GUI::Action>> create_new_project_action();
    NonnullRefPtr<GUI::Action> create_switch_to_next_editor_tab_widget_action();
    NonnullRefPtr<GUI::Action> create_switch_to_next_editor_action();
    NonnullRefPtr<GUI::Action> create_switch_to_previous_editor_action();
    NonnullRefPtr<GUI::Action> create_remove_current_editor_tab_widget_action();
    ErrorOr<NonnullRefPtr<GUI::Action>> create_remove_current_editor_action();
    ErrorOr<NonnullRefPtr<GUI::Action>> create_open_action();
    ErrorOr<NonnullRefPtr<GUI::Action>> create_toggle_open_file_in_single_click_action();
    NonnullRefPtr<GUI::Action> create_save_action();
    NonnullRefPtr<GUI::Action> create_save_as_action();
    NonnullRefPtr<GUI::Action> create_show_in_file_manager_action();
    NonnullRefPtr<GUI::Action> create_copy_relative_path_action();
    NonnullRefPtr<GUI::Action> create_copy_full_path_action();
    NonnullRefPtr<GUI::Action> create_add_editor_tab_widget_action();
    ErrorOr<NonnullRefPtr<GUI::Action>> create_add_editor_action();
    ErrorOr<NonnullRefPtr<GUI::Action>> create_add_terminal_action();
    ErrorOr<NonnullRefPtr<GUI::Action>> create_remove_current_terminal_action();
    ErrorOr<NonnullRefPtr<GUI::Action>> create_debug_action();
    ErrorOr<NonnullRefPtr<GUI::Action>> create_build_action();
    ErrorOr<NonnullRefPtr<GUI::Action>> create_run_action();
    ErrorOr<NonnullRefPtr<GUI::Action>> create_stop_action();
    ErrorOr<NonnullRefPtr<GUI::Action>> create_toggle_syntax_highlighting_mode_action();
    ErrorOr<NonnullRefPtr<GUI::Action>> create_open_project_configuration_action();
    ErrorOr<void> create_location_history_actions();

    void add_new_editor_tab_widget(GUI::Widget& parent);
    void add_new_editor(GUI::TabWidget& parent);
    RefPtr<EditorWrapper> get_editor_of_file(ByteString const& filename);
    ByteString get_project_executable_path() const;

    void on_action_tab_change();
    void reveal_action_tab(GUI::Widget&);
    void initialize_debugger();
    void update_statusbar();

    void handle_external_file_deletion(ByteString const& filepath);
    void stop_debugger_if_running();
    void close_current_project();

    void create_open_files_view(GUI::Widget& parent);
    void create_toolbar(GUI::Widget& parent);
    ErrorOr<void> create_action_tab(GUI::Widget& parent);
    ErrorOr<void> create_file_menu(GUI::Window&);
    ErrorOr<void> create_edit_menu(GUI::Window&);
    void create_build_menu(GUI::Window&);
    ErrorOr<void> create_view_menu(GUI::Window&);
    void create_help_menu(GUI::Window&);
    void create_project_tab(GUI::Widget& parent);
    void configure_project_tree_view();

    void run();
    void build();

    void hide_action_tabs();
    bool any_document_is_dirty() const;

    void update_gml_preview();
    void update_tree_view();
    void update_toolbar_actions();
    void on_cursor_change();
    void file_renamed(ByteString const& old_name, ByteString const& new_name);
    bool save_file_changes();

    struct ProjectLocation {
        ByteString filename;
        size_t line { 0 };
        size_t column { 0 };
    };

    ProjectLocation current_project_location() const;
    void update_history_actions();

    Vector<NonnullRefPtr<EditorWrapper>> m_all_editor_wrappers;
    RefPtr<EditorWrapper> m_current_editor_wrapper;
    Vector<NonnullRefPtr<GUI::TabWidget>> m_all_editor_tab_widgets;
    RefPtr<GUI::TabWidget> m_current_editor_tab_widget;

    HashMap<ByteString, NonnullRefPtr<ProjectFile>> m_open_files;
    RefPtr<Core::FileWatcher> m_file_watcher;
    Vector<ByteString> m_open_files_vector; // NOTE: This contains the keys from m_open_files and m_file_watchers

    OwnPtr<Project> m_project;

    Vector<ProjectLocation> m_locations_history;
    // This index is the boundary between the "Go Back" and "Go Forward" locations.
    // It always points at one past the current location in the list.
    size_t m_locations_history_end_index { 0 };
    bool m_locations_history_disabled { false };

    bool m_auto_save_before_build_or_run { false };

    RefPtr<GUI::TreeView> m_project_tree_view;
    RefPtr<GUI::ListView> m_open_files_view;
    RefPtr<GUI::VerticalSplitter> m_right_hand_splitter;
    RefPtr<GUI::StackWidget> m_right_hand_stack;
    RefPtr<GUI::Splitter> m_editors_splitter;
    RefPtr<DiffViewer> m_diff_viewer;
    RefPtr<GitWidget> m_git_widget;
    RefPtr<GMLPreviewWidget> m_gml_preview_widget;
    RefPtr<ClassViewWidget> m_class_view;
    RefPtr<GUI::Menu> m_project_tree_view_context_menu;
    RefPtr<GUI::Statusbar> m_statusbar;
    RefPtr<GUI::TabWidget> m_action_tab_widget;
    RefPtr<GUI::TabWidget> m_project_tab;
    RefPtr<TerminalWrapper> m_terminal_wrapper;
    RefPtr<Locator> m_locator;
    RefPtr<FindInFilesWidget> m_find_in_files_widget;
    RefPtr<ToDoEntriesWidget> m_todo_entries_widget;
    RefPtr<DebugInfoWidget> m_debug_info_widget;
    RefPtr<DisassemblyWidget> m_disassembly_widget;
    RefPtr<Threading::Thread> m_debugger_thread;
    RefPtr<EditorWrapper> m_current_editor_in_execution;
    RefPtr<GUI::Menu> m_recent_projects_submenu { nullptr };

    Vector<NonnullRefPtr<GUI::Action>> m_new_file_actions;
    RefPtr<GUI::Action> m_new_plain_file_action;

    RefPtr<GUI::Action> m_new_directory_action;
    RefPtr<GUI::Action> m_open_selected_action;
    RefPtr<GUI::Action> m_show_in_file_manager_action;
    RefPtr<GUI::Action> m_copy_relative_path_action;
    RefPtr<GUI::Action> m_copy_full_path_action;
    RefPtr<GUI::Action> m_delete_action;
    RefPtr<GUI::Action> m_tree_view_rename_action;
    RefPtr<GUI::Action> m_new_project_action;
    RefPtr<GUI::Action> m_switch_to_next_editor_tab_widget;
    RefPtr<GUI::Action> m_switch_to_next_editor;
    RefPtr<GUI::Action> m_switch_to_previous_editor;
    RefPtr<GUI::Action> m_remove_current_editor_tab_widget_action;
    RefPtr<GUI::Action> m_remove_current_editor_action;
    RefPtr<GUI::Action> m_open_action;
    RefPtr<GUI::Action> m_save_action;
    RefPtr<GUI::Action> m_save_as_action;
    RefPtr<GUI::Action> m_add_editor_action;
    RefPtr<GUI::Action> m_add_editor_tab_widget_action;
    RefPtr<GUI::Action> m_add_terminal_action;
    RefPtr<GUI::Action> m_remove_current_terminal_action;
    RefPtr<GUI::Action> m_stop_action;
    RefPtr<GUI::Action> m_debug_action;
    RefPtr<GUI::Action> m_build_action;
    RefPtr<GUI::Action> m_run_action;
    RefPtr<GUI::Action> m_locations_history_back_action;
    RefPtr<GUI::Action> m_locations_history_forward_action;
    RefPtr<GUI::Action> m_toggle_semantic_highlighting_action;
    RefPtr<GUI::Action> m_toggle_view_file_in_single_click_action;
    RefPtr<GUI::Action> m_open_project_configuration_action;

    RefPtr<Gfx::Font const> read_editor_font_from_config();
    void change_editor_font(RefPtr<Gfx::Font const>);
    RefPtr<Gfx::Font const> m_editor_font;
    RefPtr<GUI::Action> m_editor_font_action;

    GUI::TextEditor::WrappingMode m_wrapping_mode { GUI::TextEditor::NoWrap };
    GUI::ActionGroup m_wrapping_mode_actions;
    RefPtr<GUI::Action> m_no_wrapping_action;
    RefPtr<GUI::Action> m_wrap_anywhere_action;
    RefPtr<GUI::Action> m_wrap_at_words_action;

    RefPtr<GUI::Button> m_cut_button;
    RefPtr<GUI::Button> m_paste_button;
    RefPtr<GUI::Button> m_copy_button;

    Mode m_mode { Mode::Code };
    OwnPtr<Coredump::Inspector> m_coredump_inspector;
    OwnPtr<ProjectBuilder> m_project_builder;
};
}
