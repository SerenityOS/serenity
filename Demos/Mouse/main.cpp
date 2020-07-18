/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include <LibGUI/AboutDialog.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Frame.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MenuBar.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Path.h>

#include <math.h>

class MainFrame final : public GUI::Frame {
    C_OBJECT(MainFrame);

public:
    virtual void timer_event(Core::TimerEvent&) override
    {

        m_show_scroll_wheel = false;
        stop_timer();
        update();
    }

    virtual void paint_event(GUI::PaintEvent& event) override
    {
        GUI::Painter painter(*this);
        painter.add_clip_rect(event.rect());
        painter.fill_rect(frame_inner_rect(), Color::White);

        Gfx::Path path;
        // draw mouse outline
        path.move_to({ 30, 140 });
        path.line_to({ 30, 20 });
        path.line_to({ 65, 12 });
        path.line_to({ 95, 12 });
        path.line_to({ 130, 20 });
        path.line_to({ 130, 140 });
        path.line_to({ 30, 140 });

        // draw button separator
        path.move_to({ 30, 65 });
        path.line_to({ 130, 65 });

        path.move_to({ 65, 65 });
        path.line_to({ 65, 13 });

        path.move_to({ 95, 65 });
        path.line_to({ 95, 13 });

        // draw fw and back button outlines
        path.move_to({ 30, 43 });
        path.line_to({ 25, 43 });
        path.line_to({ 25, 60 });
        path.line_to({ 30, 60 });

        path.move_to({ 30, 70 });
        path.line_to({ 25, 70 });
        path.line_to({ 25, 87 });
        path.line_to({ 30, 87 });

        painter.stroke_path(path, Color::Black, 1);

        if (m_buttons & GUI::MouseButton::Left) {
            painter.fill_rect({ 31, 21, 34, 44 }, Color::Blue);
            painter.draw_triangle({ 30, 21 }, { 65, 21 }, { 65, 12 }, Color::Blue);
        }

        if (m_buttons & GUI::MouseButton::Right) {
            painter.fill_rect({ 96, 21, 34, 44 }, Color::Blue);
            painter.draw_triangle({ 96, 12 }, { 96, 21 }, { 132, 21 }, Color::Blue);
        }

        if (m_buttons & GUI::MouseButton::Middle)
            painter.fill_rect({ 66, 13, 29, 52 }, Color::Blue);

        if (m_buttons & GUI::MouseButton::Forward)
            painter.fill_rect({ 26, 44, 4, 16 }, Color::Blue);

        if (m_buttons & GUI::MouseButton::Back)
            painter.fill_rect({ 26, 71, 4, 16 }, Color::Blue);

        if (m_show_scroll_wheel) {
            auto radius = 10;
            auto off_x = 80;
            auto off_y = 38;

            Gfx::IntPoint p1;
            Gfx::IntPoint p2;
            Gfx::IntPoint p3;
            Gfx::IntPoint p4;

            p1.set_x(radius * cos(M_PI * m_wheel_delta_acc / 18) + off_x);
            p1.set_y(radius * sin(M_PI * m_wheel_delta_acc / 18) + off_y);

            p2.set_x(radius * cos(M_PI * (m_wheel_delta_acc + 18) / 18) + off_x);
            p2.set_y(radius * sin(M_PI * (m_wheel_delta_acc + 18) / 18) + off_y);

            p3.set_x(radius * cos(M_PI * (m_wheel_delta_acc + 9) / 18) + off_x);
            p3.set_y(radius * sin(M_PI * (m_wheel_delta_acc + 9) / 18) + off_y);

            p4.set_x(radius * cos(M_PI * (m_wheel_delta_acc + 27) / 18) + off_x);
            p4.set_y(radius * sin(M_PI * (m_wheel_delta_acc + 27) / 18) + off_y);

            painter.draw_line(p1, p2, Color::Red, 2);
            painter.draw_line(p3, p4, Color::Red, 2);
        }
    }

    void mousedown_event(GUI::MouseEvent& event) override
    {
        m_buttons = event.buttons();
        update();
    }

    void mouseup_event(GUI::MouseEvent& event) override
    {
        m_buttons = event.buttons();
        update();
    }

    void mousewheel_event(GUI::MouseEvent& event) override
    {
        m_wheel_delta_acc = (m_wheel_delta_acc + event.wheel_delta() + 36) % 36;
        m_show_scroll_wheel = true;
        update();
        if (!has_timer())
            start_timer(500);
    }

private:
    unsigned m_buttons;
    unsigned m_wheel_delta_acc;
    bool m_show_scroll_wheel;
    MainFrame()
        : m_buttons { 0 }
        , m_wheel_delta_acc { 0 }
        , m_show_scroll_wheel { false }
    {
    }
};

int main(int argc, char** argv)
{
    auto app = GUI::Application::construct(argc, argv);
    auto window = GUI::Window::construct();
    window->set_title("Mouse button demo");
    window->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-mouse.png"));
    window->resize(160, 155);

    auto& main_widget = window->set_main_widget<MainFrame>();
    main_widget.set_fill_with_background_color(true);

    auto menubar = GUI::MenuBar::construct();
    auto& app_menu = menubar->add_menu("Mouse Demo");
    app_menu.add_action(GUI::CommonActions::make_quit_action([&](auto&) { app->quit(); }));

    auto& help_menu = menubar->add_menu("Help");
    help_menu.add_action(GUI::Action::create("About", [&](auto&) {
        GUI::AboutDialog::show("Mouse Demo", Gfx::Bitmap::load_from_file("/res/icons/32x32/app-mouse.png"), window);
    }));

    app->set_menubar(move(menubar));
    window->set_resizable(false);
    window->show();
    return app->exec();
}
