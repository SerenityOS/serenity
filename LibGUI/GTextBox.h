#pragma once

#include "GWidget.h"
#include <AK/Function.h>

class GTextBox final : public GWidget {
public:
    explicit GTextBox(GWidget* parent);
    virtual ~GTextBox() override;

    String text() const { return m_text; }
    void setText(String&&);

    Function<void(GTextBox&)> onReturnPressed;

private:
    virtual const char* class_name() const override { return "GTextBox"; }
    virtual void paintEvent(GPaintEvent&) override;
    virtual void mouseDownEvent(GMouseEvent&) override;
    virtual void keyDownEvent(GKeyEvent&) override;
    virtual void timerEvent(GTimerEvent&) override;

    void handleBackspace();

    String m_text;
    unsigned m_cursorPosition { 0 };
    bool m_cursorBlinkState { false };
};

