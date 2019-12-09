#include "SprayTool.h"
#include "PaintableWidget.h"
#include <AK/Queue.h>
#include <AK/SinglyLinkedList.h>
#include <LibGUI/GPainter.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GMenu.h>
#include <LibDraw/GraphicsBitmap.h>
#include <stdio.h>
#include <LibM/math.h>

SprayTool::SprayTool()
{
    m_timer = CTimer::construct();
    m_timer->on_timeout = [&]() {
        paint_it();
    };
    m_timer->set_interval(200);
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
    ASSERT(bitmap.bpp() == 32);
    m_widget->update();
    const double minimal_radius = 10;
    const double base_radius = minimal_radius * m_thickness;
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
    m_timer->start();
    paint_it();
}

void SprayTool::on_mousemove(GMouseEvent& event)
{
    m_last_pos = event.position();
    if (m_timer->is_active()) {
        paint_it();
        m_timer->restart(m_timer->interval());
    }
}

void SprayTool::on_mouseup(GMouseEvent&)
{
    m_timer->stop();
}

void SprayTool::on_contextmenu(GContextMenuEvent& event)
{
    if (!m_context_menu) {
        m_context_menu = GMenu::construct();
        m_context_menu->add_action(GAction::create("1", [this](auto&) {
            m_thickness = 1;
        }));
        m_context_menu->add_action(GAction::create("2", [this](auto&) {
            m_thickness = 2;
        }));
        m_context_menu->add_action(GAction::create("3", [this](auto&) {
            m_thickness = 3;
        }));
        m_context_menu->add_action(GAction::create("4", [this](auto&) {
            m_thickness = 4;
        }));
    }
    m_context_menu->popup(event.screen_position());
}

