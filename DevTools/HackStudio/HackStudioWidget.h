/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 * Copyright (c) 2020, the SerenityOS developers
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

#pragma once

#include "Debugger/DebugInfoWidget.h"
#include "Debugger/DisassemblyWidget.h"
#include "EditorWrapper.h"
#include "FindInFilesWidget.h"
#include "FormEditorWidget.h"
#include "Git/DiffViewer.h"
#include "Git/GitWidget.h"
#include "Locator.h"
#include "Project.h"
#include "TerminalWrapper.h"
#include <LibGUI/Splitter.h>
#include <LibGUI/Widget.h>
#include <LibThread/Thread.h>

namespace HackStudio {

class HackStudioWidget : public GUI::Widget {
    C_OBJECT(HackStudioWidget)

public:
    virtual ~HackStudioWidget() override;
    void open_file(const String& filename);

    Vector<String> selected_file_names() const;

    void update_actions();
    Project& project();
    GUI::TextEditor& current_editor();
    EditorWrapper& current_editor_wrapper();
    void set_current_editor_wrapper(RefPtr<EditorWrapper>);

    String currently_open_file() const { return m_currently_open_file; }
    void initialize_menubar(GUI::MenuBar&);

private:
    static String get_full_path_of_serenity_source(const String& file);

    HackStudioWidget(const String& path_to_project);
    void open_project(String filename);

    enum class EditMode {
        Text,
        Form,
        Diff,
    };

    void set_edit_mode(EditMode);

    NonnullRefPtr<GUI::Menu> create_project_tree_view_context_menu();
    NonnullRefPtr<GUI::Action> create_new_action();
    NonnullRefPtr<GUI::Action> create_open_selected_action();
    NonnullRefPtr<GUI::Action> create_add_existing_file_action();
    NonnullRefPtr<GUI::Action> create_delete_action();
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
    NonnullRefPtr<EditorWrapper> get_editor_of_file(const String& file_name);
    String get_project_executable_path() const;

    void on_action_tab_change();
    void reveal_action_tab(GUI::Widget&);
    void initialize_debugger();

    void create_project_tree_view(GUI::Widget& parent);
    void create_form_editor(GUI::Widget& parent);
    void create_toolbar(GUI::Widget& parent);
    void create_action_tab(GUI::Widget& parent);
    void create_app_menubar(GUI::MenuBar&);
    void create_project_menubar(GUI::MenuBar&);
    void create_edit_menubar(GUI::MenuBar&);
    void create_build_menubar(GUI::MenuBar&);
    void create_view_menubar(GUI::MenuBar&);
    void create_help_menubar(GUI::MenuBar&);

    void run(TerminalWrapper& wrapper);
    void build(TerminalWrapper& wrapper);

    void hide_action_tabs();

    NonnullRefPtrVector<EditorWrapper> m_all_editor_wrappers;
    RefPtr<EditorWrapper> m_current_editor_wrapper;

    String m_currently_open_file;
    OwnPtr<Project> m_project;

    RefPtr<GUI::TreeView> m_project_tree_view;
    RefPtr<GUI::VerticalSplitter> m_right_hand_splitter;
    RefPtr<GUI::StackWidget> m_right_hand_stack;
    RefPtr<GUI::Splitter> m_editors_splitter;
    RefPtr<GUI::Widget> m_form_inner_container;
    RefPtr<FormEditorWidget> m_form_editor_widget;
    RefPtr<GUI::TreeView> m_form_widget_tree_view;
    RefPtr<DiffViewer> m_diff_viewer;
    RefPtr<GitWidget> m_git_widget;
    RefPtr<GUI::Menu> m_project_tree_view_context_menu;
    RefPtr<GUI::TabWidget> m_action_tab_widget;
    RefPtr<TerminalWrapper> m_terminal_wrapper;
    RefPtr<Locator> m_locator;
    RefPtr<FindInFilesWidget> m_find_in_files_widget;
    RefPtr<DebugInfoWidget> m_debug_info_widget;
    RefPtr<DisassemblyWidget> m_disassembly_widget;
    RefPtr<LibThread::Thread> m_debugger_thread;
    RefPtr<EditorWrapper> m_current_editor_in_execution;

    RefPtr<GUI::Action> m_new_action;
    RefPtr<GUI::Action> m_open_selected_action;
    RefPtr<GUI::Action> m_add_existing_file_action;
    RefPtr<GUI::Action> m_delete_action;
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
};
}
