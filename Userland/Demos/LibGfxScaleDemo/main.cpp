/*
 * Copyright (c) 2020, Nico Weber <thakis@chromium.org>
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
#include <LibGUI/Icon.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Font.h>
#include <LibGfx/FontDatabase.h>
#include <LibGfx/Painter.h>
#include <LibGfx/Palette.h>
#include <LibGfx/Path.h>
#include <LibGfx/WindowTheme.h>

const int WIDTH = 300;
const int HEIGHT = 200;

class Canvas final : public GUI::Widget {
    C_OBJECT(Canvas)
public:
    virtual ~Canvas() override;

private:
    Canvas();
    RefPtr<Gfx::Bitmap> m_bitmap_1x;
    RefPtr<Gfx::Bitmap> m_bitmap_2x;

    void draw(Gfx::Painter& painter);
    virtual void paint_event(GUI::PaintEvent&) override;
};

Canvas::Canvas()
{
    m_bitmap_1x = Gfx::Bitmap::create(Gfx::BitmapFormat::RGB32, { WIDTH, HEIGHT });
    m_bitmap_2x = Gfx::Bitmap::create(Gfx::BitmapFormat::RGB32, { WIDTH * 2, HEIGHT * 2 });

    Gfx::Painter painter_1x(*m_bitmap_1x);
    draw(painter_1x);

    Gfx::Painter painter_2x(*m_bitmap_2x);
    painter_2x.scale(2);
    draw(painter_2x);

    update();
}

Canvas::~Canvas()
{
}

void Canvas::paint_event(GUI::PaintEvent& event)
{
    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());
    painter.fill_rect(event.rect(), Color::Magenta);
    painter.blit({ 0, 0 }, *m_bitmap_1x, m_bitmap_1x->rect());
    painter.blit({ 0, HEIGHT }, *m_bitmap_2x, m_bitmap_2x->rect());
}

void Canvas::draw(Gfx::Painter& painter)
{
    auto active_window_icon = Gfx::Bitmap::load_from_file("/res/icons/16x16/window.png");
    Gfx::WindowTheme::current().paint_normal_frame(painter, Gfx::WindowTheme::WindowState::Active, { 4, 18, WIDTH - 8, HEIGHT - 29 }, "Well hello friends", *active_window_icon, palette(), { WIDTH - 20, 6, 16, 16 });
}

int main(int argc, char** argv)
{
    auto app = GUI::Application::construct(argc, argv);

    if (pledge("stdio rpath shared_buffer", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/res", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil(nullptr, nullptr) < 0) {
        perror("unveil");
        return 1;
    }

    auto window = GUI::Window::construct();
    window->set_title("LibGfx Scale Demo");
    window->set_resizable(false);
    window->resize(WIDTH * 2, HEIGHT * 3);

    auto app_icon = GUI::Icon::default_icon("app-libgfx-demo");
    window->set_icon(app_icon.bitmap_for_size(16));
    window->set_main_widget<Canvas>();
    window->show();

    return app->exec();
}
