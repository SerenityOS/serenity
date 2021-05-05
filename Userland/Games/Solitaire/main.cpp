/*
 * Copyright (c) 2020, Till Mayer <till.mayer@web.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Game.h"
#include <Games/Solitaire/SolitaireGML.h>
#include <LibCore/Timer.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/Statusbar.h>
#include <LibGUI/Window.h>
#include <stdio.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    auto app = GUI::Application::construct(argc, argv);
    auto app_icon = GUI::Icon::default_icon("app-solitaire");

    if (pledge("stdio recvfd sendfd rpath", nullptr) < 0) {
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

    auto window = GUI::Window::construct();
    window->set_title("Solitaire");

    auto& widget = window->set_main_widget<GUI::Widget>();
    widget.load_from_gml(solitaire_gml);

    auto& game = *widget.find_descendant_of_type_named<Solitaire::Game>("game");
    game.set_focus(true);

    auto& statusbar = *widget.find_descendant_of_type_named<GUI::Statusbar>("statusbar");
    statusbar.set_text(0, "Score: 0");
    statusbar.set_text(1, "Time: 00:00:00");

    app->on_action_enter = [&](GUI::Action& action) {
        auto text = action.status_tip();
        if (text.is_empty())
            text = Gfx::parse_ampersand_string(action.text());
        statusbar.set_override_text(move(text));
    };

    app->on_action_leave = [&](GUI::Action&) {
        statusbar.set_override_text({});
    };

    game.on_score_update = [&](uint32_t score) {
        statusbar.set_text(0, String::formatted("Score: {}", score));
    };

    uint64_t seconds_elapsed = 0;

    auto timer = Core::Timer::create_repeating(1000, [&]() {
        ++seconds_elapsed;

        uint64_t hours = seconds_elapsed / 3600;
        uint64_t minutes = (seconds_elapsed / 60) % 60;
        uint64_t seconds = seconds_elapsed % 60;

        statusbar.set_text(1, String::formatted("Time: {:02}:{:02}:{:02}", hours, minutes, seconds));
    });

    game.on_game_start = [&]() {
        seconds_elapsed = 0;
        timer->start();
    };
    game.on_game_end = [&]() {
        if (timer->is_active())
            timer->stop();
    };

    auto menubar = GUI::Menubar::construct();
    auto& game_menu = menubar->add_menu("&Game");

    game_menu.add_action(GUI::Action::create("&New Game", { Mod_None, Key_F2 }, [&](auto&) {
        game.setup();
    }));
    game_menu.add_separator();
    game_menu.add_action(GUI::CommonActions::make_quit_action([&](auto&) { app->quit(); }));

    auto& help_menu = menubar->add_menu("Help");
    help_menu.add_action(GUI::CommonActions::make_about_action("Solitaire", app_icon, window));

    window->set_resizable(false);
    window->resize(Solitaire::Game::width, Solitaire::Game::height + statusbar.max_height());
    window->set_menubar(move(menubar));
    window->set_icon(app_icon.bitmap_for_size(16));
    window->show();
    game.setup();

    return app->exec();
}
