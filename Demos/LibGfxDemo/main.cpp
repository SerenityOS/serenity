/*
 * Copyright (c) 2020, Linus Groh <mail@linusgroh.de>
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
#include <LibGfx/Bitmap.h>
#include <LibGfx/Font.h>
#include <LibGfx/Painter.h>
#include <LibGfx/Path.h>

const int WIDTH = 780;
const int HEIGHT = 600;

class Canvas final : public GUI::Widget {
    C_OBJECT(Canvas)
public:
    virtual ~Canvas() override;

private:
    Canvas();
    RefPtr<Gfx::Bitmap> m_bitmap;

    void draw();
    virtual void paint_event(GUI::PaintEvent&) override;
};

Canvas::Canvas()
{
    m_bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::RGB32, { WIDTH, HEIGHT });
    draw();
}

Canvas::~Canvas()
{
}

void Canvas::paint_event(GUI::PaintEvent& event)
{
    GUI::Painter painter(*this);
    painter.draw_scaled_bitmap(event.rect(), *m_bitmap, m_bitmap->rect());
}

void Canvas::draw()
{
    GUI::Painter painter(*m_bitmap);

    painter.fill_rect({ 20, 20, 100, 100 }, Color::Magenta);
    painter.draw_rect({ 20, 140, 100, 100 }, Color::Yellow);

    painter.fill_rect_with_gradient(Gfx::Orientation::Horizontal, { 140, 20, 100, 100 }, Color::Yellow, Color::DarkGreen);
    painter.fill_rect_with_gradient(Gfx::Orientation::Vertical, { 140, 140, 100, 100 }, Color::Red, Color::Blue);

    painter.fill_rect_with_dither_pattern({ 260, 20, 100, 100 }, Color::MidGray, Color::Black);
    painter.fill_rect_with_checkerboard({ 260, 140, 100, 100 }, { 10, 10 }, Color::LightGray, Color::White);

    painter.draw_line({ 430, 35 }, { 465, 70 }, Color::Green);
    painter.draw_line({ 465, 70 }, { 430, 105 }, Color::Green);
    painter.draw_line({ 430, 105 }, { 395, 70 }, Color::Green);
    painter.draw_line({ 395, 70 }, { 430, 35 }, Color::Green);
    painter.draw_rect({ 395, 35, 70, 70 }, Color::Blue);
    painter.draw_ellipse_intersecting({ 395, 35, 70, 70 }, Color::Red);
    painter.draw_rect({ 380, 20, 100, 100 }, Color::Yellow);

    painter.fill_rect({ 380, 140, 100, 100 }, Color::Blue);
    painter.draw_triangle({ 430, 140 }, { 380, 140 }, { 380, 240 }, Color::Green);
    painter.draw_triangle({ 430, 240 }, { 480, 140 }, { 480, 240 }, Color::Red);
    painter.draw_rect({ 380, 140, 100, 100 }, Color::Yellow);

    painter.draw_line({ 500, 20 }, { 750, 20 }, Color::Green, 1, Gfx::Painter::LineStyle::Solid);
    painter.draw_line({ 500, 30 }, { 750, 30 }, Color::Red, 5, Gfx::Painter::LineStyle::Solid);
    painter.draw_line({ 500, 45 }, { 750, 45 }, Color::Blue, 10, Gfx::Painter::LineStyle::Solid);

    painter.draw_line({ 500, 60 }, { 750, 60 }, Color::Green, 1, Gfx::Painter::LineStyle::Dotted);
    painter.draw_line({ 500, 70 }, { 750, 70 }, Color::Red, 5, Gfx::Painter::LineStyle::Dotted);
    painter.draw_line({ 500, 85 }, { 750, 85 }, Color::Blue, 10, Gfx::Painter::LineStyle::Dotted);

    painter.draw_line({ 500, 100 }, { 750, 100 }, Color::Green, 1, Gfx::Painter::LineStyle::Dashed);
    painter.draw_line({ 500, 110 }, { 750, 110 }, Color::Red, 5, Gfx::Painter::LineStyle::Dashed);
    painter.draw_line({ 500, 125 }, { 750, 125 }, Color::Blue, 10, Gfx::Painter::LineStyle::Dashed);

    painter.draw_line({ 500, 140 }, { 500, 240 }, Color::Green, 1, Gfx::Painter::LineStyle::Solid);
    painter.draw_line({ 510, 140 }, { 510, 240 }, Color::Red, 5, Gfx::Painter::LineStyle::Solid);
    painter.draw_line({ 525, 140 }, { 525, 240 }, Color::Blue, 10, Gfx::Painter::LineStyle::Solid);

    painter.draw_line({ 540, 140 }, { 540, 240 }, Color::Green, 1, Gfx::Painter::LineStyle::Dotted);
    painter.draw_line({ 550, 140 }, { 550, 240 }, Color::Red, 5, Gfx::Painter::LineStyle::Dotted);
    painter.draw_line({ 565, 140 }, { 565, 240 }, Color::Blue, 10, Gfx::Painter::LineStyle::Dotted);

    painter.draw_line({ 580, 140 }, { 580, 240 }, Color::Green, 1, Gfx::Painter::LineStyle::Dashed);
    painter.draw_line({ 590, 140 }, { 590, 240 }, Color::Red, 5, Gfx::Painter::LineStyle::Dashed);
    painter.draw_line({ 605, 140 }, { 605, 240 }, Color::Blue, 10, Gfx::Painter::LineStyle::Dashed);

    painter.draw_line({ 640, 190 }, { 740, 240 }, Color::Green, 1, Gfx::Painter::LineStyle::Solid);
    painter.draw_line({ 640, 140 }, { 740, 240 }, Color::Red, 5, Gfx::Painter::LineStyle::Solid);
    painter.draw_line({ 690, 140 }, { 740, 240 }, Color::Blue, 10, Gfx::Painter::LineStyle::Solid);
    painter.draw_line({ 740, 190 }, { 640, 240 }, Color::Green, 1, Gfx::Painter::LineStyle::Solid);
    painter.draw_line({ 740, 140 }, { 640, 240 }, Color::Red, 5, Gfx::Painter::LineStyle::Solid);
    painter.draw_line({ 690, 140 }, { 640, 240 }, Color::Blue, 10, Gfx::Painter::LineStyle::Solid);

    auto bg = Gfx::Bitmap::load_from_file("/res/html/misc/90s-bg.png");
    painter.draw_tiled_bitmap({ 20, 260, 480, 320 }, *bg);

    painter.draw_line({ 40, 480 }, { 20, 260 }, Color::Red);
    painter.draw_line({ 40, 480 }, { 120, 300 }, Color::Red);
    painter.draw_quadratic_bezier_curve({ 40, 480 }, { 20, 260 }, { 120, 300 }, Color::Blue);

    painter.draw_line({ 240, 280 }, { 80, 420 }, Color::Red, 3);
    painter.draw_line({ 240, 280 }, { 260, 360 }, Color::Red, 3);
    painter.draw_quadratic_bezier_curve({ 240, 280 }, { 80, 420 }, { 260, 360 }, Color::Blue, 3);

    auto path = Gfx::Path();
    path.move_to({ 60, 500 });
    path.line_to({ 90, 540 });
    path.quadratic_bezier_curve_to({ 320, 500 }, { 220, 400 });
    path.line_to({ 300, 440 });
    path.line_to({ 90, 460 });
    path.quadratic_bezier_curve_to({ 260, 500 }, { 200, 540 });
    path.close();
    painter.fill_path(path, Color::Yellow, Gfx::Painter::WindingRule::EvenOdd);

    auto buggie = Gfx::Bitmap::load_from_file("/res/graphics/buggie.png");
    painter.blit({ 280, 280 }, *buggie, buggie->rect(), 0.5);
    painter.blit_scaled({ 360, 280, buggie->rect().width() * 2, buggie->rect().height() * 2 }, *buggie, buggie->rect(), 0.5, 0.5);

    painter.draw_rect({ 20, 260, 480, 320 }, Color::DarkGray);

    painter.draw_rect({ 520, 260, 240, 80 }, Color::DarkGray);
    painter.draw_text({ 520, 260, 240, 80 }, "CenterLeft", Gfx::TextAlignment::CenterLeft, Color::White);
    painter.draw_text({ 520, 260, 240, 80 }, "Center", Gfx::TextAlignment::Center, Color::White);
    painter.draw_text({ 520, 260, 240, 80 }, "CenterRight", Gfx::TextAlignment::CenterRight, Color::White);
    painter.draw_text({ 520, 260, 240, 80 }, "TopLeft", Gfx::TextAlignment::TopLeft, Color::White);
    painter.draw_text({ 520, 260, 240, 80 }, "TopRight", Gfx::TextAlignment::TopRight, Color::White);

    painter.draw_rect({ 520, 360, 240, 30 }, Color::DarkGray);
    painter.draw_text({ 520, 360, 240, 30 }, "Emojis! 🙂😂🐞🦄", Gfx::TextAlignment::Center, Color::White);

    painter.draw_rect({ 520, 410, 240, 80 }, Color::DarkGray);
    painter.draw_text({ 520, 415, 240, 20 }, "Normal text", Gfx::Font::default_font(), Gfx::TextAlignment::CenterLeft, Color::Red);
    painter.draw_text({ 520, 430, 240, 20 }, "Bold text", Gfx::Font::default_bold_font(), Gfx::TextAlignment::CenterLeft, Color::Green);
    painter.draw_text({ 520, 450, 240, 20 }, "Normal text (fixed width)", Gfx::Font::default_fixed_width_font(), Gfx::TextAlignment::CenterLeft, Color::Blue);
    painter.draw_text({ 520, 465, 240, 20 }, "Bold text (fixed width)", Gfx::Font::default_bold_fixed_width_font(), Gfx::TextAlignment::CenterLeft, Color::Yellow);

    auto font = Gfx::Font::load_from_file("/res/fonts/PebbletonBold14.font");
    painter.draw_rect({ 520, 510, 240, 30 }, Color::DarkGray);
    painter.draw_text({ 520, 510, 240, 30 }, "Hello friends! :^)", *font, Gfx::TextAlignment::Center, Color::White);

    painter.fill_rect({ 520, 560, 10, 20 }, Color::White);
    painter.fill_rect({ 530, 560, 10, 20 }, Color::WarmGray);
    painter.fill_rect({ 540, 560, 10, 20 }, Color::LightGray);
    painter.fill_rect({ 550, 560, 10, 20 }, Color::MidGray);
    painter.fill_rect({ 560, 560, 10, 20 }, Color::DarkGray);
    painter.fill_rect({ 570, 560, 10, 20 }, Color::Black);
    painter.fill_rect({ 580, 560, 10, 20 }, Color::Blue);
    painter.fill_rect({ 590, 560, 10, 20 }, Color::MidBlue);
    painter.fill_rect({ 600, 560, 10, 20 }, Color::DarkBlue);
    painter.fill_rect({ 610, 560, 10, 20 }, Color::Cyan);
    painter.fill_rect({ 620, 560, 10, 20 }, Color::MidCyan);
    painter.fill_rect({ 630, 560, 10, 20 }, Color::DarkCyan);
    painter.fill_rect({ 640, 560, 10, 20 }, Color::Green);
    painter.fill_rect({ 650, 560, 10, 20 }, Color::MidGreen);
    painter.fill_rect({ 660, 560, 10, 20 }, Color::DarkGreen);
    painter.fill_rect({ 670, 560, 10, 20 }, Color::Yellow);
    painter.fill_rect({ 680, 560, 10, 20 }, Color::Red);
    painter.fill_rect({ 690, 560, 10, 20 }, Color::MidRed);
    painter.fill_rect({ 700, 560, 10, 20 }, Color::DarkRed);
    painter.fill_rect({ 710, 560, 10, 20 }, Color::Magenta);
    painter.fill_rect({ 720, 560, 10, 20 }, Color::MidMagenta);

    update();
}

int main(int argc, char** argv)
{
    auto app = GUI::Application::construct(argc, argv);

    auto window = GUI::Window::construct();
    window->set_double_buffering_enabled(true);
    window->set_title("LibGfx Demo");
    window->set_resizable(false);
    window->resize(WIDTH, HEIGHT);
    window->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-libgfx-demo.png"));
    window->set_main_widget<Canvas>();
    window->show();

    return app->exec();
}
