/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SnakeGame.h"
#include <LibConfig/Client.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Window.h>
#include <stdio.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio rpath recvfd sendfd unix", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

    Config::pledge_domains("Snake");

    if (pledge("stdio rpath recvfd sendfd", nullptr) < 0) {
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

    auto app_icon = GUI::Icon::default_icon("app-snake");

    auto window = GUI::Window::construct();

    window->set_double_buffering_enabled(false);
    window->set_title("Snake");
    window->resize(324, 344);

    auto& game = window->set_main_widget<SnakeGame>();

    auto& game_menu = window->add_menu("&Game");

    game_menu.add_action(GUI::Action::create("&New Game", { Mod_None, Key_F2 }, [&](auto&) {
        game.reset();
    }));
    game_menu.add_separator();
    game_menu.add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the()->quit();
    }));

    auto& help_menu = window->add_menu("&Help");
    help_menu.add_action(GUI::CommonActions::make_about_action("Snake", app_icon, window));

    window->show();

    window->set_icon(app_icon.bitmap_for_size(16));

    return app->exec();
}
