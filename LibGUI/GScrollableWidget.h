#pragma once

#include <LibGUI/GWidget.h>

class GScrollBar;

class GScrollableWidget : public GWidget {
public:
    virtual ~GScrollableWidget() override;

    virtual const char* class_name() const override { return "GScrollableWidget"; }

    Size content_size() const { return m_content_size; }
    int content_width() const { return m_content_size.width(); }
    int content_height() const { return m_content_size.height(); }

    Size padding() const { return m_padding; }

    Rect visible_content_rect() const;

    void scroll_into_view(const Rect&, Orientation);
    void scroll_into_view(const Rect&, bool scroll_horizontally, bool scroll_vertically);

    GScrollBar& vertical_scrollbar() { return *m_vertical_scrollbar; }
    const GScrollBar& vertical_scrollbar() const { return *m_vertical_scrollbar; }
    GScrollBar& horizontal_scrollbar() { return *m_horizontal_scrollbar; }
    const GScrollBar& horizontal_scrollbar() const { return *m_horizontal_scrollbar; }
    GWidget& corner_widget() { return *m_corner_widget; }
    const GWidget& corner_widget() const { return *m_corner_widget; }

    void set_scrollbars_enabled(bool);
    bool is_scrollbars_enabled() const { return m_scrollbars_enabled; }

protected:
    explicit GScrollableWidget(GWidget* parent);
    virtual void resize_event(GResizeEvent&) override;
    void set_content_size(const Size&);
    void set_size_occupied_by_fixed_elements(const Size&);
    void set_padding(const Size&);

    int width_occupied_by_vertical_scrollbar() const;
    int height_occupied_by_horizontal_scrollbar() const;

private:
    void update_scrollbar_ranges();

    GScrollBar* m_vertical_scrollbar { nullptr };
    GScrollBar* m_horizontal_scrollbar { nullptr };
    GWidget* m_corner_widget { nullptr };
    Size m_content_size;
    Size m_size_occupied_by_fixed_elements;
    Size m_padding;
    bool m_scrollbars_enabled { true };
};
