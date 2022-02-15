/*
 * Copyright (c) 2020, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/ElapsedTimer.h>
#include <LibCore/System.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Label.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Matrix4x4.h>
#include <LibGfx/Vector3.h>
#include <LibMain/Main.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

const int WIDTH = 200;
const int HEIGHT = 200;

static bool flag_hide_window_frame = false;

class Cube final : public GUI::Widget {
    C_OBJECT(Cube)
public:
    virtual ~Cube() override = default;
    void set_stat_label(RefPtr<GUI::Label> l) { m_stats = l; };
    void set_show_window_frame(bool);
    bool show_window_frame() const { return m_show_window_frame; }

    Function<void(GUI::ContextMenuEvent&)> on_context_menu_request;

protected:
    virtual void context_menu_event(GUI::ContextMenuEvent& event) override
    {
        if (on_context_menu_request)
            on_context_menu_request(event);
    }

private:
    Cube();

    RefPtr<Gfx::Bitmap> m_bitmap;
    RefPtr<GUI::Label> m_stats;

    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void timer_event(Core::TimerEvent&) override;

    int m_accumulated_time;
    int m_cycles;
    int m_phase;
    bool m_show_window_frame { true };
};

Cube::Cube()
{
    m_bitmap = Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRx8888, { WIDTH, HEIGHT }).release_value_but_fixme_should_propagate_errors();

    m_accumulated_time = 0;
    m_cycles = 0;
    m_phase = 0;

    stop_timer();
    start_timer(20);
}

void Cube::paint_event(GUI::PaintEvent& event)
{
    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());
    painter.draw_scaled_bitmap(rect(), *m_bitmap, m_bitmap->rect());
}

void Cube::timer_event(Core::TimerEvent&)
{
    auto timer = Core::ElapsedTimer::start_new();

    const FloatVector3 vertices[8] {
        { -1, -1, -1 },
        { -1, 1, -1 },
        { 1, 1, -1 },
        { 1, -1, -1 },
        { -1, -1, 1 },
        { -1, 1, 1 },
        { 1, 1, 1 },
        { 1, -1, 1 },
    };

#define QUAD(a, b, c, d) a, b, c, c, d, a

    const int indices[] {
        QUAD(0, 1, 2, 3),
        QUAD(7, 6, 5, 4),
        QUAD(4, 5, 1, 0),
        QUAD(3, 2, 6, 7),
        QUAD(1, 5, 6, 2),
        QUAD(0, 3, 7, 4),
    };

    const Color colors[] {
        Color::Red,
        Color::Red,
        Color::Green,
        Color::Green,
        Color::Blue,
        Color::Blue,
        Color::Magenta,
        Color::Magenta,
        Color::White,
        Color::White,
        Color::Yellow,
        Color::Yellow,
    };

    FloatVector3 transformed_vertices[8];

    static float angle = 0;
    angle += 0.02f;

    auto matrix = Gfx::translation_matrix(FloatVector3(0, 0, 1.5f))
        * Gfx::rotation_matrix(FloatVector3(1, 0, 0), angle * 1.17356641f)
        * Gfx::rotation_matrix(FloatVector3(0, 1, 0), angle * 0.90533273f)
        * Gfx::rotation_matrix(FloatVector3(0, 0, 1), angle);

    for (int i = 0; i < 8; i++) {
        transformed_vertices[i] = transform_point(matrix, vertices[i]);
    }

    GUI::Painter painter(*m_bitmap);
    if (m_show_window_frame)
        painter.fill_rect_with_gradient(Gfx::Orientation::Vertical, m_bitmap->rect(), Gfx::Color::White, Gfx::Color::Blue);
    else
        painter.clear_rect(m_bitmap->rect(), Gfx::Color::Transparent);

    auto to_point = [](const FloatVector3& v) {
        return Gfx::IntPoint(v.x(), v.y());
    };

    for (size_t i = 0; i < sizeof(indices) / sizeof(indices[0]) / 3; i++) {
        auto a = transformed_vertices[indices[i * 3]];
        auto b = transformed_vertices[indices[i * 3 + 1]];
        auto c = transformed_vertices[indices[i * 3 + 2]];
        auto normal = (b - a).cross(c - a);
        normal.normalize();

        // Perspective projection
        a.set_x(WIDTH / 2 + a.x() / (1 + a.z() * 0.35f) * WIDTH / 3);
        a.set_y(HEIGHT / 2 - a.y() / (1 + a.z() * 0.35f) * WIDTH / 3);
        b.set_x(WIDTH / 2 + b.x() / (1 + b.z() * 0.35f) * WIDTH / 3);
        b.set_y(HEIGHT / 2 - b.y() / (1 + b.z() * 0.35f) * WIDTH / 3);
        c.set_x(WIDTH / 2 + c.x() / (1 + c.z() * 0.35f) * WIDTH / 3);
        c.set_y(HEIGHT / 2 - c.y() / (1 + c.z() * 0.35f) * WIDTH / 3);

        float winding = (b.x() - a.x()) * (c.y() - a.y()) - (b.y() - a.y()) * (c.x() - a.x());
        if (winding < 0)
            continue;

        float shade = 0.5f + normal.y() * 0.5f;
        auto color = colors[i];
        color.set_red(color.red() * shade);
        color.set_green(color.green() * shade);
        color.set_blue(color.blue() * shade);

        painter.draw_triangle(to_point(a), to_point(b), to_point(c), color);
    }

    if ((m_cycles % 50) == 0) {
        dbgln("{} total cycles. finished 50 in {} ms, avg {} ms", m_cycles, m_accumulated_time, m_accumulated_time / 50);
        m_stats->set_text(String::formatted("{} ms", m_accumulated_time / 50));
        m_accumulated_time = 0;
    }

    update();

    m_accumulated_time += timer.elapsed();
    m_cycles++;
}

void Cube::set_show_window_frame(bool show)
{
    if (show == m_show_window_frame)
        return;
    m_show_window_frame = show;
    m_stats->set_visible(m_show_window_frame);
    auto& w = *window();
    w.set_frameless(!m_show_window_frame);
    w.set_has_alpha_channel(!m_show_window_frame);
    w.set_alpha_hit_threshold(m_show_window_frame ? 0 : 1);
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    auto app = TRY(GUI::Application::try_create(arguments));

    TRY(Core::System::pledge("stdio recvfd sendfd rpath"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    Core::ArgsParser parser;
    parser.set_general_help("Create a window with a spinning cube.");
    parser.add_option(flag_hide_window_frame, "Hide window frame", "hide-window", 'h');
    parser.parse(arguments);

    auto window = TRY(GUI::Window::try_create());
    window->set_double_buffering_enabled(true);
    window->set_title("Cube");
    window->set_resizable(false);
    window->resize(WIDTH, HEIGHT);
    window->set_has_alpha_channel(true);
    window->set_alpha_hit_threshold(1);

    auto cube = TRY(window->try_set_main_widget<Cube>());

    auto time = TRY(cube->try_add<GUI::Label>());
    time->set_relative_rect({ 0, 4, 40, 10 });
    time->move_by({ window->width() - time->width(), 0 });
    cube->set_stat_label(time);

    auto app_icon = GUI::Icon::default_icon("app-cube");
    window->set_icon(app_icon.bitmap_for_size(16));

    auto file_menu = TRY(window->try_add_menu("&File"));
    auto show_window_frame_action = GUI::Action::create_checkable("Show Window &Frame", [&](auto& action) {
        cube->set_show_window_frame(action.is_checked());
    });

    cube->set_show_window_frame(!flag_hide_window_frame);
    show_window_frame_action->set_checked(cube->show_window_frame());
    TRY(file_menu->try_add_action(move(show_window_frame_action)));
    TRY(file_menu->try_add_separator());
    TRY(file_menu->try_add_action(GUI::CommonActions::make_quit_action([&](auto&) { app->quit(); })));
    auto help_menu = TRY(window->try_add_menu("&Help"));
    TRY(help_menu->try_add_action(GUI::CommonActions::make_about_action("Cube Demo", app_icon, window)));

    cube->on_context_menu_request = [&](auto& event) {
        file_menu->popup(event.screen_position());
    };

    window->show();

    return app->exec();
}
