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

#include <AK/Function.h>
#include <AK/LexicalPath.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Application.h>
#include <LibGUI/TextEditor.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibWeb/Forward.h>

class TextEditorWidget final : public GUI::Widget {
    C_OBJECT(TextEditorWidget)
public:
    virtual ~TextEditorWidget() override;
    void open_sesame(const String& path);
    bool request_close();

    GUI::TextEditor& editor() { return *m_editor; }

    enum class PreviewMode {
        None,
        Markdown,
        HTML,
    };

    void set_preview_mode(PreviewMode);
    void set_auto_detect_preview_mode(bool value) { m_auto_detect_preview_mode = value; }

private:
    TextEditorWidget();
    void set_path(const LexicalPath& file);
    void update_title();
    void update_preview();
    void update_markdown_preview();
    void update_html_preview();
    void update_statusbar_cursor_position();

    virtual void drop_event(GUI::DropEvent&) override;

    RefPtr<GUI::TextEditor> m_editor;
    String m_path;
    String m_name;
    String m_extension;
    RefPtr<GUI::Action> m_new_action;
    RefPtr<GUI::Action> m_open_action;
    RefPtr<GUI::Action> m_save_action;
    RefPtr<GUI::Action> m_save_as_action;
    RefPtr<GUI::Action> m_find_replace_action;
    RefPtr<GUI::Action> m_line_wrapping_setting_action;

    RefPtr<GUI::Action> m_find_next_action;
    RefPtr<GUI::Action> m_find_previous_action;
    RefPtr<GUI::Action> m_replace_next_action;
    RefPtr<GUI::Action> m_replace_previous_action;
    RefPtr<GUI::Action> m_replace_all_action;

    GUI::ActionGroup m_preview_actions;
    RefPtr<GUI::Action> m_no_preview_action;
    RefPtr<GUI::Action> m_markdown_preview_action;
    RefPtr<GUI::Action> m_html_preview_action;

    RefPtr<GUI::StatusBar> m_statusbar;

    RefPtr<GUI::TextBox> m_find_textbox;
    RefPtr<GUI::TextBox> m_replace_textbox;
    RefPtr<GUI::Button> m_find_previous_button;
    RefPtr<GUI::Button> m_find_next_button;
    RefPtr<GUI::Button> m_replace_previous_button;
    RefPtr<GUI::Button> m_replace_next_button;
    RefPtr<GUI::Button> m_replace_all_button;
    RefPtr<GUI::Widget> m_find_replace_widget;
    RefPtr<GUI::Widget> m_find_widget;
    RefPtr<GUI::Widget> m_replace_widget;

    GUI::ActionGroup syntax_actions;
    RefPtr<GUI::Action> m_plain_text_highlight;
    RefPtr<GUI::Action> m_cpp_highlight;
    RefPtr<GUI::Action> m_js_highlight;
    RefPtr<GUI::Action> m_ini_highlight;
    RefPtr<GUI::Action> m_shell_highlight;

    RefPtr<Web::OutOfProcessWebView> m_page_view;

    GUI::ActionGroup font_actions;

    bool m_document_dirty { false };
    bool m_document_opening { false };
    bool m_auto_detect_preview_mode { false };

    PreviewMode m_preview_mode { PreviewMode::None };
};
