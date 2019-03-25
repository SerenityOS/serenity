#pragma once

#include <LibGUI/GModel.h>
#include <LibGUI/GAbstractView.h>
#include <AK/Function.h>
#include <AK/HashMap.h>

class GScrollBar;
class Painter;

class GTableView : public GAbstractView {
public:
    explicit GTableView(GWidget* parent);
    virtual ~GTableView() override;

    int header_height() const { return m_headers_visible ? 16 : 0; }
    int item_height() const { return 16; }

    bool headers_visible() const { return m_headers_visible; }
    void set_headers_visible(bool headers_visible) { m_headers_visible = headers_visible; }

    bool alternating_row_colors() const { return m_alternating_row_colors; }
    void set_alternating_row_colors(bool b) { m_alternating_row_colors = b; }

    int content_width() const;
    int horizontal_padding() const { return m_horizontal_padding; }

    void scroll_into_view(const GModelIndex&, Orientation);

    bool is_column_hidden(int) const;
    void set_column_hidden(int, bool);

    virtual const char* class_name() const override { return "GTableView"; }

private:
    virtual void did_update_model() override;
    virtual void paint_event(GPaintEvent&) override;
    virtual void mousedown_event(GMouseEvent&) override;
    virtual void doubleclick_event(GMouseEvent&) override;
    virtual void keydown_event(GKeyEvent&) override;

    void paint_headers(Painter&);
    int item_count() const;
    Rect row_rect(int item_index) const;
    Rect header_rect(int) const;
    int column_width(int) const;
    void update_content_size();

    Vector<bool> m_column_visibility;
    int m_horizontal_padding { 5 };
    bool m_headers_visible { true };
    bool m_alternating_row_colors { true };
};
