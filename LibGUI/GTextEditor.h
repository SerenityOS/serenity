#pragma once

#include <LibGUI/GWidget.h>
#include <AK/Function.h>
#include <AK/HashMap.h>

class GScrollBar;

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

private:
    int m_line { -1 };
    int m_column { -1 };
};

class GTextEditor : public GWidget {
public:
    explicit GTextEditor(GWidget* parent);
    virtual ~GTextEditor() override;

    Function<void(GTextEditor&)> on_cursor_change;

    void set_text(const String&);
    int content_width() const;
    int content_height() const;
    Rect visible_content_rect() const;
    void scroll_cursor_into_view();
    int line_count() const { return m_lines.size(); }
    int line_spacing() const { return m_line_spacing; }
    int line_height() const { return font().glyph_height() + m_line_spacing; }
    int padding() const { return 3; }
    GTextPosition cursor() const { return m_cursor; }
    int glyph_width() const { return font().glyph_width('x'); }

private:
    virtual void paint_event(GPaintEvent&) override;
    virtual void resize_event(GResizeEvent&) override;
    virtual void mousedown_event(GMouseEvent&) override;
    virtual void keydown_event(GKeyEvent&) override;
    virtual void focusin_event(GEvent&) override;
    virtual void focusout_event(GEvent&) override;
    virtual void timer_event(GTimerEvent&) override;
    virtual bool accepts_focus() const override { return true; }

    class Line {
    public:
        Line();

        const char* characters() const { return m_text.data(); }
        int length() const { return m_text.size() - 1; }
        int width(const Font&) const;
        void set_text(const String&);

    private:
        // NOTE: This vector is null terminated.
        Vector<char> m_text;
    };

    void update_scrollbar_ranges();
    Rect line_content_rect(int item_index) const;
    Rect line_widget_rect(int line_index) const;
    Rect cursor_content_rect() const;
    void update_cursor();
    void set_cursor(int line, int column);
    void set_cursor(const GTextPosition&);
    Line& current_line() { return m_lines[m_cursor.line()]; }
    const Line& current_line() const { return m_lines[m_cursor.line()]; }
    GTextPosition text_position_at(const Point&) const;
    void insert_at_cursor(char);

    GScrollBar* m_vertical_scrollbar { nullptr };
    GScrollBar* m_horizontal_scrollbar { nullptr };

    Vector<Line> m_lines;
    GTextPosition m_cursor;
    bool m_cursor_state { true };
    int m_line_spacing { 2 };
};
