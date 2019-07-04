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

    // First we fill the entire widget with the gradient. This incurs a bit of
    // overdraw but ensures a consistent look throughout the progression.
    Color start_color(110, 34, 9);
    Color end_color(244, 202, 158);
    painter.fill_rect_with_gradient(rect, start_color, end_color);

    float range_size = m_max - m_min;
    float progress = (m_value - m_min) / range_size;

    String progress_text;
    if (m_format != Format::NoText) {
        // Then we draw the progress text over the gradient.
        // We draw it twice, once offset (1, 1) for a drop shadow look.
        StringBuilder builder;
        builder.append(m_caption);
        if (m_format == Format::Percentage)
            builder.appendf("%d%%", (int)(progress * 100));
        else if (m_format == Format::ValueSlashMax)
            builder.appendf("%d/%d", m_value, m_max);

        progress_text = builder.to_string();

        painter.draw_text(rect.translated(1, 1), progress_text, TextAlignment::Center, Color::Black);
        painter.draw_text(rect, progress_text, TextAlignment::Center, Color::White);
    }

    // Then we carve out a hole in the remaining part of the widget.
    // We draw the text a third time, clipped and inverse, for sharp contrast.
    float progress_width = progress * width();
    Rect hole_rect { (int)progress_width, 0, (int)(width() - progress_width), height() };
    painter.add_clip_rect(hole_rect);
    painter.fill_rect(hole_rect, Color::White);

    if (m_format != Format::NoText)
        painter.draw_text(rect.translated(0, 0), progress_text, TextAlignment::Center, Color::Black);
}
