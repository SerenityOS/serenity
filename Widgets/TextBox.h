#pragma once

#include "Widget.h"

class TextBox final : public Widget {
public:
    explicit TextBox(Widget* parent = nullptr);
    virtual ~TextBox() override;

    String text() const { return m_text; }
    void setText(String&&);

private:
    virtual void onPaint(PaintEvent&) override;
    virtual void onMouseDown(MouseEvent&) override;
    virtual void onKeyDown(KeyEvent&) override;
    virtual void onTimer(TimerEvent&) override;

    void handleBackspace();

    String m_text;
    unsigned m_cursorPosition { 0 };
    bool m_cursorBlinkState { false };
};

