/*
 * Copyright (c) 2021, Richard Gráčik <r.gracik@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CatDog.h"
#include <LibGUI/Painter.h>
#include <LibGUI/Window.h>

void CatDog::timer_event(Core::TimerEvent&)
{
    if (!m_roaming)
        return;
    if (m_temp_pos.x() > 48) {
        m_left = false;
        m_right = true;
        m_moveX = 16;

        m_curr_bmp = m_erun1;
        if (m_curr_frame == 2)
            m_curr_bmp = m_erun2;
    } else if (m_temp_pos.x() < -16) {
        m_left = true;
        m_right = false;
        m_moveX = -16;

        m_curr_bmp = m_wrun1;
        if (m_curr_frame == 2)
            m_curr_bmp = m_wrun2;
    } else {
        m_left = false;
        m_right = false;
        m_moveX = 0;
    }

    if (m_temp_pos.y() > 48) {
        m_up = false;
        m_down = true;
        m_moveY = 10;

        m_curr_bmp = m_srun1;
        if (m_curr_frame == 2)
            m_curr_bmp = m_srun2;
    } else if (m_temp_pos.y() < -16) {
        m_up = true;
        m_down = false;
        m_moveY = -10;

        m_curr_bmp = m_nrun1;
        if (m_curr_frame == 2)
            m_curr_bmp = m_nrun2;
    } else {
        m_up = false;
        m_down = false;
        m_moveY = 0;
    }

    if (m_up && m_left) {
        m_curr_bmp = m_nwrun1;
        if (m_curr_frame == 2)
            m_curr_bmp = m_nwrun2;
    } else if (m_up && m_right) {
        m_curr_bmp = m_nerun1;
        if (m_curr_frame == 2)
            m_curr_bmp = m_nerun2;
    } else if (m_down && m_left) {
        m_curr_bmp = m_swrun1;
        if (m_curr_frame == 2)
            m_curr_bmp = m_swrun2;
    } else if (m_down && m_right) {
        m_curr_bmp = m_serun1;
        if (m_curr_frame == 2)
            m_curr_bmp = m_serun2;
    }

    window()->move_to(window()->position().x() + m_moveX, window()->position().y() + m_moveY);
    m_temp_pos.set_x(m_temp_pos.x() + (-m_moveX));
    m_temp_pos.set_y(m_temp_pos.y() + (-m_moveY));

    if (m_curr_frame == 1) {
        m_curr_frame = 2;
    } else {
        m_curr_frame = 1;
    }

    if (!m_up && !m_down && !m_left && !m_right) {
        m_curr_bmp = m_still;
        if (m_timer.elapsed() > 5000) {
            m_curr_bmp = m_sleep1;
            if (m_curr_frame == 2)
                m_curr_bmp = m_sleep2;
            m_sleeping = true;
        }
    }

    update();
}

void CatDog::paint_event(GUI::PaintEvent& event)
{
    GUI::Painter painter(*this);
    painter.clear_rect(event.rect(), Gfx::Color());
    painter.blit(Gfx::IntPoint(0, 0), *m_curr_bmp, m_curr_bmp->rect());
}

void CatDog::track_mouse_move(Gfx::IntPoint const& point)
{
    if (!m_roaming)
        return;
    Gfx::IntPoint relative_point = point - window()->position();
    if (m_temp_pos == relative_point)
        return;
    m_temp_pos = relative_point;
    m_timer.start();
    if (m_sleeping) {
        m_curr_bmp = m_alert;
        update();
    }
    m_sleeping = false;
}

void CatDog::mousedown_event(GUI::MouseEvent& event)
{
    if (event.button() != GUI::MouseButton::Primary)
        return;
    if (on_click)
        on_click();
}

void CatDog::context_menu_event(GUI::ContextMenuEvent& event)
{
    if (on_context_menu_request)
        on_context_menu_request(event);
}
