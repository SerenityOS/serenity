#pragma once

#include <LibGUI/GWidget.h>

class PaintableWidget final : public GWidget {
public:
    explicit PaintableWidget(GWidget* parent);
    virtual ~PaintableWidget() override;

    virtual const char* class_name() const override { return "PaintableWidget"; }

private:
    virtual void paint_event(GPaintEvent&) override;
    virtual void mousedown_event(GMouseEvent&) override;
    virtual void mouseup_event(GMouseEvent&) override;
    virtual void mousemove_event(GMouseEvent&) override;

    RetainPtr<GraphicsBitmap> m_bitmap;
};
