#pragma once

#include <LibGUI/GWidget.h>

class GTabWidget : public GWidget {
public:
    explicit GTabWidget(GWidget* parent);
    virtual ~GTabWidget() override;

    GWidget* active_widget() const { return m_active_widget; }
    void set_active_widget(GWidget*);

    int bar_height() const { return 22; }

    void add_widget(const String&, GWidget*);

    virtual const char* class_name() const override { return "GTabWidget"; }

protected:
    virtual void paint_event(GPaintEvent&) override;
    virtual void child_event(CChildEvent&) override;
    virtual void resize_event(GResizeEvent&) override;
    virtual void mousedown_event(GMouseEvent&) override;

private:
    Rect child_rect_for_size(const Size&) const;
    Rect button_rect(int index) const;
    Rect bar_rect() const;

    GWidget* m_active_widget { nullptr };

    struct TabData {
        Rect rect(const Font&) const;
        int width(const Font&) const;
        String title;
        GWidget* widget { nullptr };
    };
    Vector<TabData> m_tabs;
};
