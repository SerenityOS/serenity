/*
 * Copyright (c) 2021, Richard Gráčik <r.gracik@gmail.com>
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <AK/RefPtr.h>
#include <LibCore/ElapsedTimer.h>
#include <LibCore/File.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MouseTracker.h>
#include <LibGUI/Widget.h>
#include <LibGfx/Bitmap.h>
#include <unistd.h>

class CatDog final : public GUI::Widget
    , GUI::MouseTracker {
    C_OBJECT(CatDog);

public:
    // The general state, does not contain movement direction or whether CatDog is roaming.
    enum class MainState {
        Idle,      // default state
        Alerted,   // woken by mouse cursor or speaking after being idle
        Sleeping,  // mouse hasn't moved in some time
        Artist,    // PixelPaint or FontEditor are open
        Inspector, // SystemServer, Profiler or Inspector are open
    };
    static bool is_non_application_state(MainState state)
    {
        return state == MainState::Idle || state == MainState::Alerted || state == MainState::Sleeping;
    }

    virtual void timer_event(Core::TimerEvent&) override;
    virtual void paint_event(GUI::PaintEvent& event) override;
    virtual void track_mouse_move(Gfx::IntPoint const& point) override;
    virtual void mousedown_event(GUI::MouseEvent& event) override;
    virtual void context_menu_event(GUI::ContextMenuEvent& event) override;

    void start_the_timer() { m_timer.start(); }

    Function<void()> on_click;
    Function<void(GUI::ContextMenuEvent&)> on_context_menu_request;

    bool roaming() const { return m_roaming; }
    void set_roaming(bool roaming)
    {
        m_roaming = roaming;
        if (!roaming) {
            // If we stop CatDog while it's in a program-specific state, we don't want it to be alerted.
            if (m_main_state == MainState::Idle || m_main_state == MainState::Sleeping)
                m_main_state = MainState::Alerted;
            m_curr_frame = 0;
            set_image_by_main_state();
            update();
        }
    }

    MainState main_state() const { return m_main_state; }

private:
    Gfx::IntPoint m_temp_pos;
    Core::ElapsedTimer m_timer;

    int m_curr_frame = 1;
    int m_moveX, m_moveY = 0;
    MainState m_main_state { MainState::Alerted };
    bool m_up, m_down, m_left, m_right;
    bool m_roaming { true };

    RefPtr<Core::File> m_proc_all;

    NonnullRefPtr<Gfx::Bitmap> m_alert = *Gfx::Bitmap::try_load_from_file("/res/icons/catdog/alert.png"sv).release_value_but_fixme_should_propagate_errors();
    NonnullRefPtr<Gfx::Bitmap> m_artist = *Gfx::Bitmap::try_load_from_file("/res/icons/catdog/artist.png"sv).release_value_but_fixme_should_propagate_errors();
    NonnullRefPtr<Gfx::Bitmap> m_erun1 = *Gfx::Bitmap::try_load_from_file("/res/icons/catdog/erun1.png"sv).release_value_but_fixme_should_propagate_errors();
    NonnullRefPtr<Gfx::Bitmap> m_erun2 = *Gfx::Bitmap::try_load_from_file("/res/icons/catdog/erun2.png"sv).release_value_but_fixme_should_propagate_errors();
    NonnullRefPtr<Gfx::Bitmap> m_inspector = *Gfx::Bitmap::try_load_from_file("/res/icons/catdog/inspector.png"sv).release_value_but_fixme_should_propagate_errors();
    NonnullRefPtr<Gfx::Bitmap> m_nerun1 = *Gfx::Bitmap::try_load_from_file("/res/icons/catdog/nerun1.png"sv).release_value_but_fixme_should_propagate_errors();
    NonnullRefPtr<Gfx::Bitmap> m_nerun2 = *Gfx::Bitmap::try_load_from_file("/res/icons/catdog/nerun2.png"sv).release_value_but_fixme_should_propagate_errors();
    NonnullRefPtr<Gfx::Bitmap> m_nrun1 = *Gfx::Bitmap::try_load_from_file("/res/icons/catdog/nrun1.png"sv).release_value_but_fixme_should_propagate_errors();
    NonnullRefPtr<Gfx::Bitmap> m_nrun2 = *Gfx::Bitmap::try_load_from_file("/res/icons/catdog/nrun2.png"sv).release_value_but_fixme_should_propagate_errors();
    NonnullRefPtr<Gfx::Bitmap> m_nwrun1 = *Gfx::Bitmap::try_load_from_file("/res/icons/catdog/nwrun1.png"sv).release_value_but_fixme_should_propagate_errors();
    NonnullRefPtr<Gfx::Bitmap> m_nwrun2 = *Gfx::Bitmap::try_load_from_file("/res/icons/catdog/nwrun2.png"sv).release_value_but_fixme_should_propagate_errors();
    NonnullRefPtr<Gfx::Bitmap> m_serun1 = *Gfx::Bitmap::try_load_from_file("/res/icons/catdog/serun1.png"sv).release_value_but_fixme_should_propagate_errors();
    NonnullRefPtr<Gfx::Bitmap> m_serun2 = *Gfx::Bitmap::try_load_from_file("/res/icons/catdog/serun2.png"sv).release_value_but_fixme_should_propagate_errors();
    NonnullRefPtr<Gfx::Bitmap> m_sleep1 = *Gfx::Bitmap::try_load_from_file("/res/icons/catdog/sleep1.png"sv).release_value_but_fixme_should_propagate_errors();
    NonnullRefPtr<Gfx::Bitmap> m_sleep2 = *Gfx::Bitmap::try_load_from_file("/res/icons/catdog/sleep2.png"sv).release_value_but_fixme_should_propagate_errors();
    NonnullRefPtr<Gfx::Bitmap> m_srun1 = *Gfx::Bitmap::try_load_from_file("/res/icons/catdog/srun1.png"sv).release_value_but_fixme_should_propagate_errors();
    NonnullRefPtr<Gfx::Bitmap> m_srun2 = *Gfx::Bitmap::try_load_from_file("/res/icons/catdog/srun2.png"sv).release_value_but_fixme_should_propagate_errors();
    NonnullRefPtr<Gfx::Bitmap> m_still = *Gfx::Bitmap::try_load_from_file("/res/icons/catdog/still.png"sv).release_value_but_fixme_should_propagate_errors();
    NonnullRefPtr<Gfx::Bitmap> m_swrun1 = *Gfx::Bitmap::try_load_from_file("/res/icons/catdog/swrun1.png"sv).release_value_but_fixme_should_propagate_errors();
    NonnullRefPtr<Gfx::Bitmap> m_swrun2 = *Gfx::Bitmap::try_load_from_file("/res/icons/catdog/swrun2.png"sv).release_value_but_fixme_should_propagate_errors();
    NonnullRefPtr<Gfx::Bitmap> m_wrun1 = *Gfx::Bitmap::try_load_from_file("/res/icons/catdog/wrun1.png"sv).release_value_but_fixme_should_propagate_errors();
    NonnullRefPtr<Gfx::Bitmap> m_wrun2 = *Gfx::Bitmap::try_load_from_file("/res/icons/catdog/wrun2.png"sv).release_value_but_fixme_should_propagate_errors();

    NonnullRefPtr<Gfx::Bitmap> m_curr_bmp = m_alert;

    // Used if CatDog is still; may also account for animation frames.
    void set_image_by_main_state()
    {
        switch (m_main_state) {
        case MainState::Idle:
            m_curr_bmp = m_still;
            break;
        case MainState::Alerted:
            m_curr_bmp = m_alert;
            break;
        case MainState::Sleeping:
            if (m_curr_frame == 1)
                m_curr_bmp = m_sleep1;
            else
                m_curr_bmp = m_sleep2;
            break;
        case MainState::Artist:
            m_curr_bmp = m_artist;
            break;
        case MainState::Inspector:
            m_curr_bmp = m_inspector;
            break;
        }
    }

    CatDog()
        : m_temp_pos { 0, 0 }
        , m_proc_all(MUST(Core::File::open("/proc/all", Core::OpenMode::ReadOnly)))
    {
        set_image_by_main_state();
    }
};
