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
#include <AK/HashMap.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/NonnullRefPtrVector.h>
#include <LibCore/CTimer.h>
#include <LibDraw/TextAlignment.h>
#include <LibGUI/GScrollableWidget.h>
#include <LibGUI/GTextDocument.h>
#include <LibGUI/GTextRange.h>

namespace GUI {

class Action;
class Menu;
class Painter;
class ScrollBar;

class TextEditor
    : public ScrollableWidget
    , public TextDocument::Client {
    C_OBJECT(TextEditor)
public:
    enum Type {
        MultiLine,
        SingleLine
    };
    virtual ~TextEditor() override;

    const TextDocument& document() const { return *m_document; }
    TextDocument& document() { return *m_document; }

    void set_document(TextDocument&);

    bool is_readonly() const { return m_readonly; }
    void set_readonly(bool);

    virtual bool is_automatic_indentation_enabled() const final { return m_automatic_indentation_enabled; }
    void set_automatic_indentation_enabled(bool enabled) { m_automatic_indentation_enabled = enabled; }

    virtual int soft_tab_width() const final { return m_soft_tab_width; }

    bool is_line_wrapping_enabled() const { return m_line_wrapping_enabled; }
    void set_line_wrapping_enabled(bool);

    TextAlignment text_alignment() const { return m_text_alignment; }
    void set_text_alignment(TextAlignment);

    Type type() const { return m_type; }
    bool is_single_line() const { return m_type == SingleLine; }
    bool is_multi_line() const { return m_type == MultiLine; }

    bool is_ruler_visible() const { return m_ruler_visible; }
    void set_ruler_visible(bool b) { m_ruler_visible = b; }

    Function<void()> on_cursor_change;
    Function<void()> on_selection_change;

    void set_text(const StringView&);
    void scroll_cursor_into_view();
    void scroll_position_into_view(const TextPosition&);
    size_t line_count() const { return document().line_count(); }
    int line_spacing() const { return m_line_spacing; }
    int line_height() const { return font().glyph_height() + m_line_spacing; }
    TextPosition cursor() const { return m_cursor; }
    TextRange normalized_selection() const { return m_selection.normalized(); }
    // FIXME: This should take glyph spacing into account, no?
    int glyph_width() const { return font().glyph_width('x'); }

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
    void undo() { document().undo(); }
    void redo() { document().redo(); }

    Function<void()> on_change;
    Function<void()> on_return_pressed;
    Function<void()> on_escape_pressed;

    Action& undo_action() { return *m_undo_action; }
    Action& redo_action() { return *m_redo_action; }
    Action& cut_action() { return *m_cut_action; }
    Action& copy_action() { return *m_copy_action; }
    Action& paste_action() { return *m_paste_action; }
    Action& delete_action() { return *m_delete_action; }
    Action& go_to_line_action() { return *m_go_to_line_action; }

    void add_custom_context_menu_action(Action&);

    void set_cursor(size_t line, size_t column);
    void set_cursor(const TextPosition&);

protected:
    explicit TextEditor(Widget* parent);
    explicit TextEditor(Type, Widget* parent);

    virtual void did_change_font() override;
    virtual void paint_event(PaintEvent&) override;
    virtual void mousedown_event(MouseEvent&) override;
    virtual void mouseup_event(MouseEvent&) override;
    virtual void mousemove_event(MouseEvent&) override;
    virtual void doubleclick_event(MouseEvent&) override;
    virtual void keydown_event(KeyEvent&) override;
    virtual void focusin_event(Core::Event&) override;
    virtual void focusout_event(Core::Event&) override;
    virtual void timer_event(Core::TimerEvent&) override;
    virtual bool accepts_focus() const override { return true; }
    virtual void enter_event(Core::Event&) override;
    virtual void leave_event(Core::Event&) override;
    virtual void context_menu_event(ContextMenuEvent&) override;
    virtual void resize_event(ResizeEvent&) override;
    virtual void cursor_did_change() {}

    TextPosition text_position_at(const Point&) const;

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

    Rect line_content_rect(size_t item_index) const;
    Rect line_widget_rect(size_t line_index) const;
    Rect cursor_content_rect() const;
    Rect content_rect_for_position(const TextPosition&) const;
    void update_cursor();
    const NonnullOwnPtrVector<TextDocumentLine>& lines() const { return document().lines(); }
    NonnullOwnPtrVector<TextDocumentLine>& lines() { return document().lines(); }
    TextDocumentLine& line(size_t index) { return document().line(index); }
    const TextDocumentLine& line(size_t index) const { return document().line(index); }
    TextDocumentLine& current_line() { return line(m_cursor.line()); }
    const TextDocumentLine& current_line() const { return line(m_cursor.line()); }
    int ruler_width() const;
    Rect ruler_content_rect(size_t line) const;
    void toggle_selection_if_needed_for_event(const KeyEvent&);
    void delete_selection();
    void did_update_selection();
    int content_x_for_position(const TextPosition&) const;
    Rect ruler_rect_in_inner_coordinates() const;
    Rect visible_text_rect_in_inner_coordinates() const;
    void recompute_all_visual_lines();
    void ensure_cursor_is_valid();
    void flush_pending_change_notification_if_needed();
    void get_selection_line_boundaries(size_t& first_line, size_t& last_line);
    void move_selected_lines_up();
    void move_selected_lines_down();
    void sort_selected_lines();

    size_t visual_line_containing(size_t line_index, size_t column) const;
    void recompute_visual_lines(size_t line_index);

    template<class T, class... Args>
    inline void execute(Args&&... args)
    {
        auto command = make<T>(*m_document, forward<Args>(args)...);
        command->execute_from(*this);
        m_document->add_to_undo_stack(move(command));
    }

    Type m_type { MultiLine };

    TextPosition m_cursor;
    TextAlignment m_text_alignment { TextAlignment::CenterLeft };
    bool m_cursor_state { true };
    bool m_in_drag_select { false };
    bool m_ruler_visible { false };
    bool m_has_pending_change_notification { false };
    bool m_automatic_indentation_enabled { false };
    bool m_line_wrapping_enabled { false };
    bool m_readonly { false };
    int m_line_spacing { 4 };
    size_t m_soft_tab_width { 4 };
    int m_horizontal_content_padding { 2 };
    TextRange m_selection;
    RefPtr<Menu> m_context_menu;
    RefPtr<Action> m_undo_action;
    RefPtr<Action> m_redo_action;
    RefPtr<Action> m_cut_action;
    RefPtr<Action> m_copy_action;
    RefPtr<Action> m_paste_action;
    RefPtr<Action> m_delete_action;
    RefPtr<Action> m_go_to_line_action;
    Core::ElapsedTimer m_triple_click_timer;
    NonnullRefPtrVector<Action> m_custom_context_menu_actions;

    RefPtr<TextDocument> m_document;

    template<typename Callback>
    void for_each_visual_line(size_t line_index, Callback) const;

    struct LineVisualData {
        Vector<size_t, 1> visual_line_breaks;
        Rect visual_rect;
    };

    NonnullOwnPtrVector<LineVisualData> m_line_visual_data;
};

}
