#pragma once

#include "Widget.h"
#include <AK/AKString.h>

class Button final : public Widget {
public:
    explicit Button(Widget* parent);
    virtual ~Button() override;

    String caption() const { return m_caption; }
    void setCaption(String&&);

    std::function<void(Button&)> onClick;

private:
    virtual void paintEvent(PaintEvent&) override;
    virtual void mouseDownEvent(MouseEvent&) override;
    virtual void mouseUpEvent(MouseEvent&) override;

    virtual const char* class_name() const override { return "Button"; }

    String m_caption;
    bool m_beingPressed { false };
};

