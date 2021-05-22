/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 * Copyright (c) 2020-2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "ClassViewWidget.h"
#include "Debugger/DebugInfoWidget.h"
#include "Debugger/DisassemblyWidget.h"
#include "EditorWrapper.h"
#include "FindInFilesWidget.h"
#include "Git/DiffViewer.h"
#include "Git/GitWidget.h"
#include "Locator.h"
#include "Project.h"
#include "ProjectFile.h"
#include "TerminalWrapper.h"
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Scrollbar.h>
#include <LibGUI/Splitter.h>
#include <LibGUI/Widget.h>
#include <LibThreading/Thread.h>

namespace HackStudio {

class HackStudioWidget : public GUI::Widget {
    C_OBJECT(HackStudioWidget)

public:
    virtual ~HackStudioWidget() override;
    bool open_file(const String& filename);

    void update_actions();
    Project& project();
    GUI::TextEditor& current_editor();
    EditorWrapper& current_editor_wrapper();
    void set_current_editor_wrapper(RefPtr<EditorWrapper>);

    const String& active_file() const { return m_current_editor_wrapper->filename(); }
    void initialize_menubar(GUI::Menubar&);

    Locator& locator()
    {
        VERIFY(m_locator);
        return *m_locator;
    }

    enum class ContinueDecision {
        No,
        Yes
    };
    ContinueDecision warn_unsaved_changes(const String& prompt);

private:
    static String get_full_path_of_serenity_source(const String& file);
    Vector<String> selected_file_paths() const;

    HackStudioWidget(const String& path_to_project);
    void open_project(const String& root_path);

    enum class EditMode {
        Text,
        Form,
        Diff,
    };

    void set_edit_mode(EditMode);

    NonnullRefPtr<GUI::Menu> create_project_tree_view_context_menu();
    NonnullRefPtr<GUI::Action> create_new_file_action();
    NonnullRefPtr<GUI::Action> create_new_directory_action();
    NonnullRefPtr<GUI::Action> create_open_selected_action();
    NonnullRefPtr<GUI::Action> create_delete_action();
    NonnullRefPtr<GUI::Action> create_new_project_action();
    NonnullRefPtr<GUI::Action> create_switch_to_next_editor_action();
    NonnullRefPtr<GUI::Action> create_switch_to_previous_editor_action();
    NonnullRefPtr<GUI::Action> create_remove_current_editor_action();
    NonnullRefPtr<GUI::Action> create_open_action();
    NonnullRefPtr<GUI::Action> create_save_action();
    NonnullRefPtr<GUI::Action> create_add_editor_action();
    NonnullRefPtr<GUI::Action> create_add_terminal_action();
    NonnullRefPtr<GUI::Action> create_remove_current_terminal_action();
    NonnullRefPtr<GUI::Action> create_debug_action();
    NonnullRefPtr<GUI::Action> create_build_action();
    NonnullRefPtr<GUI::Action> create_run_action();
    NonnullRefPtr<GUI::Action> create_stop_action();

    void add_new_editor(GUI::Widget& parent);
    RefPtr<EditorWrapper> get_editor_of_file(const String& filename);
    String get_project_executable_path() const;

    void on_action_tab_change();
    void reveal_action_tab(GUI::Widget&);
    void initialize_debugger();

    void handle_external_file_deletion(const String& filepath);

    void create_open_files_view(GUI::Widget& parent);
    void create_toolbar(GUI::Widget& parent);
    void create_action_tab(GUI::Widget& parent);
    void create_file_menubar(GUI::Menubar&);
    void create_project_menubar(GUI::Menubar&);
    void create_edit_menubar(GUI::Menubar&);
    void create_build_menubar(GUI::Menubar&);
    void create_view_menubar(GUI::Menubar&);
    void create_help_menubar(GUI::Menubar&);
    void create_project_tab(GUI::Widget& parent);
    void configure_project_tree_view();

    void run(TerminalWrapper& wrapper);
    void build(TerminalWrapper& wrapper);

    void hide_action_tabs();
    bool any_document_is_dirty() const;

    NonnullRefPtrVector<EditorWrapper> m_all_editor_wrappers;
    RefPtr<EditorWrapper> m_current_editor_wrapper;

    HashMap<String, NonnullRefPtr<ProjectFile>> m_open_files;
    RefPtr<Core::FileWatcher> m_file_watcher;
    Vector<String> m_open_files_vector; // NOTE: This contains the keys from m_open_files and m_file_watchers

    OwnPtr<Project> m_project;

    RefPtr<GUI::TreeView> m_project_tree_view;
    RefPtr<GUI::ListView> m_open_files_view;
    RefPtr<GUI::VerticalSplitter> m_right_hand_splitter;
    RefPtr<GUI::StackWidget> m_right_hand_stack;
    RefPtr<GUI::Splitter> m_editors_splitter;
    RefPtr<GUI::Widget> m_form_inner_container;
    RefPtr<GUI::TreeView> m_form_widget_tree_view;
    RefPtr<DiffViewer> m_diff_viewer;
    RefPtr<GitWidget> m_git_widget;
    RefPtr<ClassViewWidget> m_class_view;
    RefPtr<GUI::Menu> m_project_tree_view_context_menu;
    RefPtr<GUI::TabWidget> m_action_tab_widget;
    RefPtr<GUI::TabWidget> m_project_tab;
    RefPtr<TerminalWrapper> m_terminal_wrapper;
    RefPtr<Locator> m_locator;
    RefPtr<FindInFilesWidget> m_find_in_files_widget;
    RefPtr<DebugInfoWidget> m_debug_info_widget;
    RefPtr<DisassemblyWidget> m_disassembly_widget;
    RefPtr<Threading::Thread> m_debugger_thread;
    RefPtr<EditorWrapper> m_current_editor_in_execution;

    RefPtr<GUI::Action> m_new_file_action;
    RefPtr<GUI::Action> m_new_directory_action;
    RefPtr<GUI::Action> m_open_selected_action;
    RefPtr<GUI::Action> m_delete_action;
    RefPtr<GUI::Action> m_new_project_action;
    RefPtr<GUI::Action> m_switch_to_next_editor;
    RefPtr<GUI::Action> m_switch_to_previous_editor;
    RefPtr<GUI::Action> m_remove_current_editor_action;
    RefPtr<GUI::Action> m_open_action;
    RefPtr<GUI::Action> m_save_action;
    RefPtr<GUI::Action> m_add_editor_action;
    RefPtr<GUI::Action> m_add_terminal_action;
    RefPtr<GUI::Action> m_remove_current_terminal_action;
    RefPtr<GUI::Action> m_stop_action;
    RefPtr<GUI::Action> m_debug_action;
    RefPtr<GUI::Action> m_build_action;
    RefPtr<GUI::Action> m_run_action;

    GUI::ActionGroup m_wrapping_mode_actions;
    RefPtr<GUI::Action> m_no_wrapping_action;
    RefPtr<GUI::Action> m_wrap_anywhere_action;
    RefPtr<GUI::Action> m_wrap_at_words_action;
};
}
