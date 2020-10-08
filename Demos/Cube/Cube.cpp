/*
 * Copyright (c) 2020, Stephan Unverwerth <s.unverwerth@gmx.de>
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
#include <LibGUI/Application.h>
#include <LibGUI/Label.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Matrix4x4.h>
#include <LibGfx/Vector3.h>
#include <math.h>
#include <stdio.h>
#include <stdlib.h>
#include <time.h>

const int WIDTH = 200;
const int HEIGHT = 200;

class Cube final : public GUI::Widget {
    C_OBJECT(Cube)
public:
    virtual ~Cube() override;
    void set_stat_label(RefPtr<GUI::Label> l) { m_stats = l; };

private:
    Cube();
    RefPtr<Gfx::Bitmap> m_bitmap;
    RefPtr<GUI::Label> m_stats;

    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void timer_event(Core::TimerEvent&) override;

    int m_accumulated_time;
    int m_cycles;
    int m_phase;
};

Cube::Cube()
{
    m_bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::RGB32, { WIDTH, HEIGHT });

    m_accumulated_time = 0;
    m_cycles = 0;
    m_phase = 0;

    stop_timer();
    start_timer(20);
}

Cube::~Cube()
{
}

void Cube::paint_event(GUI::PaintEvent& event)
{
    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());

    /* Blit it! */
    painter.draw_scaled_bitmap(event.rect(), *m_bitmap, m_bitmap->rect());
}

void Cube::timer_event(Core::TimerEvent&)
{
    Core::ElapsedTimer timer;
    timer.start();

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

    auto matrix = FloatMatrix4x4::translate(FloatVector3(0, 0, 1.5f))
        * FloatMatrix4x4::rotate(FloatVector3(1, 0, 0), angle * 1.17356641f)
        * FloatMatrix4x4::rotate(FloatVector3(0, 1, 0), angle * 0.90533273f)
        * FloatMatrix4x4::rotate(FloatVector3(0, 0, 1), angle);

    for (int i = 0; i < 8; i++) {
        transformed_vertices[i] = matrix.transform_point(vertices[i]);
    }

    GUI::Painter painter(*m_bitmap);
    painter.fill_rect_with_gradient(Gfx::Orientation::Vertical, m_bitmap->rect(), Gfx::Color::White, Gfx::Color::Blue);

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
        a.set_x(WIDTH / 2 + a.x() / (1 + a.z() * 0.35) * WIDTH / 3);
        a.set_y(HEIGHT / 2 - a.y() / (1 + a.z() * 0.35) * WIDTH / 3);
        b.set_x(WIDTH / 2 + b.x() / (1 + b.z() * 0.35) * WIDTH / 3);
        b.set_y(HEIGHT / 2 - b.y() / (1 + b.z() * 0.35) * WIDTH / 3);
        c.set_x(WIDTH / 2 + c.x() / (1 + c.z() * 0.35) * WIDTH / 3);
        c.set_y(HEIGHT / 2 - c.y() / (1 + c.z() * 0.35) * WIDTH / 3);

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

int main(int argc, char** argv)
{
    auto app = GUI::Application::construct(argc, argv);

    auto window = GUI::Window::construct();
    window->set_double_buffering_enabled(true);
    window->set_title("Cube");
    window->set_resizable(false);
    window->resize(WIDTH, HEIGHT);

    auto& cube = window->set_main_widget<Cube>();

    auto& time = cube.add<GUI::Label>();
    time.set_relative_rect({ 0, 4, 40, 10 });
    time.move_by({ window->width() - time.width(), 0 });
    cube.set_stat_label(time);

    window->show();
    window->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-cube.png"));

    return app->exec();
}
