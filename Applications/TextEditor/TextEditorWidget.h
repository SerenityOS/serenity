/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/FileSystemPath.h>
#include <AK/Function.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GTextEditor.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GWindow.h>

class GButton;
class GTextBox;
class GTextEditor;
class GStatusBar;

class TextEditorWidget final : public GWidget {
    C_OBJECT(TextEditorWidget)
public:
    virtual ~TextEditorWidget() override;
    void open_sesame(const String& path);
    bool request_close();

    GTextEditor& editor() { return *m_editor; }

private:
    TextEditorWidget();
    void set_path(const FileSystemPath& file);
    void update_title();

    virtual void drop_event(GDropEvent&) override;

    RefPtr<GTextEditor> m_editor;
    String m_path;
    String m_name;
    String m_extension;
    RefPtr<GAction> m_new_action;
    RefPtr<GAction> m_open_action;
    RefPtr<GAction> m_save_action;
    RefPtr<GAction> m_save_as_action;
    RefPtr<GAction> m_find_replace_action;
    RefPtr<GAction> m_line_wrapping_setting_action;
    RefPtr<GAction> m_find_next_action;
    RefPtr<GAction> m_find_previous_action;
    RefPtr<GAction> m_replace_next_action;
    RefPtr<GAction> m_replace_previous_action;
    RefPtr<GAction> m_replace_all_action;

    RefPtr<GStatusBar> m_statusbar;

    RefPtr<GTextBox> m_find_textbox;
    RefPtr<GTextBox> m_replace_textbox;
    GButton* m_find_previous_button { nullptr };
    GButton* m_find_next_button { nullptr };
    GButton* m_replace_previous_button { nullptr };
    GButton* m_replace_next_button { nullptr };
    GButton* m_replace_all_button { nullptr };
    RefPtr<GWidget> m_find_replace_widget;
    RefPtr<GWidget> m_find_widget;
    RefPtr<GWidget> m_replace_widget;

    bool m_document_dirty { false };
    bool m_document_opening { false };
};
