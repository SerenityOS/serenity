#include "LineTool.h"
#include "PaintableWidget.h"
#include <LibGUI/GAction.h>
#include <LibGUI/GMenu.h>
#include <LibGUI/GPainter.h>

LineTool::LineTool()
{
}

LineTool::~LineTool()
{
}

void LineTool::on_mousedown(GMouseEvent& event)
{
    if (event.button() != GMouseButton::Left && event.button() != GMouseButton::Right)
        return;

    if (m_drawing_button != GMouseButton::None)
        return;

    m_drawing_button = event.button();
    m_line_start_position = event.position();
    m_line_end_position = event.position();
    m_widget->update();
}

void LineTool::on_mouseup(GMouseEvent& event)
{
    if (event.button() == m_drawing_button) {
        GPainter painter(m_widget->bitmap());
        painter.draw_line(m_line_start_position, m_line_end_position, m_widget->color_for(m_drawing_button), m_thickness);
        m_drawing_button = GMouseButton::None;
        m_widget->update();
    }
}

void LineTool::on_mousemove(GMouseEvent& event)
{
    if (m_drawing_button == GMouseButton::None)
        return;

    if (!m_widget->rect().contains(event.position()))
        return;

    m_line_end_position = event.position();
    m_widget->update();
}

void LineTool::on_second_paint(GPaintEvent& event)
{
    if (m_drawing_button == GMouseButton::None)
        return;

    GPainter painter(*m_widget);
    painter.add_clip_rect(event.rect());
    painter.draw_line(m_line_start_position, m_line_end_position, m_widget->color_for(m_drawing_button), m_thickness);
}

void LineTool::on_contextmenu(GContextMenuEvent& event)
{
    if (!m_context_menu) {
        m_context_menu = make<GMenu>();
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
