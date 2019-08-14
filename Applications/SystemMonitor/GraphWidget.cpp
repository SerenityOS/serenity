#include "GraphWidget.h"
#include <LibGUI/GPainter.h>

GraphWidget::GraphWidget(GWidget* parent)
    : GFrame(parent)
{
    set_frame_thickness(2);
    set_frame_shape(FrameShape::Container);
    set_frame_shadow(FrameShadow::Sunken);
}

GraphWidget::~GraphWidget()
{
}

void GraphWidget::add_value(int value)
{
    m_values.enqueue(value);
    update();
}

void GraphWidget::paint_event(GPaintEvent& event)
{
    GFrame::paint_event(event);
    GPainter painter(*this);
    painter.add_clip_rect(event.rect());
    painter.add_clip_rect(frame_inner_rect());
    painter.fill_rect(event.rect(), Color::Black);

    auto inner_rect = frame_inner_rect();
    float scale = (float)inner_rect.height() / (float)m_max;

    Point prev_point;
    for (int i = 0; i < m_values.size(); ++i) {
        int x = inner_rect.right() - (i * 2) + 1;
        if (x < 0)
            break;
        float scaled_value = (float)m_values.at(m_values.size() - i - 1) * scale;
        Point point = { x, inner_rect.bottom() - (int)scaled_value };
        if (i != 0)
            painter.draw_line(prev_point, point, m_graph_color);
        prev_point = point;
    }

    if (!m_values.is_empty() && text_formatter) {
        Rect text_rect = inner_rect.shrunken(8, 8);
        text_rect.set_height(font().glyph_height());
        auto text = text_formatter(m_values.last(), m_max);
        painter.draw_text(text_rect.translated(1, 1), text.characters(), TextAlignment::CenterRight, Color::Black);
        painter.draw_text(text_rect, text.characters(), TextAlignment::CenterRight, m_text_color);
    }
}
