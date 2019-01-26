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

private:
    virtual const char* class_name() const override { return "GTextBox"; }
    virtual void paint_event(GPaintEvent&) override;
    virtual void mousedown_event(GMouseEvent&) override;
    virtual void keydown_event(GKeyEvent&) override;
    virtual void timerEvent(GTimerEvent&) override;
    virtual bool accepts_focus() const override { return true; }

    void handle_backspace();

    String m_text;
    unsigned m_cursorPosition { 0 };
    bool m_cursorBlinkState { false };
};

