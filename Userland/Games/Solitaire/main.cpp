/*
 * Copyright (c) 2020, Till Mayer <till.mayer@web.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Game.h"
#include <Games/Solitaire/SolitaireGML.h>
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
    auto app_icon = GUI::Icon::default_icon("app-solitaire");
    auto config = Core::ConfigFile::get_for_app("Solitaire");

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
    window->set_title("Solitaire");

    auto mode = static_cast<Solitaire::Mode>(config->read_num_entry("Settings", "Mode", static_cast<int>(Solitaire::Mode::SingleCardDraw)));

    auto update_mode = [&](Solitaire::Mode new_mode) {
        mode = new_mode;
        config->write_num_entry("Settings", "Mode", static_cast<int>(mode));
        if (!config->sync())
            GUI::MessageBox::show(window, "Configuration could not be saved", "Error", GUI::MessageBox::Type::Error);
    };

    auto high_score = [&]() {
        switch (mode) {
        case Solitaire::Mode::SingleCardDraw:
            return static_cast<u32>(config->read_num_entry("HighScores", "SingleCardDraw", 0));
        case Solitaire::Mode::ThreeCardDraw:
            return static_cast<u32>(config->read_num_entry("HighScores", "ThreeCardDraw", 0));
        default:
            VERIFY_NOT_REACHED();
        }
    };

    auto update_high_score = [&](u32 new_high_score) {
        switch (mode) {
        case Solitaire::Mode::SingleCardDraw:
            config->write_num_entry("HighScores", "SingleCardDraw", static_cast<int>(new_high_score));
            break;
        case Solitaire::Mode::ThreeCardDraw:
            config->write_num_entry("HighScores", "ThreeCardDraw", static_cast<int>(new_high_score));
            break;
        default:
            VERIFY_NOT_REACHED();
        }

        if (!config->sync())
            GUI::MessageBox::show(window, "Configuration could not be saved", "Error", GUI::MessageBox::Type::Error);
    };

    if (mode >= Solitaire::Mode::__Count)
        update_mode(Solitaire::Mode::SingleCardDraw);

    auto& widget = window->set_main_widget<GUI::Widget>();
    widget.load_from_gml(solitaire_gml);

    auto& game = *widget.find_descendant_of_type_named<Solitaire::Game>("game");
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
    };
    game.on_game_end = [&](Solitaire::GameOverReason reason, uint32_t score) {
        if (timer->is_active())
            timer->stop();

        if (reason == Solitaire::GameOverReason::Victory) {
            if (seconds_elapsed >= 30) {
                uint32_t bonus = (20'000 / seconds_elapsed) * 35;
                statusbar.set_text(0, String::formatted("Score: {} (Bonus: {})", score, bonus));
                score += bonus;
            }

            if (score > high_score()) {
                update_high_score(score);
                statusbar.set_text(1, String::formatted("High Score: {}", score));
            }
        }
        statusbar.set_text(2, "Timer starts after your first move");
    };

    GUI::ActionGroup draw_setting_actions;
    draw_setting_actions.set_exclusive(true);

    auto single_card_draw_action = GUI::Action::create_checkable("&Single Card Draw", [&](auto&) {
        update_mode(Solitaire::Mode::SingleCardDraw);
        statusbar.set_text(1, String::formatted("High Score: {}", high_score()));
        game.setup(mode);
    });
    single_card_draw_action->set_checked(mode == Solitaire::Mode::SingleCardDraw);
    single_card_draw_action->set_status_tip("Draw one card at a time");
    draw_setting_actions.add_action(single_card_draw_action);

    auto three_card_draw_action = GUI::Action::create_checkable("&Three Card Draw", [&](auto&) {
        update_mode(Solitaire::Mode::ThreeCardDraw);
        statusbar.set_text(1, String::formatted("High Score: {}", high_score()));
        game.setup(mode);
    });
    three_card_draw_action->set_checked(mode == Solitaire::Mode::ThreeCardDraw);
    three_card_draw_action->set_status_tip("Draw three cards at a time");
    draw_setting_actions.add_action(three_card_draw_action);

    auto menubar = GUI::Menubar::construct();
    auto& game_menu = menubar->add_menu("&Game");

    game_menu.add_action(GUI::Action::create("&New Game", { Mod_None, Key_F2 }, [&](auto&) {
        game.setup(mode);
    }));
    game_menu.add_separator();
    auto undo_action = GUI::CommonActions::make_undo_action([&](auto&) {
        game.perform_undo();
    });
    undo_action->set_enabled(false);
    game_menu.add_action(undo_action);
    game_menu.add_separator();
    game_menu.add_action(single_card_draw_action);
    game_menu.add_action(three_card_draw_action);
    game_menu.add_separator();
    game_menu.add_action(GUI::CommonActions::make_quit_action([&](auto&) { app->quit(); }));

    auto& help_menu = menubar->add_menu("&Help");
    help_menu.add_action(GUI::CommonActions::make_about_action("Solitaire", app_icon, window));

    window->set_resizable(false);
    window->resize(Solitaire::Game::width, Solitaire::Game::height + statusbar.max_height());
    window->set_menubar(move(menubar));
    window->set_icon(app_icon.bitmap_for_size(16));
    window->show();

    game.on_undo_availability_change = [&](bool undo_available) {
        undo_action->set_enabled(undo_available);
    };

    game.setup(mode);

    return app->exec();
}
