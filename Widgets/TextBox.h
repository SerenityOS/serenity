#pragma once

#include "Widget.h"
#include <functional>

class TextBox final : public Widget {
public:
    explicit TextBox(Widget* parent = nullptr);
    virtual ~TextBox() override;

    String text() const { return m_text; }
    void setText(String&&);

    std::function<void(TextBox&)> onReturnPressed;

private:
    virtual void paintEvent(PaintEvent&) override;
    virtual void mouseDownEvent(MouseEvent&) override;
    virtual void keyDownEvent(KeyEvent&) override;
    virtual void timerEvent(TimerEvent&) override;

    void handleBackspace();

    String m_text;
    unsigned m_cursorPosition { 0 };
    bool m_cursorBlinkState { false };
};

