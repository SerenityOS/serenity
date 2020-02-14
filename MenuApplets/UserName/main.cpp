/*
 * Copyright (c) 2020-2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
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

#include <LibGUI/Application.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibGfx/Font.h>
#include <LibGfx/Palette.h>
#include <stdio.h>

class UserNameWidget final : public GUI::Widget {
    C_OBJECT(UserNameWidget)
public:
    UserNameWidget()
        : GUI::Widget(nullptr)
    {
        m_username = getlogin();
        m_username_width = Gfx::Font::default_bold_font().width(m_username);
    }

    virtual ~UserNameWidget() override {}

    int get_width()
    {
        return m_username_width + menubar_menu_margin();
    }

private:
    static int menubar_menu_margin() { return 4; }

    virtual void paint_event(GUI::PaintEvent& event) override
    {
        GUI::Painter painter(*this);
        painter.fill_rect(event.rect(), palette().window());
        painter.draw_text(event.rect(), m_username, Gfx::Font::default_bold_font(), Gfx::TextAlignment::Center, palette().window_text());
    }

    String m_username;
    int m_username_width;
};

int main(int argc, char** argv)
{
    if (pledge("stdio shared_buffer rpath cpath unix fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/res", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/tmp", "rwc") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/etc/passwd", "r") < 0) {
        perror("unveil");
        return 1;
    }

    unveil(nullptr, nullptr);

    GUI::Application app(argc, argv);

    auto window = GUI::Window::construct();
    window->set_title("UserName");
    window->set_window_type(GUI::WindowType::MenuApplet);

    auto widget = UserNameWidget::construct();

    window->resize(widget->get_width(), 16);
    window->set_main_widget(widget);
    window->show();

    if (pledge("stdio shared_buffer rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    return app.exec();
}
