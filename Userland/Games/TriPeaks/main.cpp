/*
 * Copyright (c) 2026, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Game.h"
#include "MainWidget.h"
#include <AK/NumberFormat.h>
#include <LibConfig/Client.h>
#include <LibCore/System.h>
#include <LibCore/Timer.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/Action.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Statusbar.h>
#include <LibGUI/Window.h>
#include <LibMain/Main.h>
#include <LibURL/URL.h>
#include <stdio.h>

enum class StatisticDisplay : u8 {
    HighScore,
    BestTime,
    __Count
};

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd rpath unix proc exec"));

    auto app = TRY(GUI::Application::create(arguments));
    auto app_icon = TRY(GUI::Icon::try_create_default_icon("app-tripeaks"sv));

    Config::pledge_domains({ "Games", "TriPeaks" });
    Config::monitor_domain("Games");

    auto const man_file = "/usr/share/man/man6/TriPeaks.md";

    TRY(Desktop::Launcher::add_allowed_handler_with_only_specific_urls("/bin/Help", { URL::create_with_file_scheme(man_file) }));
    TRY(Desktop::Launcher::seal_allowlist());

    TRY(Core::System::pledge("stdio recvfd sendfd rpath proc exec"));

    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil("/bin/GamesSettings", "x"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto window = GUI::Window::construct();
    window->set_title("TriPeaks");

    auto statistic_display = static_cast<StatisticDisplay>(Config::read_u32("TriPeaks"sv, "Settings"sv, "StatisticDisplay"sv, to_underlying(StatisticDisplay::HighScore)));
    auto update_statistic_display = [&](StatisticDisplay new_statistic_display) {
        statistic_display = new_statistic_display;
        Config::write_u32("TriPeaks"sv, "Settings"sv, "StatisticDisplay"sv, to_underlying(statistic_display));
    };

    auto high_score = [&]() {
        return Config::read_u32("TriPeaks"sv, "HighScores"sv, "Default"sv, 0);
    };

    auto update_high_score = [&](u32 new_high_score) {
        Config::write_u32("TriPeaks"sv, "HighScores"sv, "Default"sv, new_high_score);
    };

    auto best_time = [&]() {
        return Config::read_u32("TriPeaks"sv, "BestTimes"sv, "Default"sv, 0);
    };

    auto update_best_time = [&](u32 new_best_time) {
        Config::write_u32("TriPeaks"sv, "BestTimes"sv, "Default"sv, new_best_time);
    };

    auto total_wins = [&]() {
        return Config::read_u32("TriPeaks"sv, "TotalWins"sv, "Default"sv, 0);
    };
    auto increment_total_wins = [&]() {
        Config::write_u32("TriPeaks"sv, "TotalWins"sv, "Default"sv, total_wins() + 1);
    };

    auto total_losses = [&]() {
        return Config::read_u32("TriPeaks"sv, "TotalLosses"sv, "Default"sv, 0);
    };
    auto increment_total_losses = [&]() {
        Config::write_u32("TriPeaks"sv, "TotalLosses"sv, "Default"sv, total_losses() + 1);
    };

    if (statistic_display >= StatisticDisplay::__Count)
        update_statistic_display(StatisticDisplay::HighScore);

    auto widget = TRY(TriPeaks::MainWidget::try_create());
    window->set_main_widget(widget);

    auto& game = *widget->find_descendant_of_type_named<TriPeaks::Game>("game");
    game.set_focus(true);

    auto& statusbar = *widget->find_descendant_of_type_named<GUI::Statusbar>("statusbar");
    auto reset_statistic_status = [&]() {
        switch (statistic_display) {
        case StatisticDisplay::HighScore:
            statusbar.set_text(1, String::formatted("High Score: {}", high_score()).release_value_but_fixme_should_propagate_errors());
            break;
        case StatisticDisplay::BestTime:
            statusbar.set_text(1, String::formatted("Best Time: {}", human_readable_digital_time(best_time())).release_value_but_fixme_should_propagate_errors());
            break;
        default:
            VERIFY_NOT_REACHED();
        }
    };

    statusbar.set_text(0, "Score: 0"_string);
    reset_statistic_status();
    statusbar.set_text(2, "Time: 00:00:00"_string);

    app->on_action_enter = [&](GUI::Action& action) {
        statusbar.set_override_text(action.status_tip());
    };

    app->on_action_leave = [&](GUI::Action&) {
        statusbar.set_override_text({});
    };

    game.on_score_update = [&](u32 score) {
        statusbar.set_text(0, String::formatted("Score: {}", score).release_value_but_fixme_should_propagate_errors());
    };

    game.on_streak_update = [&](u32) { };

    u64 seconds_elapsed = 0;

    auto timer = Core::Timer::create_repeating(1000, [&]() {
        ++seconds_elapsed;

        statusbar.set_text(2, String::formatted("Time: {}", human_readable_digital_time(seconds_elapsed)).release_value_but_fixme_should_propagate_errors());
    });

    game.on_game_start = [&]() {
        seconds_elapsed = 0;
        timer->start();
        statusbar.set_text(2, "Time: 00:00:00"_string);
    };
    game.on_game_end = [&](TriPeaks::GameOverReason reason, u32 score) {
        auto game_was_in_progress = timer->is_active();
        if (game_was_in_progress) {
            timer->stop();

            if (reason != TriPeaks::GameOverReason::Victory)
                increment_total_losses();
        }

        if (reason == TriPeaks::GameOverReason::Victory) {
            increment_total_wins();

            if (score > high_score()) {
                update_high_score(score);
            }

            auto current_best_time = best_time();
            if (seconds_elapsed < current_best_time || current_best_time == 0) {
                update_best_time(seconds_elapsed);
            }

            reset_statistic_status();

            GUI::MessageBox::show(window,
                String::formatted("Congratulations! You won with a score of {} in {}.", score, human_readable_digital_time(seconds_elapsed)).release_value_but_fixme_should_propagate_errors(),
                "You Win!"sv,
                GUI::MessageBox::Type::Information,
                GUI::MessageBox::InputType::OK);
        } else if (reason == TriPeaks::GameOverReason::NewGame && game_was_in_progress) {
            GUI::MessageBox::show(window,
                String::formatted("No more valid moves. Final score: {}.", score).release_value_but_fixme_should_propagate_errors(),
                "Game Over"sv,
                GUI::MessageBox::Type::Information,
                GUI::MessageBox::InputType::OK);
        }
        statusbar.set_text(2, "Timer starts after your first move"_string);
    };

    auto confirm_end_current_game = [&]() {
        auto game_in_progress = timer->is_active();
        if (game_in_progress) {
            auto result = GUI::MessageBox::show(window,
                "A game is still in progress, are you sure you would like to end it? Doing so will count as a loss."sv,
                "Game in progress"sv,
                GUI::MessageBox::Type::Warning,
                GUI::MessageBox::InputType::YesNo);

            return result == GUI::MessageBox::ExecResult::Yes;
        }

        return true;
    };

    window->on_close_request = [&]() {
        if (confirm_end_current_game())
            return GUI::Window::CloseRequestDecision::Close;
        return GUI::Window::CloseRequestDecision::StayOpen;
    };
    window->on_close = [&]() {
        game.on_game_end(TriPeaks::GameOverReason::Quit, 0);
    };

    auto game_menu = window->add_menu("&Game"_string);
    game_menu->add_action(GUI::Action::create("&New Game", { Mod_None, Key_F2 }, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/reload.png"sv)), [&](auto&) {
        if (!confirm_end_current_game())
            return;

        game.setup();
    }));
    game_menu->add_separator();
    auto undo_action = GUI::CommonActions::make_undo_action([&](auto&) {
        game.perform_undo();
    });
    undo_action->set_enabled(false);
    game_menu->add_action(undo_action);

    game.on_undo_availability_change = [&](bool undo_available) {
        undo_action->set_enabled(undo_available);
    };

    game_menu->add_separator();
    game_menu->add_action(TRY(Cards::make_cards_settings_action(window)));
    game_menu->add_separator();
    game_menu->add_action(GUI::CommonActions::make_quit_action([&](auto&) { app->quit(); }));

    auto view_menu = window->add_menu("&View"_string);

    GUI::ActionGroup statistic_display_actions;
    statistic_display_actions.set_exclusive(true);

    auto high_score_action = GUI::Action::create_checkable("&High Score", [&](auto&) {
        update_statistic_display(StatisticDisplay::HighScore);
        reset_statistic_status();
    });
    high_score_action->set_checked(statistic_display == StatisticDisplay::HighScore);
    high_score_action->set_status_tip("Display the high score in the status bar"_string);
    statistic_display_actions.add_action(high_score_action);

    auto best_time_actions = GUI::Action::create_checkable("&Best Time", [&](auto&) {
        update_statistic_display(StatisticDisplay::BestTime);
        reset_statistic_status();
    });
    best_time_actions->set_checked(statistic_display == StatisticDisplay::BestTime);
    best_time_actions->set_status_tip("Display the best time in the status bar"_string);
    statistic_display_actions.add_action(best_time_actions);

    view_menu->add_action(high_score_action);
    view_menu->add_action(best_time_actions);

    view_menu->add_separator();
    view_menu->add_action(GUI::CommonActions::make_fullscreen_action([&](auto&) {
        window->set_fullscreen(!window->is_fullscreen());
    }));

    auto help_menu = window->add_menu("&Help"_string);
    help_menu->add_action(GUI::CommonActions::make_command_palette_action(window));
    help_menu->add_action(GUI::CommonActions::make_help_action([&man_file](auto&) {
        Desktop::Launcher::open(URL::create_with_file_scheme(man_file), "/bin/Help");
    }));
    help_menu->add_action(GUI::CommonActions::make_about_action("TriPeaks"_string, app_icon, window));

    window->set_resizable(false);
    window->resize(TriPeaks::Game::width, TriPeaks::Game::height + statusbar.max_height().as_int());
    window->set_icon(app_icon.bitmap_for_size(16));
    window->show();

    game.setup();

    return app->exec();
}
