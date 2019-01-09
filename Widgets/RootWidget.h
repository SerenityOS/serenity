#pragma once

#include "Widget.h"

class GraphicsBitmap;

class RootWidget final : public Widget {
public:
    RootWidget();
    virtual ~RootWidget() override;

private:
    virtual void paintEvent(PaintEvent&) override;
    virtual void mouseMoveEvent(MouseEvent&) override;

    virtual GraphicsBitmap* backing() override { return m_backing.ptr(); }

    RetainPtr<GraphicsBitmap> m_backing;
};
