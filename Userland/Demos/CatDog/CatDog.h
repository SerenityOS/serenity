/*
 * Copyright (c) 2021, Richard Gráčik <r.gracik@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ElapsedTimer.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Widget.h>
#include <math.h>
#include <unistd.h>

#pragma once

class CatDog final : public GUI::Widget {
    C_OBJECT(CatDog);

public:
    virtual void timer_event(Core::TimerEvent&) override;
    virtual void paint_event(GUI::PaintEvent& event) override;
    virtual void mousemove_event(GUI::MouseEvent& event) override;
    virtual void mousedown_event(GUI::MouseEvent& event) override;
    virtual void context_menu_event(GUI::ContextMenuEvent& event) override;

    void track_cursor_globally();
    void set_skin(String const& skin_name, String const& skin_path)
    {
        m_skin_name = skin_name;
        m_skin = *Gfx::Bitmap::load_from_file(skin_path);
    };
    void start_the_timer() { m_timer.start(); }

    Function<void()> on_click;
    Function<void(GUI::ContextMenuEvent&)> on_context_menu_request;

    String const& skin_name() { return m_skin_name; }
    bool roaming() const { return m_roaming; }
    void set_roaming(bool roaming)
    {
        m_roaming = roaming;
        if (!roaming) {
            m_sleeping = false;
            m_current_frame = 0;
            m_current_second = 1;
            update();
        }
    }

private:
    Gfx::IntPoint m_temp_pos;
    Core::ElapsedTimer m_timer;

    int m_current_frame = 0;
    int m_current_second = 0;
    int m_moveX, m_moveY = 0;
    bool m_up, m_down, m_left, m_right, m_sleeping = false;
    bool m_roaming { true };
    String m_skin_name = "CatDog";

    AK::NonnullRefPtr<Gfx::Bitmap> m_skin = *Gfx::Bitmap::load_from_file("/res/icons/catdog/CatDog.png");

    CatDog()
        : m_temp_pos { 0, 0 }
    {
    }
};
