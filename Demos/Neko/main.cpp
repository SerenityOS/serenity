/*
 * Copyright (c) 2020, Richard Gráčik <r.gracik@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <LibCore/ElapsedTimer.h>
#include <LibGUI/AboutDialog.h>
#include <LibGUI/ImageWidget.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Window.h>
#include <LibGUI/WindowServerConnection.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MenuBar.h>
#include <LibGfx/Bitmap.h>
#include <LibGUI/Icon.h>

class MainFrame final : public GUI::Widget {
    C_OBJECT(MainFrame);

public:
    virtual void timer_event(Core::TimerEvent&) override
    {
        if (m_temp_pos.x() > 24)
        {
            left = false;
            right = true;
        }
        else if (m_temp_pos.x() < -23)
        {
            left = true;
            right = false;
        }
        else {
            left = false;
            right = false;
        }

        if (m_temp_pos.y() > 24)
        {
            up = false;
            down = true;
        }
        else if (m_temp_pos.y() < -23)
        {
            up = true;
            down = false;
        }
        else {
            up = false;
            down = false;
        }

        if (up && left)
        {
            curr_bmp = nwrun1;
            if(curr_frame == 2)
               curr_bmp = nwrun2;
            window()->move_to(window()->position().x()-16,window()->position().y()-16);
            m_temp_pos.set_x(m_temp_pos.x()+16);
            m_temp_pos.set_y(m_temp_pos.y()+16);
        }
        else if (up && right)
        {
            curr_bmp = nerun1;
            if(curr_frame == 2)
               curr_bmp = nerun2;
            window()->move_to(window()->position().x()+16,window()->position().y()-16);
            m_temp_pos.set_x(m_temp_pos.x()-16);
            m_temp_pos.set_y(m_temp_pos.y()+16);
        }
        else if (down && left)
        {
            curr_bmp = swrun1;
            if(curr_frame == 2)
               curr_bmp = swrun2;
            window()->move_to(window()->position().x()-16,window()->position().y()+16);
            m_temp_pos.set_x(m_temp_pos.x()+16);
            m_temp_pos.set_y(m_temp_pos.y()-16);
        }
        else if (down && right)
        {
            curr_bmp = serun1;
            if(curr_frame == 2)
               curr_bmp = serun2;
            window()->move_to(window()->position().x()+16,window()->position().y()+16);
            m_temp_pos.set_x(m_temp_pos.x()-16);
            m_temp_pos.set_y(m_temp_pos.y()-16);
        }
        else if (up)
        {
            curr_bmp = nrun1;
            if(curr_frame == 2)
               curr_bmp = nrun2;
            window()->move_to(window()->position().x(),window()->position().y()-16);
            m_temp_pos.set_y(m_temp_pos.y()+16);
        }
        else if (down)
        {
            curr_bmp = srun1;
            if(curr_frame == 2)
               curr_bmp = srun2;
            window()->move_to(window()->position().x(),window()->position().y()+16);
            m_temp_pos.set_y(m_temp_pos.y()-16);
        }
        else if (left)
        {
            curr_bmp = wrun1;
            if(curr_frame == 2)
               curr_bmp = wrun2;
            window()->move_to(window()->position().x()-16,window()->position().y());
            m_temp_pos.set_x(m_temp_pos.x()+16);
        }
        else if (right)
        {
            curr_bmp = erun1;
            if(curr_frame == 2)
               curr_bmp = erun2;
            window()->move_to(window()->position().x()+16,window()->position().y());
            m_temp_pos.set_x(m_temp_pos.x()-16);
        }

        if (!up && !down && !left && !right)
        {
            if(restartTimer)
            {
                timer.start();
                restartTimer = false;
            }
            curr_bmp = still;
            if(sleeping)
                curr_bmp = alert;

            if (timer.elapsed() > 5000)
            {
                curr_bmp = sleep1;
                if(curr_frame == 2)
                   curr_bmp = sleep2;
                sleeping = true;
            }
        }

        if (curr_frame == 1)
        {
            curr_frame = 2;
        }else{
            curr_frame = 1;
        }

        update();
    }

    void paint_event(GUI::PaintEvent& event) override
    {
        GUI::Painter painter(*this);
        painter.clear_rect(event.rect(), Gfx::Color());
        painter.blit(Gfx::IntPoint(0,0), *curr_bmp, curr_bmp->rect());
    }

    void mousemove_event(GUI::MouseEvent& event) override
    {
        m_temp_pos = event.position();
        restartTimer = true;
        if(sleeping)
            sleeping = false;
    }

    void track_cursor_globally() {
        ASSERT(window());
        auto window_id = window()->window_id();
        ASSERT(window_id >= 0);

        set_global_cursor_tracking(true);
        GUI::WindowServerConnection::the().send_sync<Messages::WindowServer::SetGlobalCursorTracking>(window_id, true);
    }

    void start_the_timer() {
        timer.start();
    }
private:
    Gfx::IntPoint m_temp_pos;
    Core::ElapsedTimer timer;
    int curr_frame = 1;
    bool up, down, left, right, restartTimer, sleeping = false;

    NonnullRefPtr<Gfx::Bitmap> alert = *Gfx::Bitmap::load_from_file("/res/icons/neko/alert.png");
    NonnullRefPtr<Gfx::Bitmap> erun1 = *Gfx::Bitmap::load_from_file("/res/icons/neko/erun1.png");
    NonnullRefPtr<Gfx::Bitmap> erun2 = *Gfx::Bitmap::load_from_file("/res/icons/neko/erun2.png");
    //NonnullRefPtr<Gfx::Bitmap> escratch1 = *Gfx::Bitmap::load_from_file("/res/icons/neko/escratch1.png");
    //NonnullRefPtr<Gfx::Bitmap> escratch2 = *Gfx::Bitmap::load_from_file("/res/icons/neko/escratch2.png");
    NonnullRefPtr<Gfx::Bitmap> itch1 = *Gfx::Bitmap::load_from_file("/res/icons/neko/itch1.png");
    NonnullRefPtr<Gfx::Bitmap> itch2 = *Gfx::Bitmap::load_from_file("/res/icons/neko/itch2.png");
    NonnullRefPtr<Gfx::Bitmap> nerun1 = *Gfx::Bitmap::load_from_file("/res/icons/neko/nerun1.png");
    NonnullRefPtr<Gfx::Bitmap> nerun2 = *Gfx::Bitmap::load_from_file("/res/icons/neko/nerun2.png");
    NonnullRefPtr<Gfx::Bitmap> nrun1 = *Gfx::Bitmap::load_from_file("/res/icons/neko/nrun1.png");
    NonnullRefPtr<Gfx::Bitmap> nrun2 = *Gfx::Bitmap::load_from_file("/res/icons/neko/nrun2.png");
    //NonnullRefPtr<Gfx::Bitmap> nscratch1 = *Gfx::Bitmap::load_from_file("/res/icons/neko/nscratch1.png");
    //NonnullRefPtr<Gfx::Bitmap> nscratch2 = *Gfx::Bitmap::load_from_file("/res/icons/neko/nscratch2.png");
    NonnullRefPtr<Gfx::Bitmap> nwrun1 = *Gfx::Bitmap::load_from_file("/res/icons/neko/nwrun1.png");
    NonnullRefPtr<Gfx::Bitmap> nwrun2 = *Gfx::Bitmap::load_from_file("/res/icons/neko/nwrun2.png");
    NonnullRefPtr<Gfx::Bitmap> serun1 = *Gfx::Bitmap::load_from_file("/res/icons/neko/serun1.png");
    NonnullRefPtr<Gfx::Bitmap> serun2 = *Gfx::Bitmap::load_from_file("/res/icons/neko/serun2.png");
    NonnullRefPtr<Gfx::Bitmap> sleep1 = *Gfx::Bitmap::load_from_file("/res/icons/neko/sleep1.png");
    NonnullRefPtr<Gfx::Bitmap> sleep2 = *Gfx::Bitmap::load_from_file("/res/icons/neko/sleep2.png");
    NonnullRefPtr<Gfx::Bitmap> srun1 = *Gfx::Bitmap::load_from_file("/res/icons/neko/srun1.png");
    NonnullRefPtr<Gfx::Bitmap> srun2 = *Gfx::Bitmap::load_from_file("/res/icons/neko/srun2.png");
    //NonnullRefPtr<Gfx::Bitmap> sscratch1 = *Gfx::Bitmap::load_from_file("/res/icons/neko/sscratch1.png");
    //NonnullRefPtr<Gfx::Bitmap> sscratch2 = *Gfx::Bitmap::load_from_file("/res/icons/neko/sscratch2.png");
    NonnullRefPtr<Gfx::Bitmap> still = *Gfx::Bitmap::load_from_file("/res/icons/neko/still.png");
    NonnullRefPtr<Gfx::Bitmap> swrun1 = *Gfx::Bitmap::load_from_file("/res/icons/neko/swrun1.png");
    NonnullRefPtr<Gfx::Bitmap> swrun2 = *Gfx::Bitmap::load_from_file("/res/icons/neko/swrun2.png");
    NonnullRefPtr<Gfx::Bitmap> wrun1 = *Gfx::Bitmap::load_from_file("/res/icons/neko/wrun1.png");
    NonnullRefPtr<Gfx::Bitmap> wrun2 = *Gfx::Bitmap::load_from_file("/res/icons/neko/wrun2.png");
    //NonnullRefPtr<Gfx::Bitmap> wscratch1 = *Gfx::Bitmap::load_from_file("/res/icons/neko/wscratch1.png");
    //NonnullRefPtr<Gfx::Bitmap> wscratch2 = *Gfx::Bitmap::load_from_file("/res/icons/neko/wscratch2.png");
    NonnullRefPtr<Gfx::Bitmap> yawn = *Gfx::Bitmap::load_from_file("/res/icons/neko/yawn.png");

    NonnullRefPtr<Gfx::Bitmap> curr_bmp = alert;
    MainFrame()
        : m_temp_pos { 0, 0 }
    {}
};

int main(int argc, char** argv)
{
    if (pledge("stdio shared_buffer accept rpath unix cpath fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

    if (pledge("stdio shared_buffer accept rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/res", "r") < 0) {
        perror("unveil");
        return 1;
    }

    auto app_icon = GUI::Icon::default_icon("app-neko");

    auto window = GUI::Window::construct();
    window->set_title("Neko Demo");
    window->resize(32,32);
    window->set_frameless(true);
    window->set_resizable(false);
    window->set_has_alpha_channel(true);

    auto& root_widget = window->set_main_widget<MainFrame>();
    root_widget.set_layout<GUI::VerticalBoxLayout>();
    root_widget.layout()->set_spacing(0);

    auto menubar = GUI::MenuBar::construct();
    auto& app_menu = menubar->add_menu("Neko Demo");
    app_menu.add_action(GUI::CommonActions::make_quit_action([&](auto&) { app->quit(); }));

    auto& help_menu = menubar->add_menu("Help");
    help_menu.add_action(GUI::Action::create("About", [&](auto&) {
        GUI::AboutDialog::show("Neko Demo", app_icon.bitmap_for_size(32), window);
    }));

    app->set_menubar(move(menubar));
    window->show();
    root_widget.track_cursor_globally();
    root_widget.start_timer(250);
    root_widget.start_the_timer(); //used to start the second timer for "mouse sleep"

    return app->exec();
}
