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

enum class StatisticDisplay : u8 {
    HighScore,
    BestTime,
    __Count
};

static String format_seconds(uint64_t seconds_elapsed)
{
    uint64_t hours = seconds_elapsed / 3600;
    uint64_t minutes = (seconds_elapsed / 60) % 60;
    uint64_t seconds = seconds_elapsed % 60;

    return String::formatted("{:02}:{:02}:{:02}", hours, minutes, seconds);
}

int main(int argc, char** argv)
{
    auto app = GUI::Application::construct(argc, argv);
    auto app_icon = GUI::Icon::default_icon("app-spider");
    auto config = Core::ConfigFile::get_for_app("Spider", Core::ConfigFile::AllowWriting::Yes);

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

    auto statistic_display = static_cast<StatisticDisplay>(config->read_num_entry("Settings", "StatisticDisplay", static_cast<int>(StatisticDisplay::HighScore)));
    auto update_statistic_display = [&](StatisticDisplay new_statistic_display) {
        statistic_display = new_statistic_display;
        config->write_num_entry("Settings", "StatisticDisplay", static_cast<int>(statistic_display));
        if (!config->sync())
            GUI::MessageBox::show(window, "Configuration could not be saved", "Error", GUI::MessageBox::Type::Error);
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

    auto total_wins = [&]() {
        return static_cast<u32>(config->read_num_entry("TotalWins", mode_id(), 0));
    };
    auto increment_total_wins = [&]() {
        config->write_num_entry("TotalWins", mode_id(), static_cast<int>(total_wins() + 1));

        if (!config->sync())
            GUI::MessageBox::show(window, "Configuration could not be saved", "Error", GUI::MessageBox::Type::Error);
    };

    auto total_losses = [&]() {
        return static_cast<u32>(config->read_num_entry("TotalLosses", mode_id(), 0));
    };
    auto increment_total_losses = [&]() {
        config->write_num_entry("TotalLosses", mode_id(), static_cast<int>(total_losses() + 1));

        if (!config->sync())
            GUI::MessageBox::show(window, "Configuration could not be saved", "Error", GUI::MessageBox::Type::Error);
    };

    if (mode >= Spider::Mode::__Count)
        update_mode(Spider::Mode::SingleSuit);

    if (statistic_display >= StatisticDisplay::__Count)
        update_statistic_display(StatisticDisplay::HighScore);

    auto& widget = window->set_main_widget<GUI::Widget>();
    widget.load_from_gml(spider_gml);

    auto& game = *widget.find_descendant_of_type_named<Spider::Game>("game");
    game.set_focus(true);

    auto& statusbar = *widget.find_descendant_of_type_named<GUI::Statusbar>("statusbar");
    auto reset_statistic_status = [&]() {
        switch (statistic_display) {
        case StatisticDisplay::HighScore:
            statusbar.set_text(1, String::formatted("High Score: {}", high_score()));
            break;
        case StatisticDisplay::BestTime:
            statusbar.set_text(1, String::formatted("Best Time: {}", format_seconds(best_time())));
            break;
        default:
            VERIFY_NOT_REACHED();
        }
    };

    statusbar.set_text(0, "Score: 0");
    reset_statistic_status();
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

        statusbar.set_text(2, String::formatted("Time: {}", format_seconds(seconds_elapsed)));
    });

    game.on_game_start = [&]() {
        seconds_elapsed = 0;
        timer->start();
        statusbar.set_text(2, "Time: 00:00:00");
    };
    game.on_game_end = [&](Spider::GameOverReason reason, uint32_t score) {
        auto game_was_in_progress = timer->is_active();
        if (game_was_in_progress) {
            timer->stop();

            if (reason != Spider::GameOverReason::Victory)
                increment_total_losses();
        }

        if (reason == Spider::GameOverReason::Victory) {
            increment_total_wins();

            if (score > high_score()) {
                update_high_score(score);
            }

            auto current_best_time = best_time();
            if (seconds_elapsed < current_best_time || current_best_time == 0) {
                update_best_time(seconds_elapsed);
            }

            reset_statistic_status();
        }
        statusbar.set_text(2, "Timer starts after your first move");
    };

    window->on_close_request = [&]() {
        auto game_in_progress = timer->is_active();
        if (game_in_progress) {
            auto result = GUI::MessageBox::show(window,
                "A game is still in progress, are you sure you would like to quit? Doing so will count as a loss.",
                "Game in progress",
                GUI::MessageBox::Type::Warning,
                GUI::MessageBox::InputType::YesNo);

            if (result == GUI::MessageBox::ExecYes)
                return GUI::Window::CloseRequestDecision::Close;
            else
                return GUI::Window::CloseRequestDecision::StayOpen;
        }

        return GUI::Window::CloseRequestDecision::Close;
    };
    window->on_close = [&]() {
        game.on_game_end(Spider::GameOverReason::Quit, 0);
    };

    GUI::ActionGroup suit_actions;
    suit_actions.set_exclusive(true);

    auto single_suit_action = GUI::Action::create_checkable("&Single Suit", [&](auto&) {
        update_mode(Spider::Mode::SingleSuit);
        reset_statistic_status();
        game.setup(mode);
    });
    single_suit_action->set_checked(mode == Spider::Mode::SingleSuit);
    suit_actions.add_action(single_suit_action);

    auto two_suit_action = GUI::Action::create_checkable("&Two Suit", [&](auto&) {
        update_mode(Spider::Mode::TwoSuit);
        reset_statistic_status();
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

    auto& view_menu = window->add_menu("&View");

    GUI::ActionGroup statistic_display_actions;
    statistic_display_actions.set_exclusive(true);

    auto high_score_action = GUI::Action::create_checkable("&High Score", [&](auto&) {
        update_statistic_display(StatisticDisplay::HighScore);
        reset_statistic_status();
    });
    high_score_action->set_checked(statistic_display == StatisticDisplay::HighScore);
    statistic_display_actions.add_action(high_score_action);

    auto best_time_actions = GUI::Action::create_checkable("&Best Time", [&](auto&) {
        update_statistic_display(StatisticDisplay::BestTime);
        reset_statistic_status();
    });
    best_time_actions->set_checked(statistic_display == StatisticDisplay::BestTime);
    statistic_display_actions.add_action(best_time_actions);

    view_menu.add_action(high_score_action);
    view_menu.add_action(best_time_actions);

    auto& help_menu = window->add_menu("&Help");
    help_menu.add_action(GUI::CommonActions::make_about_action("Spider", app_icon, window));

    window->set_resizable(false);
    window->resize(Spider::Game::width, Spider::Game::height + statusbar.max_height());
    window->set_icon(app_icon.bitmap_for_size(16));
    window->show();

    game.setup(mode);

    return app->exec();
}
