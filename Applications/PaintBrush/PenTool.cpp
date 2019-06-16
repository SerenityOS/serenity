#include "PenTool.h"
#include "PaintableWidget.h"
#include <LibGUI/GPainter.h>

PenTool::PenTool()
{
}

PenTool::~PenTool()
{
}

void PenTool::on_mousedown(GMouseEvent& event)
{
    if (event.button() != GMouseButton::Left && event.button() != GMouseButton::Right)
        return;

    GPainter painter(m_widget->bitmap());
    painter.set_pixel(event.position(), m_widget->color_for(event));
    m_widget->update({ event.position(), { 1, 1 } });
    m_last_drawing_event_position = event.position();
}

void PenTool::on_mouseup(GMouseEvent& event)
{
    if (event.button() == GMouseButton::Left || event.button() == GMouseButton::Right)
        m_last_drawing_event_position = { -1, -1 };
}

void PenTool::on_mousemove(GMouseEvent& event)
{
    if (!m_widget->rect().contains(event.position()))
        return;

    if (event.buttons() & GMouseButton::Left || event.buttons() & GMouseButton::Right) {
        GPainter painter(m_widget->bitmap());

        if (m_last_drawing_event_position != Point(-1, -1)) {
            painter.draw_line(m_last_drawing_event_position, event.position(), m_widget->color_for(event));
            m_widget->update();
        } else {
            painter.set_pixel(event.position(), m_widget->color_for(event));
            m_widget->update({ event.position(), { 1, 1 } });
        }

        m_last_drawing_event_position = event.position();
    }
}
