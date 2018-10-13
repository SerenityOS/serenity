#pragma once

#include "Widget.h"
#include <AK/String.h>

class Label final : public Widget {
public:
    explicit Label(Widget* parent);
    virtual ~Label() override;

    String text() const { return m_text; }
    void setText(String&&);

private:
    virtual void paintEvent(PaintEvent&) override;
    virtual void mouseMoveEvent(MouseEvent&) override;

    virtual const char* className() const override { return "Label"; }

    String m_text;
};

