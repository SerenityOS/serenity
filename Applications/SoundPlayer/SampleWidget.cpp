#include "SampleWidget.h"
#include <LibAudio/ABuffer.h>
#include <LibGUI/GPainter.h>

SampleWidget::SampleWidget(GWidget* parent)
    : GFrame(parent)
{
    set_frame_shape(FrameShape::Container);
    set_frame_shadow(FrameShadow::Sunken);
    set_frame_thickness(2);
}

SampleWidget::~SampleWidget()
{
}

void SampleWidget::paint_event(GPaintEvent& event)
{
    GFrame::paint_event(event);
    GPainter painter(*this);
    painter.add_clip_rect(event.rect());

    painter.fill_rect(frame_inner_rect(), Color::Black);

    if (!m_buffer)
        return;

    // FIXME: Right now we only display as many samples from the buffer as we can fit
    //        in the frame_inner_rect(). Maybe scale the samples or something?
    int samples_to_draw = min(m_buffer->sample_count(), frame_inner_rect().width());
    for (int x = 0; x < samples_to_draw; ++x) {
        // FIXME: This might look nicer if drawn as lines.
        auto& sample = m_buffer->samples()[x];
        Point p = { x, frame_inner_rect().center().y() + (int)(sample.left * frame_inner_rect().height()) };
        painter.set_pixel(p, Color::Green);
    }
}

void SampleWidget::set_buffer(ABuffer* buffer)
{
    if (m_buffer == buffer)
        return;
    m_buffer = buffer;
    update();
}
