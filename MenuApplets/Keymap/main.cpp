/*
 * Copyright (c) 2020, the SerenityOS developers.
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
#include <LibGUI/Window.h>
#include <LibGfx/Font.h>
#include <LibGfx/Palette.h>
#include <LibKeyboard/CharacterMap.h>
#include <serenity.h>
#include <spawn.h>
#include <stdio.h>
#include <time.h>

class KeymapWidget final : public GUI::Widget {
    C_OBJECT(KeymapWidget)
public:
    KeymapWidget()
    {
        update_keymap_name();
        start_timer(5000);
    }

    virtual ~KeymapWidget() override { }

    int get_width()
    {
        return m_keymap_name_width + menubar_menu_margin();
    }

private:
    static int menubar_menu_margin() { return 4; }

    virtual void paint_event(GUI::PaintEvent& event) override
    {
        GUI::Painter painter(*this);
        painter.fill_rect(event.rect(), palette().window());
        painter.draw_text(event.rect(), m_keymap_name, Gfx::Font::default_bold_font(), Gfx::TextAlignment::Center, palette().window_text());
    }

    virtual void mousedown_event(GUI::MouseEvent& event) override
    {
        if (event.button() != GUI::MouseButton::Left)
            return;
        pid_t child_pid;
        const char* argv[] = { "KeyboardSettings", nullptr };
        if ((errno = posix_spawn(&child_pid, "/bin/KeyboardSettings", nullptr, nullptr, const_cast<char**>(argv), environ))) {
            perror("posix_spawn");
        } else {
            if (disown(child_pid) < 0)
                perror("disown");
        }
    }

    virtual void timer_event(Core::TimerEvent&) override
    {
        update_keymap_name();
    }

    void update_keymap_name()
    {
        char buf[24];
        if (get_keymap_name(buf, 24) < 0) {
            perror("get_keymap_name");
            m_keymap_name = "??";
            return;
        }

        String new_name = String(buf).substring(0, 2).to_uppercase();
        if (new_name != m_keymap_name) {
            m_keymap_name = new_name;
            m_keymap_name_width = Gfx::Font::default_bold_font().width(m_keymap_name);
            update();
        }
    }

    String m_keymap_name;
    int m_keymap_name_width;
};

int main(int argc, char** argv)
{
    if (pledge("stdio shared_buffer accept rpath unix cpath fattr exec proc", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

    if (pledge("stdio shared_buffer accept rpath exec proc", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto window = GUI::Window::construct();
    window->set_title("Clock");
    window->set_window_type(GUI::WindowType::MenuApplet);

    auto& widget = window->set_main_widget<KeymapWidget>();
    window->resize(widget.get_width(), 16);
    window->show();

    if (unveil("/res", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/bin/KeyboardSettings", "x") < 0) {
        perror("unveil");
        return 1;
    }

    unveil(nullptr, nullptr);

    return app->exec();
}
