#include <LibGUI/GProgressBar.h>
#include <SharedGraphics/Painter.h>

GProgressBar::GProgressBar(GWidget* parent)
    : GWidget(parent)
{
    start_timer(10);
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
    Painter painter(*this);
    painter.set_clip_rect(event.rect());

    // First we fill the entire widget with the gradient. This incurs a bit of
    // overdraw but ensures a consistent look throughout the progression.
    Color start_color(110, 34, 9);
    Color end_color(244, 202, 158);
    painter.fill_rect_with_gradient(rect(), start_color, end_color);

    float range_size = m_max - m_min;
    float progress = (m_value - m_min) / range_size;

    // Then we draw the progress text over the gradient.
    // We draw it twice, once offset (1, 1) for a drop shadow look.
    auto progress_text = String::format("%d%%", (int)(progress * 100));
    painter.draw_text(rect().translated(1, 1), progress_text, TextAlignment::Center, Color::Black);
    painter.draw_text(rect(), progress_text, TextAlignment::Center, Color::White);

    // Then we carve out a hole in the remaining part of the widget.
    // We draw the text a third time, clipped and inverse, for sharp contrast.
    painter.save();
    float progress_width = progress * width();
    Rect hole_rect { (int)progress_width, 0, (int)(width() - progress_width), height() };
    painter.set_clip_rect(hole_rect);
    painter.fill_rect(hole_rect, Color::White);
    painter.draw_text(rect().translated(0, 0), progress_text, TextAlignment::Center, Color::Black);
    painter.restore();

    // Finally, draw a frame around the widget.
    painter.draw_rect(rect(), Color::Black);
}
