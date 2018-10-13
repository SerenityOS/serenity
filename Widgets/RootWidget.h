#pragma once

#include "Widget.h"

class RootWidget final : public Widget {
public:
    RootWidget();
    virtual ~RootWidget() override;

private:
    virtual void paintEvent(PaintEvent&) override;
    virtual void mouseMoveEvent(MouseEvent&) override;
};
