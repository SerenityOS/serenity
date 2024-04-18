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

class Gradient final : public Desktop::Screensaver {
    C_OBJECT(Gradient)
public:
    virtual ~Gradient() override = default;

private:
    Gradient(int width = 64, int height = 48, int interval = 10000);
    RefPtr<Gfx::Bitmap> m_bitmap;

    void draw();
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void timer_event(Core::TimerEvent&) override;
};

Gradient::Gradient(int width, int height, int interval)
{
    on_screensaver_exit = []() { GUI::Application::the()->quit(); };
    m_bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRx8888, { width, height }).release_value_but_fixme_should_propagate_errors();
    srand(time(nullptr));
    stop_timer();
    start_timer(interval);
    draw();
}

void Gradient::paint_event(GUI::PaintEvent& event)
{
    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());
    painter.draw_scaled_bitmap(rect(), *m_bitmap, m_bitmap->rect());
}

void Gradient::timer_event(Core::TimerEvent&)
{
    draw();
    update();
}

void Gradient::draw()
{
    Color const colors[] {
        Color::Blue,
        Color::Cyan,
        Color::Green,
        Color::Magenta,
        Color::Red,
        Color::Yellow,
    };

    Orientation const orientations[] {
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

    auto app = TRY(GUI::Application::create(arguments));

    TRY(Core::System::pledge("stdio rpath recvfd sendfd"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto window = TRY(Desktop::Screensaver::create_window("Gradient"sv, "app-gradient"sv));

    auto gradient_widget = window->set_main_widget<Gradient>(64, 48, 10000);
    gradient_widget->set_fill_with_background_color(false);
    gradient_widget->set_override_cursor(Gfx::StandardCursor::Hidden);
    gradient_widget->update();

    window->show();
    window->move_to_front();
    window->set_cursor(Gfx::StandardCursor::Hidden);
    window->update();

    return app->exec();
}
