/*
 * Copyright (c) 2020, Till Mayer <till.mayer@web.de>
 * Copyright (c) 2021, the SerenityOS developers.
 * Copyright (c) 2022-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2023, David Ganz <david.g.ganz@gmail.com>
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

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd rpath unix proc exec"));

    auto app = TRY(GUI::Application::create(arguments));
    auto app_icon = TRY(GUI::Icon::try_create_default_icon("app-solitaire"sv));

    auto const man_file = "/usr/share/man/man6/Solitaire.md"sv;

    TRY(Desktop::Launcher::add_allowed_handler_with_only_specific_urls("/bin/Help", { URL::create_with_file_scheme(man_file) }));
    TRY(Desktop::Launcher::seal_allowlist());

    Config::pledge_domains({ "Games", "Solitaire" });
    Config::monitor_domain("Games");

    TRY(Core::System::pledge("stdio recvfd sendfd rpath proc exec"));

    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil("/bin/GamesSettings", "x"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto window = GUI::Window::construct();
    window->set_title("Solitaire");

    auto mode = static_cast<Solitaire::Mode>(Config::read_u32("Solitaire"sv, "Settings"sv, "Mode"sv, to_underlying(Solitaire::Mode::SingleCardDraw)));

    auto update_mode = [&](Solitaire::Mode new_mode) {
        mode = new_mode;
        Config::write_u32("Solitaire"sv, "Settings"sv, "Mode"sv, to_underlying(mode));
    };

    auto high_score = [&]() {
        switch (mode) {
        case Solitaire::Mode::SingleCardDraw:
            return Config::read_u32("Solitaire"sv, "HighScores"sv, "SingleCardDraw"sv, 0);
        case Solitaire::Mode::ThreeCardDraw:
            return Config::read_u32("Solitaire"sv, "HighScores"sv, "ThreeCardDraw"sv, 0);
        default:
            VERIFY_NOT_REACHED();
        }
    };

    auto update_high_score = [&](u32 new_high_score) {
        switch (mode) {
        case Solitaire::Mode::SingleCardDraw:
            Config::write_u32("Solitaire"sv, "HighScores"sv, "SingleCardDraw"sv, new_high_score);
            break;
        case Solitaire::Mode::ThreeCardDraw:
            Config::write_u32("Solitaire"sv, "HighScores"sv, "ThreeCardDraw"sv, new_high_score);
            break;
        default:
            VERIFY_NOT_REACHED();
        }
    };

    if (mode >= Solitaire::Mode::__Count)
        update_mode(Solitaire::Mode::SingleCardDraw);

    auto widget = TRY(Solitaire::MainWidget::try_create());
    window->set_main_widget(widget);

    auto& game = *widget->find_descendant_of_type_named<Solitaire::Game>("game");
    game.set_focus(true);

    auto& action_bar = *widget->find_descendant_of_type_named<GUI::Widget>("game_action_bar");
    action_bar.set_background_color(game.background_color());
    action_bar.set_visible(false);

    auto& solve_button = *action_bar.find_descendant_of_type_named<GUI::Button>("solve_button");
    solve_button.on_click = [&](auto) {
        game.start_solving();
        solve_button.set_enabled(false);
    };
    solve_button.set_enabled(false);

    auto& statusbar = *widget->find_descendant_of_type_named<GUI::Statusbar>("statusbar");
    statusbar.set_text(0, "Score: 0"_string);
    statusbar.set_text(1, TRY(String::formatted("High Score: {}", high_score())));
    statusbar.set_text(2, "Time: 00:00"_string);

    app->on_action_enter = [&](GUI::Action& action) {
        statusbar.set_override_text(action.status_tip());
    };

    app->on_action_leave = [&](GUI::Action&) {
        statusbar.set_override_text({});
    };

    game.on_score_update = [&](uint32_t score) {
        statusbar.set_text(0, String::formatted("Score: {}", score).release_value_but_fixme_should_propagate_errors());
    };

    uint64_t seconds_elapsed = 0;

    auto timer = Core::Timer::create_repeating(1000, [&]() {
        ++seconds_elapsed;
        statusbar.set_text(2, String::formatted("Time: {}", human_readable_digital_time(seconds_elapsed)).release_value_but_fixme_should_propagate_errors());
    });

    game.on_game_start = [&]() {
        seconds_elapsed = 0;
        timer->start();
        statusbar.set_text(2, "Time: 00:00"_string);
    };
    game.on_move = [&]() {
        solve_button.set_enabled(true);
        action_bar.set_visible(game.can_solve());
    };
    game.on_game_end = [&](Solitaire::GameOverReason reason, uint32_t score) {
        if (timer->is_active())
            timer->stop();

        solve_button.set_enabled(false);
        action_bar.set_visible(false);

        if (reason == Solitaire::GameOverReason::Victory) {
            if (seconds_elapsed >= 30) {
                uint32_t bonus = (20'000 / seconds_elapsed) * 35;
                statusbar.set_text(0, String::formatted("Score: {} (Bonus: {})", score, bonus).release_value_but_fixme_should_propagate_errors());
                score += bonus;
            }

            if (score > high_score()) {
                update_high_score(score);
                statusbar.set_text(1, String::formatted("High Score: {}", score).release_value_but_fixme_should_propagate_errors());
            }
        }
        statusbar.set_text(2, "Timer starts after your first move"_string);
    };

    auto confirm_end_current_game = [&]() {
        auto game_in_progress = timer->is_active();
        if (game_in_progress) {
            auto result = GUI::MessageBox::show(window,
                "A game is still in progress, are you sure you would like to end it?"sv,
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

    GUI::ActionGroup draw_setting_actions;
    draw_setting_actions.set_exclusive(true);

    auto single_card_draw_action = GUI::Action::create_checkable("&Single Card Draw", [&](auto&) {
        update_mode(Solitaire::Mode::SingleCardDraw);

        if (!confirm_end_current_game())
            return;

        statusbar.set_text(1, String::formatted("High Score: {}", high_score()).release_value_but_fixme_should_propagate_errors());
        game.setup(mode);
    });
    single_card_draw_action->set_checked(mode == Solitaire::Mode::SingleCardDraw);
    single_card_draw_action->set_status_tip("Draw one card at a time"_string);
    draw_setting_actions.add_action(single_card_draw_action);

    auto three_card_draw_action = GUI::Action::create_checkable("&Three Card Draw", [&](auto&) {
        update_mode(Solitaire::Mode::ThreeCardDraw);

        if (!confirm_end_current_game())
            return;

        statusbar.set_text(1, String::formatted("High Score: {}", high_score()).release_value_but_fixme_should_propagate_errors());
        game.setup(mode);
    });
    three_card_draw_action->set_checked(mode == Solitaire::Mode::ThreeCardDraw);
    three_card_draw_action->set_status_tip("Draw three cards at a time"_string);
    draw_setting_actions.add_action(three_card_draw_action);

    game.set_auto_collect(Config::read_bool("Solitaire"sv, "Settings"sv, "AutoCollect"sv, false));
    auto toggle_auto_collect_action = GUI::Action::create_checkable("Auto-&Collect", [&](auto& action) {
        auto checked = action.is_checked();
        game.set_auto_collect(checked);
        Config::write_bool("Solitaire"sv, "Settings"sv, "AutoCollect"sv, checked);
    });
    toggle_auto_collect_action->set_checked(game.is_auto_collecting());
    toggle_auto_collect_action->set_status_tip("Auto-collect to foundation piles"_string);

    auto game_menu = window->add_menu("&Game"_string);

    game_menu->add_action(GUI::Action::create("&New Game", { Mod_None, Key_F2 }, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/reload.png"sv)), [&](auto&) {
        if (!confirm_end_current_game())
            return;

        game.setup(mode);
    }));
    game_menu->add_separator();
    auto undo_action = GUI::CommonActions::make_undo_action([&](auto&) {
        game.perform_undo();
    });
    undo_action->set_enabled(false);
    game_menu->add_action(undo_action);
    game_menu->add_separator();
    game_menu->add_action(TRY(Cards::make_cards_settings_action(window)));
    game_menu->add_action(single_card_draw_action);
    game_menu->add_action(three_card_draw_action);
    game_menu->add_separator();
    game_menu->add_action(toggle_auto_collect_action);
    game_menu->add_separator();
    game_menu->add_action(GUI::CommonActions::make_quit_action([&](auto&) { app->quit(); }));

    auto view_menu = window->add_menu("&View"_string);
    view_menu->add_action(GUI::CommonActions::make_fullscreen_action([&](auto&) {
        window->set_fullscreen(!window->is_fullscreen());
    }));

    auto help_menu = window->add_menu("&Help"_string);
    help_menu->add_action(GUI::CommonActions::make_command_palette_action(window));
    help_menu->add_action(GUI::CommonActions::make_help_action([&man_file](auto&) {
        Desktop::Launcher::open(URL::create_with_file_scheme(man_file), "/bin/Help");
    }));
    help_menu->add_action(GUI::CommonActions::make_about_action("Solitaire"_string, app_icon, window));

    window->set_resizable(false);
    window->resize(Solitaire::Game::width, Solitaire::Game::height + statusbar.max_height().as_int() + action_bar.height());
    window->set_icon(app_icon.bitmap_for_size(16));
    window->show();

    game.on_undo_availability_change = [&](bool undo_available) {
        undo_action->set_enabled(undo_available);
    };

    game.setup(mode);

    return app->exec();
}
