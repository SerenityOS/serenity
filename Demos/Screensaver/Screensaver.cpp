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

#include <LibCore/ElapsedTimer.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/Event.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <stdio.h>
#include <time.h>

#define WIDTH 64
#define HEIGHT 48
#define INTERVAL 10000

class Screensaver final : public GUI::Widget {
    C_OBJECT(Screensaver)
public:
    virtual ~Screensaver() override;

private:
    Screensaver();
    RefPtr<Gfx::Bitmap> m_bitmap;

    void draw();
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void timer_event(Core::TimerEvent&) override;
    virtual void keydown_event(GUI::KeyEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent& event) override;
    virtual void mousemove_event(GUI::MouseEvent& event) override;
};

Screensaver::Screensaver()
{
    m_bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::RGB32, { WIDTH, HEIGHT });
    srand(time(nullptr));
    stop_timer();
    start_timer(INTERVAL);
    draw();
}

Screensaver::~Screensaver()
{
}

void Screensaver::mousemove_event(GUI::MouseEvent&)
{
    ::exit(0);
}

void Screensaver::mousedown_event(GUI::MouseEvent&)
{
    ::exit(0);
}

void Screensaver::keydown_event(GUI::KeyEvent&)
{
    ::exit(0);
}

void Screensaver::paint_event(GUI::PaintEvent& event)
{
    GUI::Painter painter(*this);
    painter.draw_scaled_bitmap(event.rect(), *m_bitmap, m_bitmap->rect());
}

void Screensaver::timer_event(Core::TimerEvent&)
{
    Core::ElapsedTimer timer;
    timer.start();
    draw();
}

void Screensaver::draw()
{
    const Color colors[] {
        Color::Blue,
        Color::Cyan,
        Color::Green,
        Color::LightGray,
        Color::Magenta,
        Color::Red,
        Color::Yellow,
    };

    const Orientation orientations[] {
        Gfx::Orientation::Horizontal,
        Gfx::Orientation::Vertical
    };

    int start_color_index = 0;
    int end_color_index = 0;
    while (start_color_index == end_color_index) {
        start_color_index = rand() % (sizeof(colors) / sizeof(colors[0]));
        end_color_index = rand() % (sizeof(colors) / sizeof(colors[0]));
    }

    GUI::Painter painter(*m_bitmap);
    painter.fill_rect_with_gradient(
        orientations[rand() % (sizeof(orientations) / sizeof(orientations[0]))],
        m_bitmap->rect(),
        colors[start_color_index],
        colors[end_color_index]);

    update();
}

int main(int argc, char** argv)
{
    auto app = GUI::Application::construct(argc, argv);

    auto window = GUI::Window::construct();
    window->set_double_buffering_enabled(true);
    window->set_title("Screensaver");
    window->set_resizable(false);
    window->set_fullscreen(true);

    auto& screensaver_window = window->set_main_widget<Screensaver>();
    screensaver_window.update();

    window->show();
    window->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-screensaver.png"));

    return app->exec();
}
