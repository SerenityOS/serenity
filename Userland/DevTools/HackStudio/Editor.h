/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * 2018-2021, the SerenityOS developers
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

#include "CodeDocument.h"
#include "Debugger/BreakpointCallback.h"
#include "LanguageClient.h"
#include <AK/Assertions.h>
#include <AK/Optional.h>
#include <AK/OwnPtr.h>
#include <LibGUI/TextEditor.h>
#include <LibWeb/Forward.h>

namespace HackStudio {

class EditorWrapper;

class Editor final : public GUI::TextEditor {
    C_OBJECT(Editor)
public:
    virtual ~Editor() override;

    Function<void()> on_focus;
    Function<void(String)> on_open;

    EditorWrapper& wrapper();
    const EditorWrapper& wrapper() const;

    const Vector<size_t>& breakpoint_lines() const { return code_document().breakpoint_lines(); }
    Vector<size_t>& breakpoint_lines() { return code_document().breakpoint_lines(); }
    Optional<size_t> execution_position() const { return code_document().execution_position(); }
    void set_execution_position(size_t line_number);
    void clear_execution_position();

    const CodeDocument& code_document() const;
    CodeDocument& code_document();

    virtual void set_document(GUI::TextDocument&) override;

    virtual void on_edit_action(const GUI::Command&) override;

    virtual void undo() override;
    virtual void redo() override;

    LanguageClient& language_client()
    {
        VERIFY(m_language_client);
        return *m_language_client;
    }
    virtual void set_cursor(const GUI::TextPosition& a_position) override;

private:
    virtual void focusin_event(GUI::FocusEvent&) override;
    virtual void focusout_event(GUI::FocusEvent&) override;
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mousemove_event(GUI::MouseEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void enter_event(Core::Event&) override;
    virtual void leave_event(Core::Event&) override;

    void show_documentation_tooltip_if_available(const String&, const Gfx::IntPoint& screen_location);
    void navigate_to_include_if_available(String);
    void on_navigatable_link_click(const GUI::TextDocumentSpan&);
    void on_identifier_click(const GUI::TextDocumentSpan&);

    Gfx::IntRect breakpoint_icon_rect(size_t line_number) const;
    static const Gfx::Bitmap& breakpoint_icon_bitmap();
    static const Gfx::Bitmap& current_position_icon_bitmap();

    struct AutoCompleteRequestData {
        GUI::TextPosition position;
    };

    class LanguageServerAidedAutocompleteProvider final : virtual public GUI::AutocompleteProvider {
    public:
        LanguageServerAidedAutocompleteProvider(LanguageClient& client)
            : m_language_client(client)
        {
        }
        virtual ~LanguageServerAidedAutocompleteProvider() override { }

    private:
        virtual void provide_completions(Function<void(Vector<Entry>)> callback) override;
        LanguageClient& m_language_client;
    };

    Optional<AutoCompleteRequestData> get_autocomplete_request_data();

    void flush_file_content_to_langauge_server();

    explicit Editor();

    RefPtr<GUI::Window> m_documentation_tooltip_window;
    RefPtr<Web::OutOfProcessWebView> m_documentation_page_view;
    String m_last_parsed_token;
    GUI::TextPosition m_previous_text_position { 0, 0 };
    bool m_hovering_editor { false };
    bool m_hovering_clickable { false };
    bool m_autocomplete_in_focus { false };

    OwnPtr<LanguageClient> m_language_client;
};

}
