/*
 * Copyright (c) 2021, Richard Gráčik <r.gracik@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CatDog.h"
#include <LibGUI/Painter.h>
#include <LibGUI/Window.h>
#include <LibGUI/WindowServerConnection.h>

void CatDog::timer_event(Core::TimerEvent&)
{
    if (!m_roaming)
        return;

    if (m_temp_pos.x() > 48) {
        m_left = false;
        m_right = true;
        m_moveX = 16;

        m_current_frame = 2;
    } else if (m_temp_pos.x() < -16) {
        m_left = true;
        m_right = false;
        m_moveX = -16;

        m_current_frame = 9;
    } else {
        m_left = false;
        m_right = false;
        m_moveX = 0;
    }

    if (m_temp_pos.y() > 48) {
        m_up = false;
        m_down = true;
        m_moveY = 10;

        m_current_frame = 7;
    } else if (m_temp_pos.y() < -16) {
        m_up = true;
        m_down = false;
        m_moveY = -10;

        m_current_frame = 4;
    } else {
        m_up = false;
        m_down = false;
        m_moveY = 0;
    }

    if (m_up && m_left) {
        m_current_frame = 5;
    } else if (m_up && m_right) {
        m_current_frame = 3;
    } else if (m_down && m_left) {
        m_current_frame = 8;
    } else if (m_down && m_right) {
        m_current_frame = 6;
    }

    window()->move_to(window()->position().x() + m_moveX, window()->position().y() + m_moveY);
    m_temp_pos.set_x(m_temp_pos.x() + (-m_moveX));
    m_temp_pos.set_y(m_temp_pos.y() + (-m_moveY));

    if (m_current_second == 0) {
        m_current_second = 1;
    } else {
        m_current_second = 0;
    }

    if (!m_up && !m_down && !m_left && !m_right) {
        if (m_timer.elapsed() > 5000) {
            m_current_frame = 1;
            m_sleeping = true;
        } else {
            m_current_frame = 0;
            m_current_second = 0;
        }
    }

    update();
}

void CatDog::paint_event(GUI::PaintEvent& event)
{
    GUI::Painter painter(*this);
    painter.clear_rect(event.rect(), Gfx::Color());
    painter.blit(Gfx::IntPoint(0, 0), *m_skin, { m_current_second * 32, m_current_frame * 32, 32, 32 });
}

void CatDog::mousemove_event(GUI::MouseEvent& event)
{
    if (!m_roaming)
        return;
    if (m_temp_pos == event.position())
        return;
    m_temp_pos = event.position();
    m_timer.start();
    if (m_sleeping) {
        m_current_frame = 0;
        m_current_second = 1;
        update();
    }
    m_sleeping = false;
}

void CatDog::mousedown_event(GUI::MouseEvent& event)
{
    if (event.button() != GUI::MouseButton::Left)
        return;
    if (on_click)
        on_click();
}

void CatDog::track_cursor_globally()
{
    VERIFY(window());
    auto window_id = window()->window_id();
    VERIFY(window_id >= 0);

    set_global_cursor_tracking(true);
    GUI::WindowServerConnection::the().async_set_global_cursor_tracking(window_id, true);
}

void CatDog::context_menu_event(GUI::ContextMenuEvent& event)
{
    if (on_context_menu_request)
        on_context_menu_request(event);
}
