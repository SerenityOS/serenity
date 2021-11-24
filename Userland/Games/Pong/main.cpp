/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Game.h"
#include <LibCore/System.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath recvfd sendfd unix", nullptr));

    auto app = TRY(GUI::Application::try_create(arguments));

    TRY(Core::System::pledge("stdio rpath recvfd sendfd", nullptr));

    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto window = TRY(GUI::Window::try_create());
    window->resize(Pong::Game::game_width, Pong::Game::game_height);
    auto app_icon = GUI::Icon::default_icon("app-pong");
    window->set_icon(app_icon.bitmap_for_size(16));
    window->set_title("Pong");
    window->set_double_buffering_enabled(false);
    window->set_main_widget<Pong::Game>();
    window->set_resizable(false);

    auto& game_menu = window->add_menu("&Game");
    game_menu.add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the()->quit();
    }));

    auto& help_menu = window->add_menu("&Help");
    help_menu.add_action(GUI::CommonActions::make_about_action("Pong", app_icon, window));

    window->show();

    return app->exec();
}
