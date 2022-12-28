/*
 * Copyright (c) 2022, Dylan Katz <dykatz@uw.edu>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/LexicalPath.h>
#include <AK/Vector.h>
#include <LibGUI/Widget.h>
#include <LibSQL/SQLClient.h>

namespace SQLStudio {

class ScriptEditor;

class MainWidget : public GUI::Widget {
    C_OBJECT(MainWidget)

public:
    virtual ~MainWidget() = default;

    void initialize_menu(GUI::Window*);
    void open_new_script();
    void open_script_from_file(LexicalPath const&);
    void open_database_from_file(LexicalPath const&);

    bool request_close();

private:
    MainWidget();

    void update_title();
    void on_editor_change();
    void update_statusbar(ScriptEditor*);
    void update_editor_actions(ScriptEditor*);

    virtual void drag_enter_event(GUI::DragEvent&) override;
    virtual void drop_event(GUI::DropEvent&) override;

    RefPtr<GUI::Action> m_new_action;
    RefPtr<GUI::Action> m_open_action;
    RefPtr<GUI::Action> m_save_action;
    RefPtr<GUI::Action> m_save_as_action;
    RefPtr<GUI::Action> m_save_all_action;
    RefPtr<GUI::Action> m_copy_action;
    RefPtr<GUI::Action> m_cut_action;
    RefPtr<GUI::Action> m_paste_action;
    RefPtr<GUI::Action> m_undo_action;
    RefPtr<GUI::Action> m_redo_action;
    RefPtr<GUI::Action> m_run_script_action;

    int m_new_script_counter { 1 };
    RefPtr<GUI::TabWidget> m_tab_widget;
    RefPtr<GUI::Statusbar> m_statusbar;
    RefPtr<GUI::TabWidget> m_action_tab_widget;
    RefPtr<GUI::Widget> m_query_results_widget;
    RefPtr<GUI::TableView> m_query_results_table_view;

    RefPtr<SQL::SQLClient> m_sql_client;
    Vector<Vector<DeprecatedString>> m_results;

    void read_next_sql_statement_of_editor();
    Optional<DeprecatedString> read_next_line_of_editor();
    size_t m_current_line_for_parsing { 0 };
    int m_editor_line_level { 0 };
    SQL::ConnectionID m_connection_id { 0 };
};

}

using SQLStudio::MainWidget;
