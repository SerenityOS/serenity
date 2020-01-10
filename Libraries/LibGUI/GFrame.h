#pragma once

#include <LibDraw/StylePainter.h>
#include <LibGUI/GWidget.h>

class GFrame : public GWidget {
    C_OBJECT(GFrame)
public:
    virtual ~GFrame() override;

    int frame_thickness() const { return m_thickness; }
    void set_frame_thickness(int thickness) { m_thickness = thickness; }

    FrameShadow frame_shadow() const { return m_shadow; }
    void set_frame_shadow(FrameShadow shadow) { m_shadow = shadow; }

    FrameShape frame_shape() const { return m_shape; }
    void set_frame_shape(FrameShape shape) { m_shape = shape; }

    Rect frame_inner_rect_for_size(const Size& size) const { return { m_thickness, m_thickness, size.width() - m_thickness * 2, size.height() - m_thickness * 2 }; }
    Rect frame_inner_rect() const { return frame_inner_rect_for_size(size()); }

protected:
    explicit GFrame(GWidget* parent = nullptr);
    void paint_event(GPaintEvent&) override;

private:
    int m_thickness { 0 };
    FrameShadow m_shadow { FrameShadow::Plain };
    FrameShape m_shape { FrameShape::NoFrame };
};
