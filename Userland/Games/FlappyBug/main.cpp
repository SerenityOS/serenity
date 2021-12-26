/*
 * Copyright (c) 2021, Mim Hufford <mim@hotmail.co.uk>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Game.h"
#include <LibConfig/Client.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Window.h>
#include <LibMain/Main.h>
#include <LibSystem/Wrappers.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(System::pledge("stdio rpath recvfd sendfd unix", nullptr));

    auto app = GUI::Application::construct(arguments.argc, arguments.argv);

    Config::pledge_domains("FlappyBug");

    TRY(System::pledge("stdio rpath recvfd sendfd", nullptr));

    TRY(System::unveil("/res", "r"));
    TRY(System::unveil(nullptr, nullptr));

    u32 high_score = Config::read_i32("FlappyBug", "Game", "HighScore", 0);

    auto window = GUI::Window::construct();
    window->resize(FlappyBug::Game::game_width, FlappyBug::Game::game_height);
    auto app_icon = GUI::Icon::default_icon("app-flappybug");
    window->set_icon(app_icon.bitmap_for_size(16));
    window->set_title("Flappy Bug");
    window->set_double_buffering_enabled(false);
    window->set_resizable(false);
    auto& widget = window->set_main_widget<FlappyBug::Game>();

    widget.on_game_end = [&](u32 score) {
        if (score <= high_score)
            return high_score;

        Config::write_i32("FlappyBug", "Game", "HighScore", score);
        high_score = score;

        return high_score;
    };

    auto& game_menu = window->add_menu("&Game");
    game_menu.add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the()->quit();
    }));

    auto& help_menu = window->add_menu("&Help");
    help_menu.add_action(GUI::CommonActions::make_about_action("Flappy Bug", app_icon, window));

    window->show();

    return app->exec();
}
