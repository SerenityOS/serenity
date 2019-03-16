#pragma once

#include <LibGUI/GWidget.h>

class GScrollBar;

class GScrollableWidget : public GWidget {
public:
    virtual ~GScrollableWidget() override;

    virtual const char* class_name() const override { return "GScrollableWidget"; }

    Size content_size() const { return m_content_size; }

    Rect visible_content_rect() const;

    void scroll_into_view(const Rect&, Orientation);

    GScrollBar& vertical_scrollbar() { return *m_vertical_scrollbar; }
    const GScrollBar& vertical_scrollbar() const { return *m_vertical_scrollbar; }
    GScrollBar& horizontal_scrollbar() { return *m_horizontal_scrollbar; }
    const GScrollBar& horizontal_scrollbar() const { return *m_horizontal_scrollbar; }

protected:
    explicit GScrollableWidget(GWidget* parent);
    virtual void resize_event(GResizeEvent&) override;
    void set_content_size(const Size&);
    void set_size_occupied_by_fixed_elements(const Size&);

private:
    void update_scrollbar_ranges();

    GScrollBar* m_vertical_scrollbar { nullptr };
    GScrollBar* m_horizontal_scrollbar { nullptr };
    GWidget* m_corner_widget { nullptr };
    Size m_content_size;
    Size m_size_occupied_by_fixed_elements;
};
