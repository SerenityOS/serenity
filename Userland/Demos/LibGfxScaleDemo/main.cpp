/*
 * Copyright (c) 2020, Nico Weber <thakis@chromium.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/System.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Font/Font.h>
#include <LibGfx/Painter.h>
#include <LibGfx/Palette.h>
#include <LibGfx/Path.h>
#include <LibGfx/SystemTheme.h>
#include <LibGfx/WindowTheme.h>
#include <LibMain/Main.h>
#include <unistd.h>

int const WIDTH = 300;
int const HEIGHT = 200;

class Canvas final : public GUI::Widget {
    C_OBJECT(Canvas)
public:
    virtual ~Canvas() override = default;

private:
    Canvas();
    RefPtr<Gfx::Bitmap> m_bitmap_1x;
    RefPtr<Gfx::Bitmap> m_bitmap_2x;
    RefPtr<Gfx::Bitmap> m_bitmap_2x_as_1x;

    void draw(Gfx::Painter& painter);
    virtual void paint_event(GUI::PaintEvent&) override;
};

Canvas::Canvas()
{
    m_bitmap_1x = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, { WIDTH, HEIGHT }, 1).release_value_but_fixme_should_propagate_errors();
    m_bitmap_2x = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, { WIDTH, HEIGHT }, 2).release_value_but_fixme_should_propagate_errors();

    // m_bitmap_1x and m_bitmap_2x have the same logical size, so LibGfx will try to draw them at the same physical size:
    // When drawing on a 2x backing store it'd scale m_bitmap_1x up 2x and paint m_bitmap_2x at its physical size.
    // When drawing on a 1x backing store it'd draw m_bitmap_1x at its physical size, and it would have to scale down m_bitmap_2x to 0.5x its size.
    // But the system can't current scale down, and we want to draw the 2x bitmap at twice the size of the 1x bitmap in this particular application,
    // so make a 1x alias of the 2x bitmap to make LibGfx paint it without any scaling at paint time, mapping once pixel to one pixel.
    m_bitmap_2x_as_1x = Gfx::Bitmap::create_wrapper(Gfx::BitmapFormat::BGRA8888, m_bitmap_2x->physical_size(), 1, m_bitmap_2x->pitch(), m_bitmap_2x->scanline(0)).release_value_but_fixme_should_propagate_errors();

    Gfx::Painter painter_1x(*m_bitmap_1x);
    draw(painter_1x);

    Gfx::Painter painter_2x(*m_bitmap_2x);
    draw(painter_2x);

    update();
}

void Canvas::paint_event(GUI::PaintEvent& event)
{
    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());
    painter.fill_rect(event.rect(), Color::Magenta);
    painter.blit({ 0, 0 }, *m_bitmap_1x, m_bitmap_1x->rect());
    painter.blit({ 0, HEIGHT }, *m_bitmap_2x_as_1x, m_bitmap_2x_as_1x->rect());
}

void Canvas::draw(Gfx::Painter& painter)
{
    auto active_window_icon = Gfx::Bitmap::load_from_file("/res/icons/16x16/window.png"sv).release_value_but_fixme_should_propagate_errors();

    palette().window_theme().paint_normal_frame(painter, Gfx::WindowTheme::WindowState::Active, Gfx::WindowTheme::WindowMode::Other, { 4, 18, WIDTH - 8, HEIGHT - 29 }, "Well hello friends 🐞"sv, *active_window_icon, palette(), { WIDTH - 20, 6, 16, 16 }, 0, false);

    painter.fill_rect({ 4, 25, WIDTH - 8, HEIGHT - 30 }, palette().color(Gfx::ColorRole::Background));
    painter.draw_rect({ 20, 34, WIDTH - 40, HEIGHT - 45 }, palette().color(Gfx::ColorRole::Selection), true);
    painter.draw_rect({ 24, 38, WIDTH - 48, HEIGHT - 53 }, palette().color(Gfx::ColorRole::Selection));

    // buggie.png has an alpha channel.
    auto buggie = Gfx::Bitmap::load_from_file("/res/graphics/buggie.png"sv).release_value_but_fixme_should_propagate_errors();
    painter.blit({ 25, 39 }, *buggie, { 2, 30, 62, 20 });
    painter.draw_scaled_bitmap({ 88, 39, 62 * 2, 20 * 2 }, *buggie, Gfx::IntRect { 2, 30, 62, 20 });
    painter.draw_scaled_bitmap({ 202, 39, 80, 40 }, *buggie, Gfx::IntRect { 2, 30, 62, 20 });

    painter.draw_tiled_bitmap({ 25, 60, WIDTH - 50, 40 }, *buggie);

    painter.blit({ 25, 101 }, *buggie, { 2, 30, 3 * buggie->width(), 20 });

    // grid does not have an alpha channel.
    auto grid = Gfx::Bitmap::load_from_file("/res/wallpapers/grid.png"sv).release_value_but_fixme_should_propagate_errors();
    VERIFY(!grid->has_alpha_channel());
    painter.fill_rect({ 25, 122, 62, 20 }, Color::Green);
    painter.blit({ 25, 122 }, *grid, { (grid->width() - 62) / 2, (grid->height() - 20) / 2 + 40, 62, 20 }, 0.9);

    painter.blit_brightened({ 88, 122 }, *buggie, { 2, 30, 62, 20 });
    painter.blit_dimmed({ 140, 122 }, *buggie, { 2, 30, 62, 20 });
    painter.blit_disabled({ 192, 122 }, *buggie, { 2, 30, 62, 20 }, palette());
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    auto app = TRY(GUI::Application::create(arguments));

    TRY(Core::System::pledge("stdio recvfd sendfd rpath"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto window = GUI::Window::construct();
    window->set_title("LibGfx Scale Demo");
    window->set_resizable(false);
    window->resize(WIDTH * 2, HEIGHT * 3);

    auto file_menu = window->add_menu("&File"_string);
    file_menu->add_action(GUI::CommonActions::make_quit_action([&](auto&) { app->quit(); }));

    auto view_menu = window->add_menu("&View"_string);
    view_menu->add_action(GUI::CommonActions::make_fullscreen_action([&](auto&) {
        window->set_fullscreen(!window->is_fullscreen());
    }));

    auto app_icon = TRY(GUI::Icon::try_create_default_icon("app-libgfx-demo"sv));
    window->set_icon(app_icon.bitmap_for_size(16));
    (void)window->set_main_widget<Canvas>();
    window->show();

    return app->exec();
}
