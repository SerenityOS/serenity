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

class GAction;
class GMenu;
class GScrollBar;
class Painter;

class GTextEditor
    : public GScrollableWidget
    , public GTextDocument::Client {
    C_OBJECT(GTextEditor)
public:
    enum Type {
        MultiLine,
        SingleLine
    };
    virtual ~GTextEditor() override;

    const GTextDocument& document() const { return *m_document; }
    GTextDocument& document() { return *m_document; }

    void set_document(GTextDocument&);

    bool is_readonly() const { return m_readonly; }
    void set_readonly(bool);

    bool is_automatic_indentation_enabled() const { return m_automatic_indentation_enabled; }
    void set_automatic_indentation_enabled(bool enabled) { m_automatic_indentation_enabled = enabled; }

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
    void scroll_position_into_view(const GTextPosition&);
    int line_count() const { return document().line_count(); }
    int line_spacing() const { return m_line_spacing; }
    int line_height() const { return font().glyph_height() + m_line_spacing; }
    GTextPosition cursor() const { return m_cursor; }
    GTextRange normalized_selection() const { return m_selection.normalized(); }
    // FIXME: This should take glyph spacing into account, no?
    int glyph_width() const { return font().glyph_width('x'); }

    bool write_to_file(const StringView& path);

    bool has_selection() const { return m_selection.is_valid(); }
    String selected_text() const;
    void set_selection(const GTextRange&);
    bool can_undo() const { return m_undo_stack_index < m_undo_stack.size() && !m_undo_stack.is_empty(); }
    bool can_redo() const { return m_undo_stack_index > 0 && m_undo_stack[m_undo_stack_index - 1].m_undo_vector.size() > 0 && !m_undo_stack.is_empty(); }

    String text() const;

    void clear();

    void cut();
    void copy();
    void paste();
    void do_delete();
    void delete_current_line();
    void select_all();
    void undo();
    void redo();

    Function<void()> on_change;
    Function<void()> on_return_pressed;
    Function<void()> on_escape_pressed;

    GAction& undo_action() { return *m_undo_action; }
    GAction& redo_action() { return *m_redo_action; }
    GAction& cut_action() { return *m_cut_action; }
    GAction& copy_action() { return *m_copy_action; }
    GAction& paste_action() { return *m_paste_action; }
    GAction& delete_action() { return *m_delete_action; }

    void add_custom_context_menu_action(GAction&);

    void set_cursor(int line, int column);
    void set_cursor(const GTextPosition&);

protected:
    explicit GTextEditor(GWidget* parent);
    explicit GTextEditor(Type, GWidget* parent);

    virtual void did_change_font() override;
    virtual void paint_event(GPaintEvent&) override;
    virtual void mousedown_event(GMouseEvent&) override;
    virtual void mouseup_event(GMouseEvent&) override;
    virtual void mousemove_event(GMouseEvent&) override;
    virtual void doubleclick_event(GMouseEvent&) override;
    virtual void keydown_event(GKeyEvent&) override;
    virtual void focusin_event(CEvent&) override;
    virtual void focusout_event(CEvent&) override;
    virtual void timer_event(CTimerEvent&) override;
    virtual bool accepts_focus() const override { return true; }
    virtual void enter_event(CEvent&) override;
    virtual void leave_event(CEvent&) override;
    virtual void context_menu_event(GContextMenuEvent&) override;
    virtual void resize_event(GResizeEvent&) override;
    virtual void cursor_did_change() {}

    GTextPosition text_position_at(const Point&) const;

private:
    friend class GTextDocumentLine;

    // ^GTextDocument::Client
    virtual void document_did_append_line() override;
    virtual void document_did_insert_line(int) override;
    virtual void document_did_remove_line(int) override;
    virtual void document_did_remove_all_lines() override;
    virtual void document_did_change() override;

    void create_actions();
    void paint_ruler(Painter&);
    void update_content_size();
    void did_change();

    Rect line_content_rect(int item_index) const;
    Rect line_widget_rect(int line_index) const;
    Rect cursor_content_rect() const;
    Rect content_rect_for_position(const GTextPosition&) const;
    void update_cursor();
    void update_undo_timer();
    const NonnullOwnPtrVector<GTextDocumentLine>& lines() const { return document().lines(); }
    NonnullOwnPtrVector<GTextDocumentLine>& lines() { return document().lines(); }
    GTextDocumentLine& line(int index) { return document().line(index); }
    const GTextDocumentLine& line(int index) const { return document().line(index); }
    GTextDocumentLine& current_line() { return line(m_cursor.line()); }
    const GTextDocumentLine& current_line() const { return line(m_cursor.line()); }
    void insert_at_cursor(char);
    void insert_at_cursor(const StringView&);
    int ruler_width() const;
    Rect ruler_content_rect(int line) const;
    void toggle_selection_if_needed_for_event(const GKeyEvent&);
    void insert_at_cursor_or_replace_selection(const StringView&);
    void delete_selection();
    void did_update_selection();
    int content_x_for_position(const GTextPosition&) const;
    Rect ruler_rect_in_inner_coordinates() const;
    Rect visible_text_rect_in_inner_coordinates() const;
    void recompute_all_visual_lines();
    void ensure_cursor_is_valid();
    void flush_pending_change_notification_if_needed();
    void get_selection_line_boundaries(int& first_line, int& last_line);
    void move_selected_lines_up();
    void move_selected_lines_down();
    void sort_selected_lines();

    class UndoCommand {

    public:
        UndoCommand(GTextEditor& text_editor);
        virtual ~UndoCommand();
        virtual void undo();
        virtual void redo();

    protected:
        GTextEditor& m_text_editor;
    };

    class InsertCharacterCommand : public UndoCommand {
    public:
        InsertCharacterCommand(GTextEditor& text_editor, char ch, GTextPosition text_position);
        virtual void undo() override;
        virtual void redo() override;

    private:
        char m_character;
        GTextPosition m_text_position;
    };

    class RemoveCharacterCommand : public UndoCommand {
    public:
        RemoveCharacterCommand(GTextEditor& text_editor, char ch, GTextPosition text_position);
        virtual void undo() override;
        virtual void redo() override;

    private:
        char m_character;
        GTextPosition m_text_position;
    };

    class RemoveLineCommand : public UndoCommand {
    public:
        RemoveLineCommand(GTextEditor& text_editor, String, GTextPosition text_position, bool has_merged_content);
        virtual void undo() override;
        virtual void redo() override;

    private:
        String m_line_content;
        GTextPosition m_text_position;
        bool m_has_merged_content;
    };

    class CreateLineCommand : public UndoCommand {
    public:
        CreateLineCommand(GTextEditor& text_editor, Vector<char> line_content, GTextPosition text_position);
        virtual void undo() override;
        virtual void redo() override;

    private:
        Vector<char> m_line_content;
        GTextPosition m_text_position;
    };

    struct UndoCommandsContainer {
        NonnullOwnPtrVector<UndoCommand> m_undo_vector;
    };

    void add_to_undo_stack(NonnullOwnPtr<UndoCommand> undo_command);
    int visual_line_containing(int line_index, int column) const;
    void recompute_visual_lines(int line_index);

    Type m_type { MultiLine };

    GTextPosition m_cursor;
    TextAlignment m_text_alignment { TextAlignment::CenterLeft };
    bool m_cursor_state { true };
    bool m_in_drag_select { false };
    bool m_ruler_visible { false };
    bool m_has_pending_change_notification { false };
    bool m_automatic_indentation_enabled { false };
    bool m_line_wrapping_enabled { false };
    bool m_readonly { false };
    int m_line_spacing { 4 };
    int m_soft_tab_width { 4 };
    int m_horizontal_content_padding { 2 };
    GTextRange m_selection;
    OwnPtr<GMenu> m_context_menu;
    RefPtr<GAction> m_undo_action;
    RefPtr<GAction> m_redo_action;
    RefPtr<GAction> m_cut_action;
    RefPtr<GAction> m_copy_action;
    RefPtr<GAction> m_paste_action;
    RefPtr<GAction> m_delete_action;
    CElapsedTimer m_triple_click_timer;
    NonnullRefPtrVector<GAction> m_custom_context_menu_actions;
    NonnullOwnPtrVector<UndoCommandsContainer> m_undo_stack;
    int m_undo_stack_index = 0;
    RefPtr<CTimer> m_undo_timer;
    int m_last_updated_undo_vector_size = 0;

    RefPtr<GTextDocument> m_document;

    template<typename Callback>
    void for_each_visual_line(int line_index, Callback) const;

    struct LineVisualData {
        Vector<int, 1> visual_line_breaks;
        Rect visual_rect;
    };

    NonnullOwnPtrVector<LineVisualData> m_line_visual_data;
};
