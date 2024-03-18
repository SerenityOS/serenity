/*
 * Copyright (c) 2022, Joe Petrus <joe@petrus.io>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MainWidget.h"
#include "WordGame.h"
#include <LibConfig/Client.h>
#include <LibCore/System.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/Action.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/InputBox.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Statusbar.h>
#include <LibGUI/Window.h>
#include <LibMain/Main.h>
#include <LibURL/URL.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath recvfd sendfd unix"));

    auto app = TRY(GUI::Application::create(arguments));

    Config::pledge_domain("MasterWord");

    TRY(Desktop::Launcher::add_allowed_handler_with_only_specific_urls("/bin/Help", { URL::create_with_file_scheme("/usr/share/man/man6/MasterWord.md") }));
    TRY(Desktop::Launcher::seal_allowlist());

    TRY(Core::System::pledge("stdio rpath recvfd sendfd"));

    TRY(Core::System::unveil("/tmp/session/%sid/portal/launch", "rw"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto app_icon = TRY(GUI::Icon::try_create_default_icon("app-masterword"sv));

    auto window = GUI::Window::construct();
    window->set_icon(app_icon.bitmap_for_size(16));
    window->set_double_buffering_enabled(false);
    window->set_title("MasterWord");
    window->set_resizable(true);
    window->set_auto_shrink(true);

    auto main_widget = TRY(MasterWord::MainWidget::try_create());
    window->set_main_widget(main_widget);
    auto& game = *main_widget->find_descendant_of_type_named<MasterWord::WordGame>("word_game");
    auto& statusbar = *main_widget->find_descendant_of_type_named<GUI::Statusbar>("statusbar");
    GUI::Application::the()->on_action_enter = [&statusbar](GUI::Action& action) {
        statusbar.set_override_text(action.status_tip());
    };
    GUI::Application::the()->on_action_leave = [&statusbar](GUI::Action&) {
        statusbar.set_override_text({});
    };

    auto use_system_theme = Config::read_bool("MasterWord"sv, ""sv, "use_system_theme"sv, false);
    game.set_use_system_theme(use_system_theme);

    auto shortest_word = game.shortest_word();
    auto longest_word = game.longest_word();

    window->set_focused_widget(&game);

    auto game_menu = window->add_menu("&Game"_string);

    game_menu->add_action(GUI::Action::create("&New Game", { Mod_None, Key_F2 }, [&](auto&) {
        game.reset();
    }));

    game_menu->add_separator();
    game_menu->add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the()->quit();
    }));

    auto settings_menu = window->add_menu("&Settings"_string);

    settings_menu->add_action(GUI::Action::create("Set &Word Length...", [&](auto&) {
        auto word_length = Config::read_i32("MasterWord"sv, ""sv, "word_length"sv, 5);
        auto result = GUI::InputBox::show_numeric(window, word_length, shortest_word, longest_word, "Word Length"sv);
        if (!result.is_error() && result.value() == GUI::InputBox::ExecResult::OK) {
            Config::write_i32("MasterWord"sv, ""sv, "word_length"sv, word_length);
            game.set_word_length(word_length);
        }
    }));
    settings_menu->add_action(GUI::Action::create("Set &Number of Guesses...", [&](auto&) {
        auto max_guesses = Config::read_i32("MasterWord"sv, ""sv, "max_guesses"sv, 5);
        auto result = GUI::InputBox::show_numeric(window, max_guesses, 1, 20, "Number of Guesses"sv);
        if (!result.is_error() && result.value() == GUI::InputBox::ExecResult::OK) {
            Config::write_i32("MasterWord"sv, ""sv, "max_guesses"sv, max_guesses);
            game.set_max_guesses(max_guesses);
        }
    }));

    auto toggle_check_guesses = GUI::Action::create_checkable("Check &Guesses in Dictionary", [&](auto& action) {
        auto checked = action.is_checked();
        game.set_check_guesses_in_dictionary(checked);
        Config::write_bool("MasterWord"sv, ""sv, "check_guesses_in_dictionary"sv, checked);
    });
    toggle_check_guesses->set_checked(game.is_checking_guesses());
    settings_menu->add_action(toggle_check_guesses);

    auto theme_menu = window->add_menu("&Theme"_string);
    auto system_theme_action = GUI::Action::create("&System", [&](auto&) {
        game.set_use_system_theme(true);
        Config::write_bool("MasterWord"sv, ""sv, "use_system_theme"sv, true);
    });
    system_theme_action->set_checkable(true);
    system_theme_action->set_checked(use_system_theme);
    theme_menu->add_action(system_theme_action);

    auto wordle_theme_action = GUI::Action::create("&Wordle", [&](auto&) {
        game.set_use_system_theme(false);
        Config::write_bool("MasterWord"sv, ""sv, "use_system_theme"sv, false);
    });
    wordle_theme_action->set_checkable(true);
    wordle_theme_action->set_checked(!use_system_theme);
    theme_menu->add_action(wordle_theme_action);

    GUI::ActionGroup theme_actions;
    theme_actions.set_exclusive(true);
    theme_actions.set_unchecking_allowed(false);
    theme_actions.add_action(system_theme_action);
    theme_actions.add_action(wordle_theme_action);

    auto view_menu = window->add_menu("&View"_string);
    view_menu->add_action(GUI::CommonActions::make_fullscreen_action([&](auto&) {
        window->set_fullscreen(!window->is_fullscreen());
    }));

    auto help_menu = window->add_menu("&Help"_string);
    help_menu->add_action(GUI::CommonActions::make_command_palette_action(window));
    help_menu->add_action(GUI::CommonActions::make_help_action([](auto&) {
        Desktop::Launcher::open(URL::create_with_file_scheme("/usr/share/man/man6/MasterWord.md"), "/bin/Help");
    }));
    help_menu->add_action(GUI::CommonActions::make_about_action("MasterWord"_string, app_icon, window));

    game.on_message = [&](auto message) {
        if (!message.has_value())
            statusbar.set_text({});
        else
            statusbar.set_text(String::from_utf8(*message).release_value_but_fixme_should_propagate_errors());
    };

    window->show();

    return app->exec();
}
