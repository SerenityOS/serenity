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

#include <LibGUI/TextEditor.h>

class EditorWrapper;
class HtmlView;

class Editor final : public GUI::TextEditor {
    C_OBJECT(Editor)
public:
    virtual ~Editor() override;

    Function<void()> on_focus;

    EditorWrapper& wrapper();
    const EditorWrapper& wrapper() const;

    void notify_did_rehighlight();

private:
    virtual void focusin_event(Core::Event&) override;
    virtual void focusout_event(Core::Event&) override;
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mousemove_event(GUI::MouseEvent&) override;
    virtual void cursor_did_change() override;

    void show_documentation_tooltip_if_available(const String&, const Gfx::Point& screen_location);
    void highlight_matching_token_pair();

    explicit Editor(GUI::Widget* parent);

    RefPtr<GUI::Window> m_documentation_tooltip_window;
    RefPtr<HtmlView> m_documentation_html_view;
    String m_last_parsed_token;

    struct BuddySpan {
        int index { -1 };
        GUI::TextDocumentSpan span_backup;
    };

    bool m_has_brace_buddies { false };
    BuddySpan m_brace_buddies[2];
};
