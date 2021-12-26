#include "EraseTool.h"
#include "PaintableWidget.h"
#include <LibGUI/GAction.h>
#include <LibGUI/GMenu.h>
#include <LibGUI/GPainter.h>

EraseTool::EraseTool()
{
}

EraseTool::~EraseTool()
{
}

Rect EraseTool::build_rect(const Point& pos, const Rect& widget_rect)
{
    const int base_eraser_size = 10;
    const int eraser_size = (base_eraser_size * m_thickness);
    const int eraser_radius = eraser_size / 2;
    const auto ex = pos.x();
    const auto ey = pos.y();
    return Rect(ex - eraser_radius, ey - eraser_radius, eraser_size, eraser_size).intersected(widget_rect);
}

void EraseTool::on_mousedown(GMouseEvent& event)
{
    if (event.button() != GMouseButton::Left && event.button() != GMouseButton::Right)
        return;
    Rect r = build_rect(event.position(), m_widget->bitmap().rect());
    GPainter painter(m_widget->bitmap());
    painter.fill_rect(r, Color(Color::White));
    m_widget->update();
}

void EraseTool::on_mousemove(GMouseEvent& event)
{
    if (!m_widget->rect().contains(event.position()))
        return;

    if (event.buttons() & GMouseButton::Left || event.buttons() & GMouseButton::Right) {
        Rect r = build_rect(event.position(), m_widget->bitmap().rect());
        GPainter painter(m_widget->bitmap());
        painter.fill_rect(r, Color(Color::White));
        m_widget->update();
    }
}

void EraseTool::on_contextmenu(GContextMenuEvent& event)
{
    if (!m_context_menu) {
        m_context_menu = make<GMenu>("EraseTool Context Menu");
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

