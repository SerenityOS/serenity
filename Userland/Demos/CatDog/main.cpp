/*
 * Copyright (c) 2021, Richard Gráčik <r.gracik@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

#include <LibCore/ElapsedTimer.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MenuBar.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibGUI/WindowServerConnection.h>
#include <LibGfx/Bitmap.h>

class MainFrame final : public GUI::Widget {
    C_OBJECT(MainFrame);

public:
    virtual void timer_event(Core::TimerEvent&) override
    {
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

    void paint_event(GUI::PaintEvent& event) override
    {
        GUI::Painter painter(*this);
        painter.clear_rect(event.rect(), Gfx::Color());
        painter.blit(Gfx::IntPoint(0, 0), *m_curr_bmp, m_curr_bmp->rect());
    }

    void mousemove_event(GUI::MouseEvent& event) override
    {
        if (m_temp_pos == event.position())
            return;
        m_temp_pos = event.position();
        m_timer.start();
        if (m_sleeping) {
            m_curr_bmp = m_alert;
            update();
        }
        m_sleeping = false;
    }

    void track_cursor_globally()
    {
        VERIFY(window());
        auto window_id = window()->window_id();
        VERIFY(window_id >= 0);

        set_global_cursor_tracking(true);
        GUI::WindowServerConnection::the().send_sync<Messages::WindowServer::SetGlobalCursorTracking>(window_id, true);
    }

    void start_the_timer() { m_timer.start(); }

private:
    Gfx::IntPoint m_temp_pos;
    Core::ElapsedTimer m_timer;
    int m_curr_frame = 1;
    int m_moveX, m_moveY = 0;
    bool m_up, m_down, m_left, m_right, m_sleeping = false;

    NonnullRefPtr<Gfx::Bitmap> m_alert = *Gfx::Bitmap::load_from_file("/res/icons/catdog/alert.png");
    NonnullRefPtr<Gfx::Bitmap> m_erun1 = *Gfx::Bitmap::load_from_file("/res/icons/catdog/erun1.png");
    NonnullRefPtr<Gfx::Bitmap> m_erun2 = *Gfx::Bitmap::load_from_file("/res/icons/catdog/erun2.png");
    NonnullRefPtr<Gfx::Bitmap> m_nerun1 = *Gfx::Bitmap::load_from_file("/res/icons/catdog/nerun1.png");
    NonnullRefPtr<Gfx::Bitmap> m_nerun2 = *Gfx::Bitmap::load_from_file("/res/icons/catdog/nerun2.png");
    NonnullRefPtr<Gfx::Bitmap> m_nrun1 = *Gfx::Bitmap::load_from_file("/res/icons/catdog/nrun1.png");
    NonnullRefPtr<Gfx::Bitmap> m_nrun2 = *Gfx::Bitmap::load_from_file("/res/icons/catdog/nrun2.png");
    NonnullRefPtr<Gfx::Bitmap> m_nwrun1 = *Gfx::Bitmap::load_from_file("/res/icons/catdog/nwrun1.png");
    NonnullRefPtr<Gfx::Bitmap> m_nwrun2 = *Gfx::Bitmap::load_from_file("/res/icons/catdog/nwrun2.png");
    NonnullRefPtr<Gfx::Bitmap> m_serun1 = *Gfx::Bitmap::load_from_file("/res/icons/catdog/serun1.png");
    NonnullRefPtr<Gfx::Bitmap> m_serun2 = *Gfx::Bitmap::load_from_file("/res/icons/catdog/serun2.png");
    NonnullRefPtr<Gfx::Bitmap> m_sleep1 = *Gfx::Bitmap::load_from_file("/res/icons/catdog/sleep1.png");
    NonnullRefPtr<Gfx::Bitmap> m_sleep2 = *Gfx::Bitmap::load_from_file("/res/icons/catdog/sleep2.png");
    NonnullRefPtr<Gfx::Bitmap> m_srun1 = *Gfx::Bitmap::load_from_file("/res/icons/catdog/srun1.png");
    NonnullRefPtr<Gfx::Bitmap> m_srun2 = *Gfx::Bitmap::load_from_file("/res/icons/catdog/srun2.png");
    NonnullRefPtr<Gfx::Bitmap> m_still = *Gfx::Bitmap::load_from_file("/res/icons/catdog/still.png");
    NonnullRefPtr<Gfx::Bitmap> m_swrun1 = *Gfx::Bitmap::load_from_file("/res/icons/catdog/swrun1.png");
    NonnullRefPtr<Gfx::Bitmap> m_swrun2 = *Gfx::Bitmap::load_from_file("/res/icons/catdog/swrun2.png");
    NonnullRefPtr<Gfx::Bitmap> m_wrun1 = *Gfx::Bitmap::load_from_file("/res/icons/catdog/wrun1.png");
    NonnullRefPtr<Gfx::Bitmap> m_wrun2 = *Gfx::Bitmap::load_from_file("/res/icons/catdog/wrun2.png");

    NonnullRefPtr<Gfx::Bitmap> m_curr_bmp = m_alert;
    MainFrame()
        : m_temp_pos { 0, 0 }
    {
    }
};

int main(int argc, char** argv)
{
    if (pledge("stdio recvfd sendfd rpath wpath cpath accept unix fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);
    auto app_icon = GUI::Icon::default_icon("app-catdog");

    if (pledge("stdio recvfd sendfd rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/res", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil(nullptr, nullptr) < 0) {
        perror("unveil");
        return 1;
    }

    auto window = GUI::Window::construct();
    window->set_title("CatDog Demo");
    window->resize(32, 32);
    window->set_frameless(true);
    window->set_resizable(false);
    window->set_has_alpha_channel(true);
    window->set_alpha_hit_threshold(1.0f);
    window->set_icon(app_icon.bitmap_for_size(16));

    auto& root_widget = window->set_main_widget<MainFrame>();
    root_widget.set_layout<GUI::VerticalBoxLayout>();
    root_widget.layout()->set_spacing(0);

    auto menubar = GUI::MenuBar::construct();
    auto& app_menu = menubar->add_menu("File");
    app_menu.add_action(GUI::CommonActions::make_quit_action([&](auto&) { app->quit(); }));
    auto& help_menu = menubar->add_menu("Help");
    help_menu.add_action(GUI::CommonActions::make_about_action("CatDog Demo", app_icon, window));
    window->set_menubar(move(menubar));

    window->show();
    root_widget.track_cursor_globally();
    root_widget.start_timer(250, Core::TimerShouldFireWhenNotVisible::Yes);
    root_widget.start_the_timer(); // timer for "mouse sleep detection"

    return app->exec();
}
