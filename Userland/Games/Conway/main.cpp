/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Game.h"
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/Window.h>
#include <stdio.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio rpath wpath cpath recvfd sendfd accept cpath unix fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

    if (pledge("stdio rpath recvfd sendfd accept", nullptr) < 0) {
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

    auto app_icon = GUI::Icon::default_icon("app-conway");

    auto window = GUI::Window::construct();

    window->set_title("Conway");
    window->resize(400, 400);
    window->set_double_buffering_enabled(true);
    window->set_icon(app_icon.bitmap_for_size(16));

    auto& game = window->set_main_widget<Game>();
    window->set_minimum_size(game.columns(), game.rows());

    auto menubar = GUI::Menubar::construct();

    auto& game_menu = menubar->add_menu("Game");

    game_menu.add_action(GUI::Action::create("Reset", { Mod_None, Key_F2 }, [&](auto&) {
        game.reset();
    }));
    game_menu.add_separator();
    game_menu.add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the()->quit();
    }));

    auto& help_menu = menubar->add_menu("Help");
    help_menu.add_action(GUI::CommonActions::make_about_action("Conway", app_icon, window));

    window->set_menubar(move(menubar));

    window->show();

    return app->exec();
}
