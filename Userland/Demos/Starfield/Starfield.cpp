/*
 * Copyright (c) 2021, Jagger De Leo <jcdl@fastmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibGUI/Application.h>
#include <LibGUI/Event.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <WindowServer/Screen.h>
#include <stdio.h>
#include <time.h>
#include <unistd.h>

struct Coordinate {
    int x;
    int y;
    int z;

    operator Gfx::IntPoint() const
    {
        return { x, y };
    }
};

class Starfield final : public GUI::Widget {
    C_OBJECT(Starfield)
public:
    virtual ~Starfield() override;
    void create_stars(int, int, int);

private:
    Starfield(int);
    RefPtr<Gfx::Bitmap> m_bitmap;

    void draw();
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void timer_event(Core::TimerEvent&) override;
    virtual void keydown_event(GUI::KeyEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent& event) override;
    virtual void mousemove_event(GUI::MouseEvent& event) override;

    Vector<Coordinate> m_stars;
    int m_sweep_plane = 2000;
};

Starfield::Starfield(int interval)
{
    srand(time(nullptr));
    stop_timer();
    start_timer(interval);
}

void Starfield::create_stars(int width, int height, int stars)
{
    m_bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRx8888, { width, height });

    m_stars.grow_capacity(stars);
    for (int i = 0; i < stars; i++) {
        m_stars.append({ rand() % width - width / 2,
            rand() % height - height / 2,
            rand() % 1999 + 1 });
    }
    draw();
}

Starfield::~Starfield()
{
}

void Starfield::mousemove_event(GUI::MouseEvent&)
{
}

void Starfield::mousedown_event(GUI::MouseEvent&)
{
    ::exit(0);
}

void Starfield::keydown_event(GUI::KeyEvent&)
{
    ::exit(0);
}

void Starfield::paint_event(GUI::PaintEvent& event)
{
    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());
    painter.draw_scaled_bitmap(rect(), *m_bitmap, m_bitmap->rect());
}

void Starfield::timer_event(Core::TimerEvent&)
{
    m_bitmap->fill(Color::Black);

    auto computed_point = Gfx::IntPoint();
    auto half_x = width() / 2;
    auto half_y = height() / 2;

    for (auto star : m_stars) {
        auto z = ((star.z + m_sweep_plane) % 2000) * 0.0005;
        computed_point.set_x(half_x + star.x / z);
        computed_point.set_y(half_y + star.y / z);

        if (computed_point.x() < 0 || computed_point.x() >= width() || computed_point.y() < 0 || computed_point.y() >= height())
            continue;

        u8 falloff = (1 - z * z) * 255;
        m_bitmap->set_pixel(computed_point, Color(falloff, falloff, falloff));
    }
    m_sweep_plane -= 1;
    if (m_sweep_plane < 0)
        m_sweep_plane = 2000;
    update();
}

void Starfield::draw()
{
    GUI::Painter painter(*m_bitmap);
    painter.fill_rect(m_bitmap->rect(), Color::Black);
}

int main(int argc, char** argv)
{

    if (pledge("stdio recvfd sendfd rpath unix", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    int star_count = 0;
    int refresh_rate = 0;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("The classic starfield screensaver.");
    args_parser.add_option(star_count, "Number of stars to draw (default = 1000)", "stars", 'c', "number");
    args_parser.add_option(refresh_rate, "Refresh rate (default = 16)", "rate", 'r', "milliseconds");
    args_parser.parse(argc, argv);

    if (star_count == 0)
        star_count = 1000;

    if (refresh_rate == 0)
        refresh_rate = 16;

    auto app = GUI::Application::construct(argc, argv);

    if (pledge("stdio recvfd sendfd rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app_icon = GUI::Icon::default_icon("app-screensaver");
    auto window = GUI::Window::construct();

    window->set_double_buffering_enabled(true);
    window->set_title("Starfield");
    window->set_resizable(false);
    window->set_frameless(true);
    window->set_fullscreen(true);
    window->set_minimizable(false);
    window->set_icon(app_icon.bitmap_for_size(16));

    auto& starfield_window = window->set_main_widget<Starfield>(refresh_rate);
    starfield_window.set_fill_with_background_color(false);
    starfield_window.set_override_cursor(Gfx::StandardCursor::Hidden);
    starfield_window.update();
    window->show();

    starfield_window.create_stars(window->width(), window->height(), star_count);
    starfield_window.update();

    window->move_to_front();
    window->set_cursor(Gfx::StandardCursor::Hidden);
    window->update();

    return app->exec();
}
