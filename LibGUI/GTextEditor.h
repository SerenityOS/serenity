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

private:
    int m_line { -1 };
    int m_column { -1 };
};

class GTextEditor : public GWidget {
public:
    explicit GTextEditor(GWidget* parent);
    virtual ~GTextEditor() override;

    void set_text(const String&);

    int content_width() const;

    Rect visible_content_rect() const;
    void scroll_into_view(const GTextPosition&, Orientation);

    int line_count() const { return m_lines.size(); }

    int padding() const { return 2; }

private:
    virtual void paint_event(GPaintEvent&) override;
    virtual void resize_event(GResizeEvent&) override;
    virtual void mousedown_event(GMouseEvent&) override;
    virtual void keydown_event(GKeyEvent&) override;
    virtual bool accepts_focus() const override { return true; }

    void update_scrollbar_ranges();
    Rect line_content_rect(int item_index) const;
    Rect cursor_content_rect() const;
    void update_cursor();

    GScrollBar* m_vertical_scrollbar { nullptr };
    GScrollBar* m_horizontal_scrollbar { nullptr };

    class Line {
    public:
        Line() { }

        String text() const { return m_text; }
        int length() const { return m_text.length(); }
        int width(const Font&) const;
        void set_text(const String&);

    private:
        String m_text;
        mutable int m_cached_width { -1 };
    };
    Vector<Line> m_lines;
    GTextPosition m_cursor;
};
