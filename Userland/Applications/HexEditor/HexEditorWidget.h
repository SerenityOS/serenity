/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "HexEditor.h"
#include <AK/Function.h>
#include <AK/LexicalPath.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Application.h>
#include <LibGUI/TextEditor.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>

class HexEditor;

class HexEditorWidget final : public GUI::Widget {
    C_OBJECT(HexEditorWidget)
public:
    virtual ~HexEditorWidget() override = default;
    void open_file(NonnullRefPtr<Core::File>);
    void initialize_menubar(GUI::Window&);
    bool request_close();

private:
    HexEditorWidget();
    void set_path(StringView);
    void update_title();
    void set_search_results_visible(bool visible);

    virtual void drop_event(GUI::DropEvent&) override;

    RefPtr<HexEditor> m_editor;
    String m_path;
    String m_name;
    String m_extension;

    int m_goto_history { 0 };
    String m_search_text;
    ByteBuffer m_search_buffer;
    int last_found_index() const { return m_last_found_index == -1 ? 0 : m_last_found_index; }
    int m_last_found_index { -1 };

    RefPtr<GUI::Action> m_new_action;
    RefPtr<GUI::Action> m_open_action;
    RefPtr<GUI::Action> m_save_action;
    RefPtr<GUI::Action> m_save_as_action;
    RefPtr<GUI::Action> m_find_action;
    RefPtr<GUI::Action> m_goto_offset_action;
    RefPtr<GUI::Action> m_layout_toolbar_action;
    RefPtr<GUI::Action> m_layout_search_results_action;

    GUI::ActionGroup m_bytes_per_row_actions;

    RefPtr<GUI::Statusbar> m_statusbar;
    RefPtr<GUI::Toolbar> m_toolbar;
    RefPtr<GUI::ToolbarContainer> m_toolbar_container;
    RefPtr<GUI::TableView> m_search_results;
    RefPtr<GUI::Widget> m_search_results_container;

    bool m_document_dirty { false };
};
