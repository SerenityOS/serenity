#pragma once

#include <AK/Function.h>
#include <AK/HashMap.h>
#include <LibGUI/GAbstractView.h>
#include <LibGUI/GModel.h>

class GPainter;
class GScrollBar;
class Painter;

class GTableCellPaintingDelegate {
public:
    virtual ~GTableCellPaintingDelegate() {}

    virtual void paint(GPainter&, const Rect&, const GModel&, const GModelIndex&) = 0;
};

class GTableView : public GAbstractView {
    C_OBJECT(GTableView)
public:
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

    void set_size_columns_to_fit_content(bool b) { m_size_columns_to_fit_content = b; }
    bool size_columns_to_fit_content() const { return m_size_columns_to_fit_content; }

    Point adjusted_position(const Point&) const;

    virtual Rect content_rect(const GModelIndex&) const override;

    void set_cell_painting_delegate(int column, OwnPtr<GTableCellPaintingDelegate>&&);

protected:
    explicit GTableView(GWidget* parent);

    virtual void did_update_model() override;
    virtual void paint_event(GPaintEvent&) override;
    virtual void mousedown_event(GMouseEvent&) override;
    virtual void mousemove_event(GMouseEvent&) override;
    virtual void mouseup_event(GMouseEvent&) override;
    virtual void doubleclick_event(GMouseEvent&) override;
    virtual void keydown_event(GKeyEvent&) override;
    virtual void leave_event(CEvent&) override;
    virtual void context_menu_event(GContextMenuEvent&) override;

    GModelIndex index_at_event_position(const Point&) const;

    Rect content_rect(int row, int column) const;
    void paint_headers(Painter&);
    int item_count() const;
    Rect row_rect(int item_index) const;
    Rect header_rect(int) const;
    Rect column_resize_grabbable_rect(int) const;
    int column_width(int) const;
    void update_content_size();
    void update_column_sizes();
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
    int m_horizontal_padding { 5 };
    bool m_headers_visible { true };
    bool m_alternating_row_colors { true };
    bool m_size_columns_to_fit_content { false };
    bool m_in_column_resize { false };
    Point m_column_resize_origin;
    int m_column_resize_original_width { 0 };
    int m_resizing_column { -1 };
    int m_pressed_column_header_index { -1 };
    bool m_pressed_column_header_is_pressed { false };
    int m_hovered_column_header_index { -1 };

    GMenu& ensure_header_context_menu();
    OwnPtr<GMenu> m_header_context_menu;
};
