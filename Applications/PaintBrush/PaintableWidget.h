#pragma once

#include <LibGUI/GWidget.h>

class PaintableWidget final : public GWidget {
public:
    explicit PaintableWidget(GWidget* parent);
    virtual ~PaintableWidget() override;

    virtual const char* class_name() const override { return "PaintableWidget"; }

    Color primary_color() const { return m_primary_color; }
    Color secondary_color() const { return m_secondary_color; }

    void set_primary_color(Color color) { m_primary_color = color; }
    void set_secondary_color(Color color) { m_secondary_color = color; }

private:
    virtual void paint_event(GPaintEvent&) override;
    virtual void mousedown_event(GMouseEvent&) override;
    virtual void mouseup_event(GMouseEvent&) override;
    virtual void mousemove_event(GMouseEvent&) override;

    Color color_for(const GMouseEvent&);

    Point m_last_drawing_event_position { -1, -1 };
    RetainPtr<GraphicsBitmap> m_bitmap;

    Color m_primary_color { Color::Black };
    Color m_secondary_color { Color::White };
};
