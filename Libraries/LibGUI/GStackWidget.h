#pragma once

#include <LibGUI/GWidget.h>

class GStackWidget : public GWidget {
    C_OBJECT(GStackWidget)
public:
    virtual ~GStackWidget() override;

    GWidget* active_widget() { return m_active_widget.ptr(); }
    const GWidget* active_widget() const { return m_active_widget.ptr(); }
    void set_active_widget(GWidget*);

    Function<void(GWidget*)> on_active_widget_change;

protected:
    explicit GStackWidget(GWidget* parent);
    virtual void child_event(CChildEvent&) override;
    virtual void resize_event(GResizeEvent&) override;

private:
    RefPtr<GWidget> m_active_widget;
};
