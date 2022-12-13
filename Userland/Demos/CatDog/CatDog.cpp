/*
 * Copyright (c) 2021, Richard Gráčik <r.gracik@gmail.com>
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CatDog.h"
#include <LibCore/ProcessStatisticsReader.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Window.h>

ErrorOr<NonnullRefPtr<CatDog>> CatDog::create()
{
    auto catdog = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) CatDog));
    catdog->m_alert = *TRY(Gfx::Bitmap::try_load_from_file("/res/icons/catdog/alert.png"sv));
    catdog->m_artist = *TRY(Gfx::Bitmap::try_load_from_file("/res/icons/catdog/artist.png"sv));
    catdog->m_erun1 = *TRY(Gfx::Bitmap::try_load_from_file("/res/icons/catdog/erun1.png"sv));
    catdog->m_erun2 = *TRY(Gfx::Bitmap::try_load_from_file("/res/icons/catdog/erun2.png"sv));
    catdog->m_inspector = *TRY(Gfx::Bitmap::try_load_from_file("/res/icons/catdog/inspector.png"sv));
    catdog->m_nerun1 = *TRY(Gfx::Bitmap::try_load_from_file("/res/icons/catdog/nerun1.png"sv));
    catdog->m_nerun2 = *TRY(Gfx::Bitmap::try_load_from_file("/res/icons/catdog/nerun2.png"sv));
    catdog->m_nrun1 = *TRY(Gfx::Bitmap::try_load_from_file("/res/icons/catdog/nrun1.png"sv));
    catdog->m_nrun2 = *TRY(Gfx::Bitmap::try_load_from_file("/res/icons/catdog/nrun2.png"sv));
    catdog->m_nwrun1 = *TRY(Gfx::Bitmap::try_load_from_file("/res/icons/catdog/nwrun1.png"sv));
    catdog->m_nwrun2 = *TRY(Gfx::Bitmap::try_load_from_file("/res/icons/catdog/nwrun2.png"sv));
    catdog->m_serun1 = *TRY(Gfx::Bitmap::try_load_from_file("/res/icons/catdog/serun1.png"sv));
    catdog->m_serun2 = *TRY(Gfx::Bitmap::try_load_from_file("/res/icons/catdog/serun2.png"sv));
    catdog->m_sleep1 = *TRY(Gfx::Bitmap::try_load_from_file("/res/icons/catdog/sleep1.png"sv));
    catdog->m_sleep2 = *TRY(Gfx::Bitmap::try_load_from_file("/res/icons/catdog/sleep2.png"sv));
    catdog->m_srun1 = *TRY(Gfx::Bitmap::try_load_from_file("/res/icons/catdog/srun1.png"sv));
    catdog->m_srun2 = *TRY(Gfx::Bitmap::try_load_from_file("/res/icons/catdog/srun2.png"sv));
    catdog->m_still = *TRY(Gfx::Bitmap::try_load_from_file("/res/icons/catdog/still.png"sv));
    catdog->m_swrun1 = *TRY(Gfx::Bitmap::try_load_from_file("/res/icons/catdog/swrun1.png"sv));
    catdog->m_swrun2 = *TRY(Gfx::Bitmap::try_load_from_file("/res/icons/catdog/swrun2.png"sv));
    catdog->m_wrun1 = *TRY(Gfx::Bitmap::try_load_from_file("/res/icons/catdog/wrun1.png"sv));
    catdog->m_wrun2 = *TRY(Gfx::Bitmap::try_load_from_file("/res/icons/catdog/wrun2.png"sv));
    catdog->m_curr_bmp = catdog->m_alert;
    return catdog;
}

void CatDog::timer_event(Core::TimerEvent&)
{
    auto maybe_proc_info = Core::ProcessStatisticsReader::get_all(*m_proc_all);
    if (!maybe_proc_info.is_error()) {
        auto proc_info = maybe_proc_info.release_value();

        auto maybe_paint_program = proc_info.processes.first_matching([](auto& process) {
            return process.name.equals_ignoring_case("pixelpaint"sv) || process.name.equals_ignoring_case("fonteditor"sv);
        });
        if (maybe_paint_program.has_value()) {
            m_main_state = MainState::Artist;
        } else {
            auto maybe_inspector_program = proc_info.processes.first_matching([](auto& process) {
                return process.name.equals_ignoring_case("inspector"sv) || process.name.equals_ignoring_case("systemmonitor"sv) || process.name.equals_ignoring_case("profiler"sv);
            });

            if (maybe_inspector_program.has_value())
                m_main_state = MainState::Inspector;
            // If we currently have an application state but that app isn't open anymore, go back to idle.
            else if (!is_non_application_state(m_main_state))
                m_main_state = MainState::Idle;
        }
    }

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
        // Select the movement-free image based on the main state.
        if (m_timer.elapsed() > 5000)
            m_main_state = MainState::Sleeping;
        set_image_by_main_state();
    } else if (is_non_application_state(m_main_state)) {
        // If CatDog currently moves, it should be idle the next time it stops.
        m_main_state = MainState::Idle;
    }

    update();
}

void CatDog::paint_event(GUI::PaintEvent& event)
{
    GUI::Painter painter(*this);
    painter.clear_rect(event.rect(), Gfx::Color());
    painter.blit(Gfx::IntPoint(0, 0), *m_curr_bmp, m_curr_bmp->rect());
}

void CatDog::track_mouse_move(Gfx::IntPoint point)
{
    if (!m_roaming)
        return;
    Gfx::IntPoint relative_point = point - window()->position();
    if (m_temp_pos == relative_point)
        return;
    m_temp_pos = relative_point;
    m_timer.start();
    if (m_main_state == MainState::Sleeping) {
        m_main_state = MainState::Alerted;
        set_image_by_main_state();
        update();
    }
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
