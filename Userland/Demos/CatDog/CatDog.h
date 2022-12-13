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
#include <LibCore/Stream.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MouseTracker.h>
#include <LibGUI/Widget.h>
#include <LibGfx/Bitmap.h>
#include <unistd.h>

class CatDog final : public GUI::Widget
    , GUI::MouseTracker {
    C_OBJECT(CatDog);

public:
    static ErrorOr<NonnullRefPtr<CatDog>> create();

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
    virtual void track_mouse_move(Gfx::IntPoint point) override;
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

    NonnullOwnPtr<Core::Stream::File> m_proc_all;

    RefPtr<Gfx::Bitmap> m_alert;
    RefPtr<Gfx::Bitmap> m_artist;
    RefPtr<Gfx::Bitmap> m_erun1;
    RefPtr<Gfx::Bitmap> m_erun2;
    RefPtr<Gfx::Bitmap> m_inspector;
    RefPtr<Gfx::Bitmap> m_nerun1;
    RefPtr<Gfx::Bitmap> m_nerun2;
    RefPtr<Gfx::Bitmap> m_nrun1;
    RefPtr<Gfx::Bitmap> m_nrun2;
    RefPtr<Gfx::Bitmap> m_nwrun1;
    RefPtr<Gfx::Bitmap> m_nwrun2;
    RefPtr<Gfx::Bitmap> m_serun1;
    RefPtr<Gfx::Bitmap> m_serun2;
    RefPtr<Gfx::Bitmap> m_sleep1;
    RefPtr<Gfx::Bitmap> m_sleep2;
    RefPtr<Gfx::Bitmap> m_srun1;
    RefPtr<Gfx::Bitmap> m_srun2;
    RefPtr<Gfx::Bitmap> m_still;
    RefPtr<Gfx::Bitmap> m_swrun1;
    RefPtr<Gfx::Bitmap> m_swrun2;
    RefPtr<Gfx::Bitmap> m_wrun1;
    RefPtr<Gfx::Bitmap> m_wrun2;

    RefPtr<Gfx::Bitmap> m_curr_bmp;

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
        , m_proc_all(MUST(Core::Stream::File::open("/sys/kernel/processes"sv, Core::Stream::OpenMode::Read)))
    {
        set_image_by_main_state();
    }
};
