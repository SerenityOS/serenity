#pragma once

#include <LibGUI/GWidget.h>

class GFrame : public GWidget {
public:
    explicit GFrame(GWidget* parent);
    virtual ~GFrame() override;

    enum Shadow { Plain, Raised, Sunken };
    enum Shape { NoFrame, Container, Panel, VerticalLine, HorizontalLine };

    int frame_thickness() const { return m_thickness; }
    void set_frame_thickness(int thickness) { m_thickness = thickness; }

    Shadow frame_shadow() const { return m_shadow; }
    void set_frame_shadow(Shadow shadow) { m_shadow = shadow; }

    Shape frame_shape() const { return m_shape; }
    void set_frame_shape(Shape shape) { m_shape = shape; }

    Rect frame_inner_rect_for_size(const Size& size) const { return { m_thickness, m_thickness, size.width() - m_thickness * 2, size.height() - m_thickness * 2 }; }
    Rect frame_inner_rect() const { return frame_inner_rect_for_size(size()); }

    virtual const char* class_name() const override { return "GFrame"; }

protected:
    void paint_event(GPaintEvent&) override;

private:
    int m_thickness { 0 };
    Shadow m_shadow { Plain };
    Shape m_shape { NoFrame };
};
