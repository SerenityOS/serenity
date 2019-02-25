#pragma once

#include "GWidget.h"
#include <AK/Function.h>

class GTextBox final : public GWidget {
public:
    explicit GTextBox(GWidget* parent);
    virtual ~GTextBox() override;

    String text() const { return m_text; }
    void set_text(String&&);

    Function<void(GTextBox&)> on_return_pressed;
    Function<void(GTextBox&)> on_change;

private:
    virtual const char* class_name() const override { return "GTextBox"; }
    virtual void paint_event(GPaintEvent&) override;
    virtual void mousedown_event(GMouseEvent&) override;
    virtual void keydown_event(GKeyEvent&) override;
    virtual void timer_event(GTimerEvent&) override;
    virtual void focusin_event(GEvent&) override;
    virtual void focusout_event(GEvent&) override;
    virtual bool accepts_focus() const override { return true; }

    void handle_backspace();

    String m_text;
    int m_cursor_position { 0 };
    bool m_cursor_blink_state { false };
};

