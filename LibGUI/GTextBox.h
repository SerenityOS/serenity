#pragma once

#include "GWidget.h"
#include <AK/Function.h>

class GTextBox final : public GWidget {
public:
    explicit GTextBox(GWidget* parent);
    virtual ~GTextBox() override;

    String text() const { return m_text; }
    void set_text(const String&);

    Function<void(GTextBox&)> on_return_pressed;
    Function<void(GTextBox&)> on_change;

    virtual const char* class_name() const override { return "GTextBox"; }

private:
    virtual void paint_event(GPaintEvent&) override;
    virtual void mousedown_event(GMouseEvent&) override;
    virtual void keydown_event(GKeyEvent&) override;
    virtual void timer_event(GTimerEvent&) override;
    virtual void focusin_event(GEvent&) override;
    virtual void focusout_event(GEvent&) override;
    virtual void resize_event(GResizeEvent&) override;
    virtual bool accepts_focus() const override { return true; }

    Point cursor_content_position() const;
    Rect visible_content_rect() const;
    void handle_backspace();
    void scroll_cursor_into_view(HorizontalDirection);

    String m_text;
    int m_cursor_position { 0 };
    int m_scroll_offset { 0 };
    bool m_cursor_blink_state { false };
};

