/*
 * Copyright (c) 2021, Mim Hufford <mim@hotmail.co.uk>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Game.h"
#include <LibConfig/Client.h>
#include <LibCore/System.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Window.h>
#include <LibMain/Main.h>
#include <LibURL/URL.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath recvfd sendfd unix"));

    auto app = TRY(GUI::Application::create(arguments));

    Config::pledge_domain("FlappyBug");

    TRY(Desktop::Launcher::add_allowed_handler_with_only_specific_urls("/bin/Help", { URL::create_with_file_scheme("/usr/share/man/man6/FlappyBug.md") }));
    TRY(Desktop::Launcher::seal_allowlist());

    TRY(Core::System::pledge("stdio rpath recvfd sendfd"));

    TRY(Core::System::unveil("/tmp/session/%sid/portal/launch", "rw"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    u32 high_score = Config::read_i32("FlappyBug"sv, "Game"sv, "HighScore"sv, 0);

    auto window = GUI::Window::construct();
    window->resize(FlappyBug::Game::game_width, FlappyBug::Game::game_height);
    auto app_icon = TRY(GUI::Icon::try_create_default_icon("app-flappybug"sv));
    window->set_icon(app_icon.bitmap_for_size(16));
    window->set_title("Flappy Bug");
    window->set_double_buffering_enabled(false);
    window->set_resizable(false);
    auto widget = window->set_main_widget<FlappyBug::Game>(TRY(FlappyBug::Game::Bug::construct()), TRY(FlappyBug::Game::Cloud::construct()));

    widget->on_game_end = [&](u32 score) {
        if (score <= high_score)
            return high_score;

        Config::write_i32("FlappyBug"sv, "Game"sv, "HighScore"sv, score);
        high_score = score;

        return high_score;
    };

    auto game_menu = window->add_menu("&Game"_string);
    game_menu->add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the()->quit();
    }));

    auto view_menu = window->add_menu("&View"_string);
    view_menu->add_action(GUI::CommonActions::make_fullscreen_action([&](auto&) {
        window->set_fullscreen(!window->is_fullscreen());
    }));

    auto help_menu = window->add_menu("&Help"_string);
    help_menu->add_action(GUI::CommonActions::make_command_palette_action(window));
    help_menu->add_action(GUI::CommonActions::make_help_action([](auto&) {
        Desktop::Launcher::open(URL::create_with_file_scheme("/usr/share/man/man6/FlappyBug.md"), "/bin/Help");
    }));
    help_menu->add_action(GUI::CommonActions::make_about_action("Flappy Bug"_string, app_icon, window));

    window->show();

    return app->exec();
}
