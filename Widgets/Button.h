#pragma once

#include "Widget.h"
#include <AK/String.h>

class Button final : public Widget {
public:
    explicit Button(Widget* parent);
    virtual ~Button() override;

    String caption() const { return m_caption; }
    void setCaption(String&&);

private:
    virtual void onPaint(PaintEvent&) override;
    virtual void onMouseDown(MouseEvent&) override;
    virtual void onMouseUp(MouseEvent&) override;

    virtual const char* className() const override { return "Button"; }

    String m_caption;
    bool m_beingPressed { false };
};

