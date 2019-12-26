#include "EllipseTool.h"
#include "PaintableWidget.h"
#include <LibDraw/Rect.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GMenu.h>
#include <LibGUI/GPainter.h>
#include <LibM/math.h>

EllipseTool::EllipseTool()
{
}

EllipseTool::~EllipseTool()
{
}

void EllipseTool::draw_using(Painter& painter)
{
    auto ellipse_intersecting_rect = Rect::from_two_points(m_ellipse_start_position, m_ellipse_end_position);
    switch (m_mode) {
    case Mode::Outline:
        painter.draw_ellipse_intersecting(ellipse_intersecting_rect, m_widget->color_for(m_drawing_button), m_thickness);
        break;
    default:
        ASSERT_NOT_REACHED();
    }
}

void EllipseTool::on_mousedown(GMouseEvent& event)
{
    if (event.button() != GMouseButton::Left && event.button() != GMouseButton::Right)
        return;

    if (m_drawing_button != GMouseButton::None)
        return;

    m_drawing_button = event.button();
    m_ellipse_start_position = event.position();
    m_ellipse_end_position = event.position();
    m_widget->update();
}

void EllipseTool::on_mouseup(GMouseEvent& event)
{
    if (event.button() == m_drawing_button) {
        GPainter painter(m_widget->bitmap());
        draw_using(painter);
        m_drawing_button = GMouseButton::None;
        m_widget->update();
    }
}

void EllipseTool::on_mousemove(GMouseEvent& event)
{
    if (m_drawing_button == GMouseButton::None)
        return;

    if (!m_widget->rect().contains(event.position()))
        return;

    m_ellipse_end_position = event.position();
    m_widget->update();
}

void EllipseTool::on_second_paint(GPaintEvent& event)
{
    if (m_drawing_button == GMouseButton::None)
        return;

    GPainter painter(*m_widget);
    painter.add_clip_rect(event.rect());
    draw_using(painter);
}

void EllipseTool::on_keydown(GKeyEvent& event)
{
    if (event.key() == Key_Escape && m_drawing_button != GMouseButton::None) {
        m_drawing_button = GMouseButton::None;
        m_widget->update();
        event.accept();
    }
}

void EllipseTool::on_contextmenu(GContextMenuEvent& event)
{
    if (!m_context_menu) {
        m_context_menu = GMenu::construct();
        m_context_menu->add_action(GAction::create("Outline", [this](auto&) {
            m_mode = Mode::Outline;
        }));
        m_context_menu->add_separator();
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
