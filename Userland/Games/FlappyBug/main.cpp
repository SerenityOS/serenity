/*
 * Copyright (c) 2021, Mim Hufford <mim@hotmail.co.uk>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Game.h"
#include <LibCore/ConfigFile.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Window.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio rpath wpath cpath recvfd sendfd cpath unix", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);
    auto config = Core::ConfigFile::get_for_app("FlappyBug", Core::ConfigFile::AllowWriting::Yes);

    if (pledge("stdio rpath wpath cpath recvfd sendfd", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/res", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil(config->filename().characters(), "crw") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil(nullptr, nullptr) < 0) {
        perror("unveil");
        return 1;
    }

    u32 high_score = static_cast<u32>(config->read_num_entry("Game", "HighScore", 0));

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

        config->write_num_entry("Game", "HighScore", static_cast<int>(score));
        high_score = score;

        if (!config->sync())
            GUI::MessageBox::show(window, "Configuration could not be saved", "Error", GUI::MessageBox::Type::Error);

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
