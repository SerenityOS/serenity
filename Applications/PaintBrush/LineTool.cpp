#include "LineTool.h"
#include "PaintableWidget.h"
#include <LibGUI/GAction.h>
#include <LibGUI/GMenu.h>
#include <LibGUI/GPainter.h>
#include <LibM/math.h>

Point constrain_line_angle(const Point& start_pos, const Point& end_pos, float angle_increment)
{
    float current_angle = atan2(end_pos.y() - start_pos.y(), end_pos.x() - start_pos.x()) + M_PI * 2.;

    float constrained_angle = ((int)((current_angle + angle_increment / 2.) / angle_increment)) * angle_increment;

    auto diff = end_pos - start_pos;
    float line_length = sqrt(diff.x() * diff.x() + diff.y() * diff.y());

    return { start_pos.x() + (int)(cos(constrained_angle) * line_length),
        start_pos.y() + (int)(sin(constrained_angle) * line_length) };
}

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

    if (!m_constrain_angle) {
        m_line_end_position = event.position();
    } else {
        const float ANGLE_STEP = M_PI / 8.0f;
        m_line_end_position = constrain_line_angle(m_line_start_position, event.position(), ANGLE_STEP);
    }
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

void LineTool::on_keydown(GKeyEvent& event)
{
    if (event.key() == Key_Escape && m_drawing_button != GMouseButton::None) {
        m_drawing_button = GMouseButton::None;
        m_widget->update();
        event.accept();
    }

    if (event.key() == Key_Shift) {
        m_constrain_angle = true;
        m_widget->update();
        event.accept();
    }
}

void LineTool::on_keyup(GKeyEvent& event)
{
    if (event.key() == Key_Shift) {
        m_constrain_angle = false;
        m_widget->update();
        event.accept();
    }
}

void LineTool::on_contextmenu(GContextMenuEvent& event)
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
