#include <LibGUI/GFrame.h>
#include <SharedGraphics/StylePainter.h>
#include <LibGUI/GPainter.h>

GFrame::GFrame(GWidget* parent)
    : GWidget(parent)
{
}

GFrame::~GFrame()
{
}

void GFrame::paint_event(GPaintEvent& event)
{
    if (m_shape == FrameShape::NoFrame)
        return;

    GPainter painter(*this);
    painter.add_clip_rect(event.rect());
    StylePainter::paint_frame(painter, rect(), m_shape, m_shadow, m_thickness, spans_entire_window_horizontally());
}
