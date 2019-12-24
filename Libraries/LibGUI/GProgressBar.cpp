#include <AK/StringBuilder.h>
#include <LibGUI/GPainter.h>
#include <LibGUI/GProgressBar.h>

GProgressBar::GProgressBar(GWidget* parent)
    : GFrame(parent)
{
    set_frame_shape(FrameShape::Container);
    set_frame_shadow(FrameShadow::Sunken);
    set_frame_thickness(2);
}

GProgressBar::~GProgressBar()
{
}

void GProgressBar::set_value(int value)
{
    if (m_value == value)
        return;
    m_value = value;
    update();
}

void GProgressBar::set_range(int min, int max)
{
    ASSERT(min < max);
    m_min = min;
    m_max = max;
    if (m_value > m_max)
        m_value = m_max;
    if (m_value < m_min)
        m_value = m_min;
}

void GProgressBar::paint_event(GPaintEvent& event)
{
    GFrame::paint_event(event);

    GPainter painter(*this);
    auto rect = frame_inner_rect();
    painter.add_clip_rect(rect);
    painter.add_clip_rect(event.rect());

    String progress_text;
    if (m_format != Format::NoText) {
        // Then we draw the progress text over the gradient.
        // We draw it twice, once offset (1, 1) for a drop shadow look.
        StringBuilder builder;
        builder.append(m_caption);
        if (m_format == Format::Percentage) {
            float range_size = m_max - m_min;
            float progress = (m_value - m_min) / range_size;
            builder.appendf("%d%%", (int)(progress * 100));
        } else if (m_format == Format::ValueSlashMax) {
            builder.appendf("%d/%d", m_value, m_max);
        }
        progress_text = builder.to_string();
    }

    StylePainter::paint_progress_bar(painter, rect, palette(), m_min, m_max, m_value, progress_text);
}
