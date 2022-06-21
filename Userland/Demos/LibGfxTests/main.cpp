/*
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
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
#include <LibGfx/CharacterBitmap.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibGfx/Painter.h>
#include <LibGfx/Path.h>
#include <LibMain/Main.h>
#include <unistd.h>

int const WIDTH = 80;
int const HEIGHT = 80;

class Canvas final : public GUI::Widget {
    C_OBJECT(Canvas)
public:
    virtual ~Canvas() override = default;

private:
    Canvas();
    virtual void paint_event(GUI::PaintEvent&) override;
};

Canvas::Canvas()
{
}

void Canvas::paint_event(GUI::PaintEvent& event)
{
    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());

    dbgln("$$$$$$ drawing with scale {} {}", painter.scale(), painter.target()->scale());

    // painter.fill_rect_with_dither_pattern({0, 0, 80, 80}, Color::Red, Color::MidRed);

    Gfx::IntRect rect_a = {{0, 0}, this->size()};
    painter.fill_rect_with_checkerboard(rect_a, { 1, 2 }, Color::MidBlue, Color::Blue);
    painter.fill_ellipse(rect_a, Color::MidGray);

    static Gfx::CharacterBitmap char_bitmap {
        "           "
        "     #     "
        "    # #    "
        "     #     "
        "    # #    "
        "   #   #   "
        " ##     ## "
        "           ",
        11, 8
    };

    painter.draw_bitmap({ 20, 20 }, char_bitmap, Color::White);

    painter.draw_text(
        rect_a,
        "Test und so \n ≥0 is a normal value.. or so; this is just some text, whatever",
        Gfx::TextAlignment::TopLeft,
        Color::DarkGreen,
        Gfx::TextElision::None,
        Gfx::TextWrapping::Wrap);

    painter.fill_rect({rect_a.right() - 16, rect_a.bottom() - 16, 16, 16}, Color::White);

    painter.draw_text(
        rect_a,
        (painter.scale() == 1 ? "1" : "2"),
        Gfx::TextAlignment::BottomRight,
        Color::DarkRed,
        Gfx::TextElision::None,
        Gfx::TextWrapping::Wrap);

    /*
    static const AK::Vector<Gfx::IntPoint, 3> s_up_arrow_coords {
        { 5, 3 },
        { 1, 7 },
        { 9, 7 },
    };

    static const AK::Vector<Gfx::IntPoint, 3> s_down_arrow_coords {
        { 1, 3 },
        { 9, 3 },
        { 5, 7 },
    };

    static const AK::Vector<Gfx::IntPoint, 3> s_left_arrow_coords {
        { 7, 1 },
        { 3, 5 },
        { 7, 9 },
    };

    static const AK::Vector<Gfx::IntPoint, 3> s_right_arrow_coords {
        { 3, 1 },
        { 7, 5 },
        { 3, 9 },
    };

    painter.fill_rect_with_dither_pattern({4, 4, 10, 10}, Color::DarkBlue, Color::MidBlue);
    painter.draw_triangle({4, 4}, s_up_arrow_coords, Color::White);

    painter.fill_rect_with_dither_pattern({16, 4, 10, 10}, Color::DarkBlue, Color::MidBlue);
    painter.draw_triangle({16, 4}, s_down_arrow_coords, Color::White);

    painter.fill_rect_with_dither_pattern({4, 16, 10, 10}, Color::DarkBlue, Color::MidBlue);
    painter.draw_triangle({4, 16}, s_left_arrow_coords, Color::White);

    painter.fill_rect_with_dither_pattern({16, 16, 10, 10}, Color::DarkBlue, Color::MidBlue);
    painter.draw_triangle({16, 16}, s_right_arrow_coords, Color::White);
    */
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    auto app = TRY(GUI::Application::try_create(arguments));

    TRY(Core::System::pledge("stdio recvfd sendfd rpath"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto window = TRY(GUI::Window::try_create());
    window->set_double_buffering_enabled(true);
    window->set_title("LibGfx Tests");
    window->set_resizable(true);
    window->resize(WIDTH, HEIGHT);
    window->set_supported_scale_factors({1, 2});

    auto file_menu = TRY(window->try_add_menu("&File"));
    TRY(file_menu->try_add_action(GUI::CommonActions::make_quit_action([&](auto&) { app->quit(); })));

    auto app_icon = TRY(GUI::Icon::try_create_default_icon("app-libgfx-demo"));
    window->set_icon(app_icon.bitmap_for_size(16));
    (void)TRY(window->try_set_main_widget<Canvas>());
    window->show();

    return app->exec();
}
