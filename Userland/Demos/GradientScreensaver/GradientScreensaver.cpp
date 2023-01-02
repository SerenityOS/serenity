/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/System.h>
#include <LibDesktop/Screensaver.h>
#include <LibGUI/Application.h>
#include <LibGUI/Event.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <LibMain/Main.h>
#include <stdio.h>
#include <time.h>
#include <unistd.h>

class Screensaver final : public Desktop::Screensaver {
    C_OBJECT(Screensaver)
public:
    virtual ~Screensaver() override = default;

private:
    Screensaver(int width = 64, int height = 48, int interval = 10000);
    RefPtr<Gfx::Bitmap> m_bitmap;

    void draw();
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void timer_event(Core::TimerEvent&) override;
};

Screensaver::Screensaver(int width, int height, int interval)
{
    on_screensaver_exit = []() { GUI::Application::the()->quit(); };
    m_bitmap = Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRx8888, { width, height }).release_value_but_fixme_should_propagate_errors();
    srand(time(nullptr));
    stop_timer();
    start_timer(interval);
    draw();
}

void Screensaver::paint_event(GUI::PaintEvent& event)
{
    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());
    painter.draw_scaled_bitmap(rect(), *m_bitmap, m_bitmap->rect());
}

void Screensaver::timer_event(Core::TimerEvent&)
{
    draw();
    update();
}

void Screensaver::draw()
{
    const Color colors[] {
        Color::Blue,
        Color::Cyan,
        Color::Green,
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
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath recvfd sendfd unix"));

    auto app = TRY(GUI::Application::try_create(arguments));

    TRY(Core::System::pledge("stdio rpath recvfd sendfd"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto window = TRY(Desktop::Screensaver::create_window("Screensaver"sv, "app-screensaver"sv));

    auto screensaver_window = TRY(window->try_set_main_widget<Screensaver>(64, 48, 10000));
    screensaver_window->set_fill_with_background_color(false);
    screensaver_window->set_override_cursor(Gfx::StandardCursor::Hidden);
    screensaver_window->update();

    window->show();
    window->move_to_front();
    window->set_cursor(Gfx::StandardCursor::Hidden);
    window->update();

    return app->exec();
}
