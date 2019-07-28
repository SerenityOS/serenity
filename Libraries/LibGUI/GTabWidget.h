#pragma once

#include <LibGUI/GWidget.h>

class GTabWidget : public GWidget {
    C_OBJECT(GTabWidget)
public:
    enum TabPosition {
        Top,
        Bottom,
    };

    explicit GTabWidget(GWidget* parent);
    virtual ~GTabWidget() override;

    TabPosition tab_position() const { return m_tab_position; }
    void set_tab_position(TabPosition);

    GWidget* active_widget() const { return m_active_widget; }
    void set_active_widget(GWidget*);

    int bar_height() const { return 21; }
    int container_padding() const { return 2; }

    void add_widget(const StringView&, GWidget*);

protected:
    virtual void paint_event(GPaintEvent&) override;
    virtual void child_event(CChildEvent&) override;
    virtual void resize_event(GResizeEvent&) override;
    virtual void mousedown_event(GMouseEvent&) override;
    virtual void mousemove_event(GMouseEvent&) override;
    virtual void leave_event(CEvent&) override;

private:
    Rect child_rect_for_size(const Size&) const;
    Rect button_rect(int index) const;
    Rect bar_rect() const;
    Rect container_rect() const;
    void update_bar();

    GWidget* m_active_widget { nullptr };

    struct TabData {
        Rect rect(const Font&) const;
        int width(const Font&) const;
        String title;
        GWidget* widget { nullptr };
    };
    Vector<TabData> m_tabs;
    TabPosition m_tab_position { TabPosition::Top };
    int m_hovered_tab_index { -1 };
};
