#pragma once

#include "Widget.h"

class RootWidget final : public Widget {
public:
    RootWidget();
    virtual ~RootWidget() override;

private:
    virtual void onPaint(PaintEvent&) override;
    virtual void onMouseMove(MouseEvent&) override;
};
