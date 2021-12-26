#pragma once

#include <LibGUI/GScrollableWidget.h>
#include <AK/Function.h>
#include <AK/HashMap.h>

class GScrollBar;
class Painter;

class GTextPosition {
public:
    GTextPosition() { }
    GTextPosition(int line, int column)
        : m_line(line)
        , m_column(column)
    {
    }

    bool is_valid() const { return m_line >= 0 && m_column >= 0; }

    int line() const { return m_line; }
    int column() const { return m_column; }

    void set_line(int line) { m_line = line; }
    void set_column(int column) { m_column = column; }

    bool operator==(const GTextPosition& other) const { return m_line == other.m_line && m_column == other.m_column; }
    bool operator!=(const GTextPosition& other) const { return m_line != other.m_line || m_column != other.m_column; }
    bool operator<(const GTextPosition& other) const { return m_line < other.m_line || (m_line == other.m_line && m_column < other.m_column); }

private:
    int m_line { -1 };
    int m_column { -1 };
};

class GTextRange {
public:
    GTextRange() { }
    GTextRange(const GTextPosition& start, const GTextPosition& end) : m_start(start) , m_end(end) { }

    bool is_valid() const { return m_start.is_valid() && m_end.is_valid(); }
    void clear() { m_start = { }; m_end = { }; }

    GTextPosition& start() { return m_start; }
    GTextPosition& end() { return m_end; }
    const GTextPosition& start() const { return m_start; }
    const GTextPosition& end() const { return m_end; }

    GTextRange normalized() const { return GTextRange(normalized_start(), normalized_end()); }

    void set_start(const GTextPosition& position) { m_start = position; }
    void set_end(const GTextPosition& position) { m_end = position; }

    void set(const GTextPosition& start, const GTextPosition& end) { m_start = start; m_end = end; }

private:
    GTextPosition normalized_start() const { return m_start < m_end ? m_start : m_end; }
    GTextPosition normalized_end() const { return m_start < m_end ? m_end : m_start; }

    GTextPosition m_start;
    GTextPosition m_end;
};

class GTextEditor : public GScrollableWidget {
public:
    enum Type { MultiLine, SingleLine };
    GTextEditor(Type, GWidget* parent);
    virtual ~GTextEditor() override;

    Type type() const { return m_type; }
    bool is_single_line() const { return m_type == SingleLine; }
    bool is_multi_line() const { return m_type == MultiLine; }

    Function<void(GTextEditor&)> on_cursor_change;

    void set_text(const String&);
    void scroll_cursor_into_view();
    int line_count() const { return m_lines.size(); }
    int line_spacing() const { return m_line_spacing; }
    int line_height() const { return font().glyph_height() + m_line_spacing; }
    GTextPosition cursor() const { return m_cursor; }
    GTextRange normalized_selection() const { return m_selection.normalized(); }
    int glyph_width() const { return font().glyph_width('x'); }

    bool write_to_file(const String& path);

    bool has_selection() const { return m_selection.is_valid(); }
    String selected_text() const;
    String text() const;

    void clear();

    void cut();
    void copy();
    void paste();

    Function<void(GTextEditor&)> on_return_pressed;

    virtual const char* class_name() const override { return "GTextEditor"; }

private:
    virtual void paint_event(GPaintEvent&) override;
    virtual void mousedown_event(GMouseEvent&) override;
    virtual void mouseup_event(GMouseEvent&) override;
    virtual void mousemove_event(GMouseEvent&) override;
    virtual void keydown_event(GKeyEvent&) override;
    virtual void focusin_event(GEvent&) override;
    virtual void focusout_event(GEvent&) override;
    virtual void timer_event(GTimerEvent&) override;
    virtual bool accepts_focus() const override { return true; }
    void paint_ruler(Painter&);
    void update_content_size();

    class Line {
        friend class GTextEditor;
    public:
        Line();

        const char* characters() const { return m_text.data(); }
        int length() const { return m_text.size() - 1; }
        int width(const Font&) const;
        void set_text(const String&);
        void append(char);
        void prepend(char);
        void insert(int index, char);
        void remove(int index);
        void append(const char*, int);
        void truncate(int length);
        void clear();

    private:
        // NOTE: This vector is null terminated.
        Vector<char> m_text;
    };

    Rect line_content_rect(int item_index) const;
    Rect line_widget_rect(int line_index) const;
    Rect cursor_content_rect() const;
    void update_cursor();
    void set_cursor(int line, int column);
    void set_cursor(const GTextPosition&);
    Line& current_line() { return *m_lines[m_cursor.line()]; }
    const Line& current_line() const { return *m_lines[m_cursor.line()]; }
    GTextPosition text_position_at(const Point&) const;
    void insert_at_cursor(char);
    void insert_at_cursor(const String&);
    int ruler_width() const;
    Rect ruler_content_rect(int line) const;
    void toggle_selection_if_needed_for_event(const GKeyEvent&);
    void insert_at_cursor_or_replace_selection(const String&);
    void delete_selection();

    Type m_type { MultiLine };

    Vector<OwnPtr<Line>> m_lines;
    GTextPosition m_cursor;
    bool m_cursor_state { true };
    bool m_in_drag_select { false };
    bool m_ruler_visible { true };
    int m_line_spacing { 2 };
    int m_soft_tab_width { 4 };
    GTextRange m_selection;
};
