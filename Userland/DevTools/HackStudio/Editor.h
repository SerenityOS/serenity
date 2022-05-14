/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2022, Itamar S. <itamar8910@gmail.com>
 * Copyright (c) 2018-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "CodeDocument.h"
#include "Debugger/BreakpointCallback.h"
#include "LanguageClient.h"
#include <AK/Assertions.h>
#include <AK/Optional.h>
#include <AK/OwnPtr.h>
#include <LibGUI/TextEditor.h>
#include <LibWebView/Forward.h>

namespace HackStudio {

class EditorWrapper;

class Editor final : public GUI::TextEditor {
    C_OBJECT_ABSTRACT(Editor)
public:
    static ErrorOr<NonnullRefPtr<Editor>> try_create();

    virtual ~Editor() override = default;

    Function<void()> on_focus;
    Function<void(String)> on_open;

    EditorWrapper& wrapper();
    EditorWrapper const& wrapper() const;

    Vector<size_t> const& breakpoint_lines() const { return code_document().breakpoint_lines(); }
    Vector<size_t>& breakpoint_lines() { return code_document().breakpoint_lines(); }
    Optional<size_t> execution_position() const { return code_document().execution_position(); }
    bool is_program_running() const { return execution_position().has_value(); }
    void set_execution_position(size_t line_number);
    void clear_execution_position();
    void set_debug_mode(bool);

    CodeDocument const& code_document() const;
    CodeDocument& code_document();

    virtual void set_document(GUI::TextDocument&) override;
    virtual void will_execute(GUI::TextDocumentUndoCommand const&) override;

    virtual void undo() override;
    virtual void redo() override;

    LanguageClient& language_client()
    {
        VERIFY(m_language_client);
        return *m_language_client;
    }
    virtual void set_cursor(const GUI::TextPosition& a_position) override;
    void set_semantic_syntax_highlighting(bool value);

private:
    virtual void focusin_event(GUI::FocusEvent&) override;
    virtual void focusout_event(GUI::FocusEvent&) override;
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mousemove_event(GUI::MouseEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void drop_event(GUI::DropEvent&) override;
    virtual void enter_event(Core::Event&) override;
    virtual void leave_event(Core::Event&) override;
    virtual void keydown_event(GUI::KeyEvent&) override;

    void show_documentation_tooltip_if_available(String const&, Gfx::IntPoint const& screen_location);
    void navigate_to_include_if_available(String);
    void on_navigatable_link_click(const GUI::TextDocumentSpan&);
    void on_identifier_click(const GUI::TextDocumentSpan&);

    Gfx::IntRect gutter_icon_rect(size_t line_number) const;
    static Gfx::Bitmap const& breakpoint_icon_bitmap();
    static Gfx::Bitmap const& current_position_icon_bitmap();

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
    void set_syntax_highlighter_for(CodeDocument const&);
    void set_language_client_for(CodeDocument const&);
    void set_autocomplete_provider_for(CodeDocument const&);
    void handle_function_parameters_hint_request();
    void on_token_info_timer_tick();
    void on_tokens_info_result(Vector<GUI::AutocompleteProvider::TokenInfo> const& tokens_info);
    void create_tokens_info_timer();
    ErrorOr<void> initialize_documentation_tooltip();
    ErrorOr<void> initialize_parameters_hint_tooltip();

    explicit Editor();

    RefPtr<GUI::Window> m_documentation_tooltip_window;
    RefPtr<GUI::Window> m_parameters_hint_tooltip_window;
    RefPtr<WebView::OutOfProcessWebView> m_documentation_page_view;
    RefPtr<WebView::OutOfProcessWebView> m_parameter_hint_page_view;
    String m_last_parsed_token;
    GUI::TextPosition m_previous_text_position { 0, 0 };
    bool m_hovering_editor { false };
    bool m_hovering_clickable { false };
    bool m_autocomplete_in_focus { false };
    RefPtr<GUI::Action> m_move_execution_to_line_action;
    RefPtr<Core::Timer> m_tokens_info_timer; // Used for querying language server for syntax highlighting info
    OwnPtr<LanguageClient> m_language_client;
    bool m_use_semantic_syntax_highlighting { false };
};

}
