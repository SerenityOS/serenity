#pragma once

#include <LibGUI/GAbstractView.h>

class GPainter;

// FIXME: Rename this to something without "table cell" in the name.
class GTableCellPaintingDelegate {
public:
    virtual ~GTableCellPaintingDelegate() {}

    virtual void paint(GPainter&, const Rect&, const GModel&, const GModelIndex&) = 0;
};

class GAbstractColumnView : public GAbstractView {
public:
    int item_height() const { return 16; }

    bool alternating_row_colors() const { return m_alternating_row_colors; }
    void set_alternating_row_colors(bool b) { m_alternating_row_colors = b; }

    int header_height() const { return m_headers_visible ? 16 : 0; }

    bool headers_visible() const { return m_headers_visible; }
    void set_headers_visible(bool headers_visible) { m_headers_visible = headers_visible; }

    bool is_column_hidden(int) const;
    void set_column_hidden(int, bool);

    void set_size_columns_to_fit_content(bool b) { m_size_columns_to_fit_content = b; }
    bool size_columns_to_fit_content() const { return m_size_columns_to_fit_content; }

    void set_cell_painting_delegate(int column, OwnPtr<GTableCellPaintingDelegate>&&);

    int horizontal_padding() const { return m_horizontal_padding; }

    Point adjusted_position(const Point&) const;
    GModelIndex index_at_event_position(const Point&) const;

    virtual Rect content_rect(const GModelIndex&) const override;
    Rect content_rect(int row, int column) const;
    Rect row_rect(int item_index) const;

    void scroll_into_view(const GModelIndex&, Orientation);

protected:
    virtual ~GAbstractColumnView() override;
    explicit GAbstractColumnView(GWidget* parent);

    virtual void did_update_model() override;
    virtual void mouseup_event(GMouseEvent&) override;
    virtual void mousedown_event(GMouseEvent&) override;
    virtual void mousemove_event(GMouseEvent&) override;
    virtual void doubleclick_event(GMouseEvent&) override;
    virtual void keydown_event(GKeyEvent&) override;
    virtual void leave_event(CEvent&) override;
    virtual void context_menu_event(GContextMenuEvent&) override;

    void paint_headers(GPainter&);
    Rect header_rect(int column) const;

    static const Font& header_font();
    void update_headers();
    void set_hovered_header_index(int);

    struct ColumnData {
        int width { 0 };
        bool has_initialized_width { false };
        bool visibility { true };
        RefPtr<GAction> visibility_action;
        OwnPtr<GTableCellPaintingDelegate> cell_painting_delegate;
    };
    ColumnData& column_data(int column) const;

    mutable Vector<ColumnData> m_column_data;

    GMenu& ensure_header_context_menu();
    RefPtr<GMenu> m_header_context_menu;

    Rect column_resize_grabbable_rect(int) const;
    int column_width(int) const;
    void update_content_size();
    void update_column_sizes();
    int item_count() const;

private:
    bool m_headers_visible { true };
    bool m_size_columns_to_fit_content { false };
    bool m_in_column_resize { false };
    bool m_alternating_row_colors { true };
    int m_horizontal_padding { 5 };
    Point m_column_resize_origin;
    int m_column_resize_original_width { 0 };
    int m_resizing_column { -1 };
    int m_pressed_column_header_index { -1 };
    bool m_pressed_column_header_is_pressed { false };
    int m_hovered_column_header_index { -1 };
};
