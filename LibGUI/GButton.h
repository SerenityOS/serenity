#pragma once

#include <LibGUI/GWidget.h>
#include <LibGUI/GStyle.h>
#include <AK/AKString.h>
#include <AK/Function.h>
#include <SharedGraphics/GraphicsBitmap.h>

class GButton : public GWidget {
public:
    explicit GButton(GWidget* parent);
    virtual ~GButton() override;

    String caption() const { return m_caption; }
    void set_caption(String&&);

    void set_icon(RetainPtr<GraphicsBitmap>&& icon) { m_icon = move(icon); }
    const GraphicsBitmap* icon() const { return m_icon.ptr(); }
    GraphicsBitmap* icon() { return m_icon.ptr(); }

    Function<void(GButton&)> on_click;

    void set_button_style(GButtonStyle style) { m_button_style = style; }
    GButtonStyle button_style() const { return m_button_style; }

private:
    virtual void paint_event(GPaintEvent&) override;
    virtual void mousedown_event(GMouseEvent&) override;
    virtual void mouseup_event(GMouseEvent&) override;
    virtual void mousemove_event(GMouseEvent&) override;
    virtual void enter_event(GEvent&) override;
    virtual void leave_event(GEvent&) override;

    virtual const char* class_name() const override { return "GButton"; }

    String m_caption;
    RetainPtr<GraphicsBitmap> m_icon;
    GButtonStyle m_button_style { GButtonStyle::Normal };
    bool m_being_pressed { false };
    bool m_tracking_cursor { false };
    bool m_hovered { false };
};

