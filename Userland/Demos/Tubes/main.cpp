/*
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Demos/Tubes/Tubes.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Window.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd rpath unix prot_exec"));

    unsigned refresh_rate = 12;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Screensaver rendering colorful moving tubes using LibGL");
    args_parser.add_option(refresh_rate, "Refresh rate", "rate", 'r', "milliseconds");
    args_parser.parse(arguments);

    auto app = TRY(GUI::Application::try_create(arguments));

    TRY(Core::System::pledge("stdio recvfd sendfd rpath prot_exec"));

    auto app_icon = GUI::Icon::default_icon("app-tubes"sv);
    auto window = TRY(GUI::Window::try_create());

    window->set_double_buffering_enabled(true);
    window->set_title("Tubes");
    window->set_resizable(false);
    window->set_frameless(true);
    window->set_fullscreen(true);
    window->set_minimizable(false);
    window->set_icon(app_icon.bitmap_for_size(16));
    window->update();

    auto tubes_widget = TRY(window->try_set_main_widget<Tubes>(refresh_rate));
    tubes_widget->set_fill_with_background_color(false);
    tubes_widget->set_override_cursor(Gfx::StandardCursor::Hidden);
    window->show();

    TRY(tubes_widget->create_buffer(window->size()));
    tubes_widget->setup_view();
    tubes_widget->reset_tubes();

    window->move_to_front();
    window->set_cursor(Gfx::StandardCursor::Hidden);

    return app->exec();
}
