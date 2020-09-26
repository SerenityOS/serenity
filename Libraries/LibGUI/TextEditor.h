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
#include <AK/NonnullOwnPtrVector.h>
#include <AK/NonnullRefPtrVector.h>
#include <LibCore/ElapsedTimer.h>
#include <LibGUI/ScrollableWidget.h>
#include <LibGUI/TextDocument.h>
#include <LibGUI/TextRange.h>
#include <LibGfx/TextAlignment.h>

namespace GUI {

class TextEditor
    : public ScrollableWidget
    , public TextDocument::Client {
    C_OBJECT(TextEditor)
public:
    enum Type {
        MultiLine,
        SingleLine
    };

    enum Mode {
        Editable,
        ReadOnly,
        DisplayOnly
    };

    virtual ~TextEditor() override;

    const TextDocument& document() const { return *m_document; }
    TextDocument& document() { return *m_document; }

    virtual void set_document(TextDocument&);

    const String& placeholder() const { return m_placeholder; }
    void set_placeholder(const StringView& placeholder) { m_placeholder = placeholder; }

    void set_visualize_trailing_whitespace(bool);
    bool visualize_trailing_whitespace() const { return m_visualize_trailing_whitespace; }

    bool has_visible_list() const { return m_has_visible_list; }
    void set_has_visible_list(bool);
    bool has_open_button() const { return m_has_open_button; }
    void set_has_open_button(bool);

    virtual bool is_automatic_indentation_enabled() const final { return m_automatic_indentation_enabled; }
    void set_automatic_indentation_enabled(bool enabled) { m_automatic_indentation_enabled = enabled; }

    virtual int soft_tab_width() const final { return m_soft_tab_width; }

    bool is_line_wrapping_enabled() const { return m_line_wrapping_enabled; }
    void set_line_wrapping_enabled(bool);

    Gfx::TextAlignment text_alignment() const { return m_text_alignment; }
    void set_text_alignment(Gfx::TextAlignment);

    Type type() const { return m_type; }
    bool is_single_line() const { return m_type == SingleLine; }
    bool is_multi_line() const { return m_type == MultiLine; }

    Mode mode() const { return m_mode; }
    bool is_editable() const { return m_mode == Editable; }
    bool is_readonly() const { return m_mode == ReadOnly; }
    bool is_displayonly() const { return m_mode == DisplayOnly; }
    void set_mode(const Mode);

    bool is_ruler_visible() const { return m_ruler_visible; }
    void set_ruler_visible(bool b) { m_ruler_visible = b; }

    void set_icon(const Gfx::Bitmap*);
    const Gfx::Bitmap* icon() const { return m_icon; }

    Function<void()> on_cursor_change;
    Function<void()> on_selection_change;
    Function<void()> on_focusin;
    Function<void()> on_focusout;

    void set_text(const StringView&);
    void scroll_cursor_into_view();
    void scroll_position_into_view(const TextPosition&);
    size_t line_count() const { return document().line_count(); }
    int line_spacing() const { return m_line_spacing; }
    int line_height() const;
    TextPosition cursor() const { return m_cursor; }
    TextRange normalized_selection() const { return m_selection.normalized(); }

    void insert_at_cursor_or_replace_selection(const StringView&);
    bool write_to_file(const StringView& path);
    bool has_selection() const { return m_selection.is_valid(); }
    String selected_text() const;
    void set_selection(const TextRange&);
    void clear_selection();
    bool can_undo() const { return document().can_undo(); }
    bool can_redo() const { return document().can_redo(); }

    String text() const;

    void clear();

    void cut();
    void copy();
    void paste();
    void do_delete();
    void delete_current_line();
    void select_all();
    virtual void undo() { document().undo(); }
    virtual void redo() { document().redo(); }

    Function<void()> on_change;
    Function<void()> on_mousedown;
    Function<void()> on_return_pressed;
    Function<void()> on_escape_pressed;
    Function<void()> on_up_pressed;
    Function<void()> on_down_pressed;
    Function<void()> on_pageup_pressed;
    Function<void()> on_pagedown_pressed;

    Action& undo_action() { return *m_undo_action; }
    Action& redo_action() { return *m_redo_action; }
    Action& cut_action() { return *m_cut_action; }
    Action& copy_action() { return *m_copy_action; }
    Action& paste_action() { return *m_paste_action; }
    Action& delete_action() { return *m_delete_action; }
    Action& go_to_line_action() { return *m_go_to_line_action; }
    Action& select_all_action() { return *m_select_all_action; }

    void add_custom_context_menu_action(Action&);

    void set_cursor(size_t line, size_t column);
    void set_cursor(const TextPosition&);

    const SyntaxHighlighter* syntax_highlighter() const;
    void set_syntax_highlighter(OwnPtr<SyntaxHighlighter>);

    bool is_in_drag_select() const { return m_in_drag_select; }

protected:
    explicit TextEditor(Type = Type::MultiLine);

    virtual void did_change_font() override;
    virtual void paint_event(PaintEvent&) override;
    virtual void mousedown_event(MouseEvent&) override;
    virtual void mouseup_event(MouseEvent&) override;
    virtual void mousemove_event(MouseEvent&) override;
    virtual void doubleclick_event(MouseEvent&) override;
    virtual void keydown_event(KeyEvent&) override;
    virtual void focusin_event(FocusEvent&) override;
    virtual void focusout_event(FocusEvent&) override;
    virtual void timer_event(Core::TimerEvent&) override;
    virtual bool accepts_focus() const override { return true; }
    virtual void enter_event(Core::Event&) override;
    virtual void leave_event(Core::Event&) override;
    virtual void context_menu_event(ContextMenuEvent&) override;
    virtual void resize_event(ResizeEvent&) override;
    virtual void theme_change_event(ThemeChangeEvent&) override;
    virtual void cursor_did_change() { }
    Gfx::IntRect ruler_content_rect(size_t line) const;

    TextPosition text_position_at(const Gfx::IntPoint&) const;
    bool ruler_visible() const { return m_ruler_visible; }
    Gfx::IntRect content_rect_for_position(const TextPosition&) const;
    int ruler_width() const;

private:
    friend class TextDocumentLine;

    // ^TextDocument::Client
    virtual void document_did_append_line() override;
    virtual void document_did_insert_line(size_t) override;
    virtual void document_did_remove_line(size_t) override;
    virtual void document_did_remove_all_lines() override;
    virtual void document_did_change() override;
    virtual void document_did_set_text() override;
    virtual void document_did_set_cursor(const TextPosition&) override;

    void create_actions();
    void paint_ruler(Painter&);
    void update_content_size();
    void did_change();
    int fixed_glyph_width() const;

    void defer_reflow();
    void undefer_reflow();

    int icon_size() const { return 16; }
    int icon_padding() const { return 2; }
    int button_padding() const { return m_has_open_button ? 17 : 2; }

    class ReflowDeferrer {
    public:
        ReflowDeferrer(TextEditor& editor)
            : m_editor(editor)
        {
            m_editor.defer_reflow();
        }
        ~ReflowDeferrer()
        {
            m_editor.undefer_reflow();
        }

    private:
        TextEditor& m_editor;
    };

    Gfx::IntRect line_content_rect(size_t item_index) const;
    Gfx::IntRect line_widget_rect(size_t line_index) const;
    Gfx::IntRect cursor_content_rect() const;
    void update_cursor();
    const NonnullOwnPtrVector<TextDocumentLine>& lines() const { return document().lines(); }
    NonnullOwnPtrVector<TextDocumentLine>& lines() { return document().lines(); }
    TextDocumentLine& line(size_t index) { return document().line(index); }
    const TextDocumentLine& line(size_t index) const { return document().line(index); }
    TextDocumentLine& current_line() { return line(m_cursor.line()); }
    const TextDocumentLine& current_line() const { return line(m_cursor.line()); }
    void toggle_selection_if_needed_for_event(const KeyEvent&);
    void delete_selection();
    void did_update_selection();
    int content_x_for_position(const TextPosition&) const;
    Gfx::IntRect ruler_rect_in_inner_coordinates() const;
    Gfx::IntRect visible_text_rect_in_inner_coordinates() const;
    void recompute_all_visual_lines();
    void ensure_cursor_is_valid();
    void flush_pending_change_notification_if_needed();
    void get_selection_line_boundaries(size_t& first_line, size_t& last_line);
    void move_selected_lines_up();
    void move_selected_lines_down();
    void sort_selected_lines();

    size_t visual_line_containing(size_t line_index, size_t column) const;
    void recompute_visual_lines(size_t line_index);

    void automatic_selection_scroll_timer_fired();

    template<class T, class... Args>
    inline void execute(Args&&... args)
    {
        auto command = make<T>(*m_document, forward<Args>(args)...);
        on_edit_action(*command);
        command->execute_from(*this);
        m_document->add_to_undo_stack(move(command));
    }

    virtual void on_edit_action(const Command&) { }

    Type m_type { MultiLine };
    Mode m_mode { Editable };

    TextPosition m_cursor;
    Gfx::TextAlignment m_text_alignment { Gfx::TextAlignment::CenterLeft };
    bool m_cursor_state { true };
    bool m_in_drag_select { false };
    bool m_ruler_visible { false };
    bool m_has_pending_change_notification { false };
    bool m_automatic_indentation_enabled { false };
    bool m_line_wrapping_enabled { false };
    bool m_has_visible_list { false };
    bool m_has_open_button { false };
    bool m_visualize_trailing_whitespace { true };
    int m_line_spacing { 4 };
    size_t m_soft_tab_width { 4 };
    int m_horizontal_content_padding { 3 };
    TextRange m_selection;
    RefPtr<Menu> m_context_menu;
    RefPtr<Action> m_undo_action;
    RefPtr<Action> m_redo_action;
    RefPtr<Action> m_cut_action;
    RefPtr<Action> m_copy_action;
    RefPtr<Action> m_paste_action;
    RefPtr<Action> m_delete_action;
    RefPtr<Action> m_go_to_line_action;
    RefPtr<Action> m_select_all_action;
    Core::ElapsedTimer m_triple_click_timer;
    NonnullRefPtrVector<Action> m_custom_context_menu_actions;

    size_t m_reflow_deferred { 0 };
    bool m_reflow_requested { false };

    bool is_visual_data_up_to_date() const { return !m_reflow_requested; }

    RefPtr<TextDocument> m_document;

    String m_placeholder { "" };

    template<typename Callback>
    void for_each_visual_line(size_t line_index, Callback) const;

    struct LineVisualData {
        Vector<size_t, 1> visual_line_breaks;
        Gfx::IntRect visual_rect;
    };

    NonnullOwnPtrVector<LineVisualData> m_line_visual_data;

    OwnPtr<SyntaxHighlighter> m_highlighter;

    RefPtr<Core::Timer> m_automatic_selection_scroll_timer;
    Gfx::IntPoint m_last_mousemove_position;

    RefPtr<Gfx::Bitmap> m_icon;
};

}
