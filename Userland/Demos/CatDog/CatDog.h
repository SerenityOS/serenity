/*
 * Copyright (c) 2021, Richard Gráčik <r.gracik@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ElapsedTimer.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MouseTracker.h>
#include <LibGUI/Widget.h>
#include <unistd.h>

#pragma once

class CatDog final : public GUI::Widget
    , GUI::MouseTracker {
    C_OBJECT(CatDog);

public:
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
            m_sleeping = false;
            m_curr_frame = 0;
            m_curr_bmp = m_alert;
            update();
        }
    }

private:
    Gfx::IntPoint m_temp_pos;
    Core::ElapsedTimer m_timer;

    int m_curr_frame = 1;
    int m_moveX, m_moveY = 0;
    bool m_up, m_down, m_left, m_right, m_sleeping = false;
    bool m_roaming { true };

    NonnullRefPtr<Gfx::Bitmap> m_alert = *Gfx::Bitmap::try_load_from_file("/res/icons/catdog/alert.png").release_value_but_fixme_should_propagate_errors();
    NonnullRefPtr<Gfx::Bitmap> m_erun1 = *Gfx::Bitmap::try_load_from_file("/res/icons/catdog/erun1.png").release_value_but_fixme_should_propagate_errors();
    NonnullRefPtr<Gfx::Bitmap> m_erun2 = *Gfx::Bitmap::try_load_from_file("/res/icons/catdog/erun2.png").release_value_but_fixme_should_propagate_errors();
    NonnullRefPtr<Gfx::Bitmap> m_nerun1 = *Gfx::Bitmap::try_load_from_file("/res/icons/catdog/nerun1.png").release_value_but_fixme_should_propagate_errors();
    NonnullRefPtr<Gfx::Bitmap> m_nerun2 = *Gfx::Bitmap::try_load_from_file("/res/icons/catdog/nerun2.png").release_value_but_fixme_should_propagate_errors();
    NonnullRefPtr<Gfx::Bitmap> m_nrun1 = *Gfx::Bitmap::try_load_from_file("/res/icons/catdog/nrun1.png").release_value_but_fixme_should_propagate_errors();
    NonnullRefPtr<Gfx::Bitmap> m_nrun2 = *Gfx::Bitmap::try_load_from_file("/res/icons/catdog/nrun2.png").release_value_but_fixme_should_propagate_errors();
    NonnullRefPtr<Gfx::Bitmap> m_nwrun1 = *Gfx::Bitmap::try_load_from_file("/res/icons/catdog/nwrun1.png").release_value_but_fixme_should_propagate_errors();
    NonnullRefPtr<Gfx::Bitmap> m_nwrun2 = *Gfx::Bitmap::try_load_from_file("/res/icons/catdog/nwrun2.png").release_value_but_fixme_should_propagate_errors();
    NonnullRefPtr<Gfx::Bitmap> m_serun1 = *Gfx::Bitmap::try_load_from_file("/res/icons/catdog/serun1.png").release_value_but_fixme_should_propagate_errors();
    NonnullRefPtr<Gfx::Bitmap> m_serun2 = *Gfx::Bitmap::try_load_from_file("/res/icons/catdog/serun2.png").release_value_but_fixme_should_propagate_errors();
    NonnullRefPtr<Gfx::Bitmap> m_sleep1 = *Gfx::Bitmap::try_load_from_file("/res/icons/catdog/sleep1.png").release_value_but_fixme_should_propagate_errors();
    NonnullRefPtr<Gfx::Bitmap> m_sleep2 = *Gfx::Bitmap::try_load_from_file("/res/icons/catdog/sleep2.png").release_value_but_fixme_should_propagate_errors();
    NonnullRefPtr<Gfx::Bitmap> m_srun1 = *Gfx::Bitmap::try_load_from_file("/res/icons/catdog/srun1.png").release_value_but_fixme_should_propagate_errors();
    NonnullRefPtr<Gfx::Bitmap> m_srun2 = *Gfx::Bitmap::try_load_from_file("/res/icons/catdog/srun2.png").release_value_but_fixme_should_propagate_errors();
    NonnullRefPtr<Gfx::Bitmap> m_still = *Gfx::Bitmap::try_load_from_file("/res/icons/catdog/still.png").release_value_but_fixme_should_propagate_errors();
    NonnullRefPtr<Gfx::Bitmap> m_swrun1 = *Gfx::Bitmap::try_load_from_file("/res/icons/catdog/swrun1.png").release_value_but_fixme_should_propagate_errors();
    NonnullRefPtr<Gfx::Bitmap> m_swrun2 = *Gfx::Bitmap::try_load_from_file("/res/icons/catdog/swrun2.png").release_value_but_fixme_should_propagate_errors();
    NonnullRefPtr<Gfx::Bitmap> m_wrun1 = *Gfx::Bitmap::try_load_from_file("/res/icons/catdog/wrun1.png").release_value_but_fixme_should_propagate_errors();
    NonnullRefPtr<Gfx::Bitmap> m_wrun2 = *Gfx::Bitmap::try_load_from_file("/res/icons/catdog/wrun2.png").release_value_but_fixme_should_propagate_errors();

    NonnullRefPtr<Gfx::Bitmap> m_curr_bmp = m_alert;
    CatDog()
        : m_temp_pos { 0, 0 }
    {
    }
};
