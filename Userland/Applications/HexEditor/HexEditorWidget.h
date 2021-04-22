/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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
    virtual ~HexEditorWidget() override;
    void open_file(const String& path);
    void initialize_menubar(GUI::Menubar&);
    bool request_close();

private:
    HexEditorWidget();
    void set_path(const LexicalPath& file);
    void update_title();

    RefPtr<HexEditor> m_editor;
    String m_path;
    String m_name;
    String m_extension;

    String m_search_text;
    ByteBuffer m_search_buffer;
    int last_found_index() const { return m_last_found_index == -1 ? 0 : m_last_found_index; }
    int m_last_found_index { -1 };

    RefPtr<GUI::Action> m_new_action;
    RefPtr<GUI::Action> m_open_action;
    RefPtr<GUI::Action> m_save_action;
    RefPtr<GUI::Action> m_save_as_action;
    RefPtr<GUI::Action> m_goto_decimal_offset_action;
    RefPtr<GUI::Action> m_goto_hex_offset_action;

    GUI::ActionGroup m_bytes_per_row_actions;

    RefPtr<GUI::Statusbar> m_statusbar;

    bool m_document_dirty { false };
};
