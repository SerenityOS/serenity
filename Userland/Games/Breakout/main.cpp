/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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
    TRY(Core::System::pledge("stdio recvfd sendfd rpath unix"));

    auto app = TRY(GUI::Application::try_create(arguments));

    TRY(Core::System::pledge("stdio recvfd sendfd rpath"));

    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto window = TRY(GUI::Window::try_create());
    window->resize(Breakout::Game::game_width, Breakout::Game::game_height);
    window->set_resizable(false);
    window->set_double_buffering_enabled(false);
    window->set_title("Breakout");

    auto app_icon = GUI::Icon::default_icon("app-breakout");
    window->set_icon(app_icon.bitmap_for_size(16));

    auto game = TRY(window->try_set_main_widget<Breakout::Game>());

    auto game_menu = TRY(window->try_add_menu("&Game"));
    TRY(game_menu->try_add_action(GUI::Action::create_checkable("&Pause", { {}, Key_P }, [&](auto& action) {
        game->set_paused(action.is_checked());
    })));

    TRY(game_menu->try_add_separator());
    TRY(game_menu->try_add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the()->quit();
    })));

    auto help_menu = TRY(window->try_add_menu("&Help"));
    TRY(help_menu->try_add_action(GUI::CommonActions::make_about_action("Breakout", app_icon, window)));

    window->show();

    return app->exec();
}
