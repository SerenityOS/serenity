#pragma once

#include <LibGUI/GFrame.h>

class GScrollBar;

class GScrollableWidget : public GFrame {
    C_OBJECT(GScrollableWidget)
public:
    virtual ~GScrollableWidget() override;

    Size content_size() const { return m_content_size; }
    int content_width() const { return m_content_size.width(); }
    int content_height() const { return m_content_size.height(); }

    Rect visible_content_rect() const;

    Rect widget_inner_rect() const;

    void scroll_into_view(const Rect&, Orientation);
    void scroll_into_view(const Rect&, bool scroll_horizontally, bool scroll_vertically);

    void set_scrollbars_enabled(bool);
    bool is_scrollbars_enabled() const { return m_scrollbars_enabled; }

    Size available_size() const;

    GScrollBar& vertical_scrollbar() { return *m_vertical_scrollbar; }
    const GScrollBar& vertical_scrollbar() const { return *m_vertical_scrollbar; }
    GScrollBar& horizontal_scrollbar() { return *m_horizontal_scrollbar; }
    const GScrollBar& horizontal_scrollbar() const { return *m_horizontal_scrollbar; }
    GWidget& corner_widget() { return *m_corner_widget; }
    const GWidget& corner_widget() const { return *m_corner_widget; }

    void scroll_to_top();
    void scroll_to_bottom();

    int width_occupied_by_vertical_scrollbar() const;
    int height_occupied_by_horizontal_scrollbar() const;

protected:
    explicit GScrollableWidget(GWidget* parent);
    virtual void resize_event(GResizeEvent&) override;
    virtual void mousewheel_event(GMouseEvent&) override;
    virtual void did_scroll() {}
    void set_content_size(const Size&);
    void set_size_occupied_by_fixed_elements(const Size&);


private:
    void update_scrollbar_ranges();

    GScrollBar* m_vertical_scrollbar { nullptr };
    GScrollBar* m_horizontal_scrollbar { nullptr };
    GWidget* m_corner_widget { nullptr };
    Size m_content_size;
    Size m_size_occupied_by_fixed_elements;
    bool m_scrollbars_enabled { true };
};
