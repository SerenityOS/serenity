#include "SprayTool.h"
#include "PaintableWidget.h"
#include <AK/Queue.h>
#include <AK/SinglyLinkedList.h>
#include <LibGUI/GPainter.h>
#include <SharedGraphics/GraphicsBitmap.h>
#include <stdio.h>
#include <LibM/math.h>

SprayTool::SprayTool()
{
    m_timer.on_timeout = [=]() {
        paint_it();
    };
    m_timer.set_interval(200);
}

SprayTool::~SprayTool()
{
}

static double nrand()
{
    return double(rand()) / double(RAND_MAX);
}

void SprayTool::paint_it()
{
    GPainter painter(m_widget->bitmap());
    auto& bitmap = m_widget->bitmap();
    ASSERT(bitmap.format() == GraphicsBitmap::Format::RGB32);
    m_widget->update();
    const double base_radius = 15;
    for (int i = 0; i < 100 + (nrand() * 800); i++) {
        double radius = base_radius * nrand();
        double angle = 2 * M_PI * nrand();
        const int xpos = m_last_pos.x() + radius * cos(angle);
        const int ypos = m_last_pos.y() - radius * sin(angle);
        if (xpos < 0 || xpos >= bitmap.width())
            continue;
        if (ypos < 0 || ypos >= bitmap.height())
            continue;
        bitmap.set_pixel<GraphicsBitmap::Format::RGB32>(xpos, ypos, m_color);
    }
}

void SprayTool::on_mousedown(GMouseEvent& event)
{
    if (!m_widget->rect().contains(event.position()))
        return;

    m_color = m_widget->color_for(event);
    m_last_pos = event.position();
    m_timer.start();
    paint_it();
}

void SprayTool::on_mousemove(GMouseEvent& event)
{
    m_last_pos = event.position();
    if (m_timer.is_active()) {
        paint_it();
        m_timer.restart(m_timer.interval());
    }
}

void SprayTool::on_mouseup(GMouseEvent&)
{
    m_timer.stop();
}
