#pragma once

#include "Widget.h"

class Label final : public Widget {
public:
    explicit Label(Widget* parent);
    virtual ~Label() override;

private:
    virtual void onPaint(PaintEvent&) override;
    virtual void onMouseMove(MouseEvent&) override;

    virtual const char* className() const override { return "Label"; }
};

