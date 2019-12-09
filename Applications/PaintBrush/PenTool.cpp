#include "PenTool.h"
#include "PaintableWidget.h"
#include <LibGUI/GAction.h>
#include <LibGUI/GMenu.h>
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
    painter.draw_line(event.position(), event.position(), m_widget->color_for(event), m_thickness);
    m_widget->update();
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

        if (m_last_drawing_event_position != Point(-1, -1))
            painter.draw_line(m_last_drawing_event_position, event.position(), m_widget->color_for(event), m_thickness);
        else
            painter.draw_line(event.position(), event.position(), m_widget->color_for(event), m_thickness);
        m_widget->update();

        m_last_drawing_event_position = event.position();
    }
}

void PenTool::on_contextmenu(GContextMenuEvent& event)
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
