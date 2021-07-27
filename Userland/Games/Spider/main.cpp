/*
 * Copyright (c) 2021, Jamie Mansfield <jmansfield@cadixdev.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Game.h"
#include <Games/Spider/SpiderGML.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/Timer.h>
#include <LibGUI/Action.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Statusbar.h>
#include <LibGUI/Window.h>
#include <stdio.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    auto app = GUI::Application::construct(argc, argv);
    auto app_icon = GUI::Icon::default_icon("app-spider");
    auto config = Core::ConfigFile::get_for_app("Spider");

    if (pledge("stdio recvfd sendfd rpath wpath cpath", nullptr) < 0) {
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

    auto window = GUI::Window::construct();
    window->set_title("Spider");

    auto mode = static_cast<Spider::Mode>(config->read_num_entry("Settings", "Mode", static_cast<int>(Spider::Mode::SingleSuit)));

    auto update_mode = [&](Spider::Mode new_mode) {
        mode = new_mode;
        config->write_num_entry("Settings", "Mode", static_cast<int>(mode));
        if (!config->sync())
            GUI::MessageBox::show(window, "Configuration could not be saved", "Error", GUI::MessageBox::Type::Error);
    };

    auto mode_id = [&]() {
        switch (mode) {
        case Spider::Mode::SingleSuit:
            return "SingleSuit";
        case Spider::Mode::TwoSuit:
            return "TwoSuit";
        default:
            VERIFY_NOT_REACHED();
        }
    };

    auto high_score = [&]() {
        return static_cast<u32>(config->read_num_entry("HighScores", mode_id(), 0));
    };

    auto update_high_score = [&](u32 new_high_score) {
        config->write_num_entry("HighScores", mode_id(), static_cast<int>(new_high_score));

        if (!config->sync())
            GUI::MessageBox::show(window, "Configuration could not be saved", "Error", GUI::MessageBox::Type::Error);
    };

    auto best_time = [&]() {
        return static_cast<u32>(config->read_num_entry("BestTimes", mode_id(), 0));
    };

    auto update_best_time = [&](u32 new_best_time) {
        config->write_num_entry("BestTimes", mode_id(), static_cast<int>(new_best_time));

        if (!config->sync())
            GUI::MessageBox::show(window, "Configuration could not be saved", "Error", GUI::MessageBox::Type::Error);
    };

    if (mode >= Spider::Mode::__Count)
        update_mode(Spider::Mode::SingleSuit);

    auto& widget = window->set_main_widget<GUI::Widget>();
    widget.load_from_gml(spider_gml);

    auto& game = *widget.find_descendant_of_type_named<Spider::Game>("game");
    game.set_focus(true);

    auto& statusbar = *widget.find_descendant_of_type_named<GUI::Statusbar>("statusbar");
    statusbar.set_text(0, "Score: 0");
    statusbar.set_text(1, String::formatted("High Score: {}", high_score()));
    statusbar.set_text(2, "Time: 00:00:00");

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

        statusbar.set_text(2, String::formatted("Time: {:02}:{:02}:{:02}", hours, minutes, seconds));
    });

    game.on_game_start = [&]() {
        seconds_elapsed = 0;
        timer->start();
        statusbar.set_text(2, "Time: 00:00:00");
    };
    game.on_game_end = [&](Spider::GameOverReason reason, uint32_t score) {
        if (timer->is_active())
            timer->stop();

        if (reason == Spider::GameOverReason::Victory) {
            if (score > high_score()) {
                update_high_score(score);
                statusbar.set_text(1, String::formatted("High Score: {}", score));
            }

            auto current_best_time = best_time();
            if (seconds_elapsed < current_best_time || current_best_time == 0) {
                update_best_time(seconds_elapsed);
            }
        }
        statusbar.set_text(2, "Timer starts after your first move");
    };

    GUI::ActionGroup suit_actions;
    suit_actions.set_exclusive(true);

    auto single_suit_action = GUI::Action::create_checkable("&Single Suit", [&](auto&) {
        update_mode(Spider::Mode::SingleSuit);
        statusbar.set_text(1, String::formatted("High Score: {}", high_score()));
        game.setup(mode);
    });
    single_suit_action->set_checked(mode == Spider::Mode::SingleSuit);
    suit_actions.add_action(single_suit_action);

    auto two_suit_action = GUI::Action::create_checkable("&Two Suit", [&](auto&) {
        update_mode(Spider::Mode::TwoSuit);
        statusbar.set_text(1, String::formatted("High Score: {}", high_score()));
        game.setup(mode);
    });
    two_suit_action->set_checked(mode == Spider::Mode::TwoSuit);
    suit_actions.add_action(two_suit_action);

    auto& game_menu = window->add_menu("&Game");
    game_menu.add_action(GUI::Action::create("&New Game", { Mod_None, Key_F2 }, [&](auto&) {
        game.setup(mode);
    }));
    game_menu.add_separator();
    game_menu.add_action(single_suit_action);
    game_menu.add_action(two_suit_action);
    game_menu.add_separator();
    game_menu.add_action(GUI::CommonActions::make_quit_action([&](auto&) { app->quit(); }));

    auto& help_menu = window->add_menu("&Help");
    help_menu.add_action(GUI::CommonActions::make_about_action("Spider", app_icon, window));

    window->set_resizable(false);
    window->resize(Spider::Game::width, Spider::Game::height + statusbar.max_height());
    window->set_icon(app_icon.bitmap_for_size(16));
    window->show();

    game.setup(mode);

    return app->exec();
}
