/*
 * Copyright (c) 2022, Joe Petrus <joe@petrus.io>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "WordGame.h"
#include <AK/URL.h>
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
#include <LibGUI/Window.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath recvfd sendfd unix"));

    auto app = TRY(GUI::Application::try_create(arguments));

    Config::pledge_domain("MasterWord");

    TRY(Desktop::Launcher::add_allowed_handler_with_only_specific_urls("/bin/Help", { URL::create_with_file_protocol("/usr/share/man/man6/MasterWord.md") }));
    TRY(Desktop::Launcher::seal_allowlist());

    TRY(Core::System::pledge("stdio rpath recvfd sendfd"));

    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil("/tmp/portal/launch", "rw"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto app_icon = TRY(GUI::Icon::try_create_default_icon("app-masterword"));

    auto window = TRY(GUI::Window::try_create());

    window->set_double_buffering_enabled(false);
    window->set_title("MasterWord");
    window->set_resizable(false);

    auto game = TRY(window->try_set_main_widget<WordGame>());

    auto use_system_theme = Config::read_bool("MasterWord", "", "use_system_theme", false);
    game->set_use_system_theme(use_system_theme);

    auto shortest_word = game->shortest_word();
    auto longest_word = game->longest_word();

    window->resize(game->game_size());

    auto game_menu = TRY(window->try_add_menu("&Game"));

    TRY(game_menu->try_add_action(GUI::Action::create("&New Game", { Mod_None, Key_F2 }, [&](auto&) {
        game->reset();
    })));
    TRY(game_menu->try_add_action(GUI::Action::create("Set &Word Length", [&](auto&) {
        auto word_length = Config::read_i32("MasterWord", "", "word_length", 5);
        auto word_length_string = String::number(word_length);
        if (GUI::InputBox::show(window, word_length_string, "Word length:", "MasterWord") == GUI::InputBox::ExecResult::OK && !word_length_string.is_empty()) {
            auto maybe_word_length = word_length_string.template to_uint();
            if (!maybe_word_length.has_value() || maybe_word_length.value() < shortest_word || maybe_word_length.value() > longest_word) {
                GUI::MessageBox::show(window, String::formatted("Please enter a number between {} and {}.", shortest_word, longest_word), "MasterWord");
                return;
            }

            word_length = maybe_word_length.value();
            Config::write_i32("MasterWord", "", "word_length", word_length);
            game->set_word_length(word_length);
            window->resize(game->game_size());
        }
    })));
    TRY(game_menu->try_add_action(GUI::Action::create("Set &Number Of Guesses", [&](auto&) {
        auto max_guesses = Config::read_i32("MasterWord", "", "max_guesses", 5);
        auto max_guesses_string = String::number(max_guesses);
        if (GUI::InputBox::show(window, max_guesses_string, "Maximum number of guesses:", "MasterWord") == GUI::InputBox::ExecResult::OK && !max_guesses_string.is_empty()) {
            auto maybe_max_guesses = max_guesses_string.template to_uint();
            if (!maybe_max_guesses.has_value() || maybe_max_guesses.value() < 1 || maybe_max_guesses.value() > 20) {
                GUI::MessageBox::show(window, "Please enter a number between 1 and 20.", "MasterWord");
                return;
            }

            max_guesses = maybe_max_guesses.value();
            Config::write_i32("MasterWord", "", "max_guesses", max_guesses);
            game->set_max_guesses(max_guesses);
            window->resize(game->game_size());
        }
    })));

    TRY(game_menu->try_add_separator());
    TRY(game_menu->try_add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the()->quit();
    })));

    auto theme_menu = TRY(window->try_add_menu("&Theme"));
    auto system_theme_action = GUI::Action::create("&System", [&](auto&) {
        game->set_use_system_theme(true);
        Config::write_bool("MasterWord", "", "use_system_theme", true);
    });
    system_theme_action->set_checkable(true);
    system_theme_action->set_checked(use_system_theme);
    TRY(theme_menu->try_add_action(system_theme_action));

    auto wordle_theme_action = GUI::Action::create("&Wordle", [&](auto&) {
        game->set_use_system_theme(false);
        Config::write_bool("MasterWord", "", "use_system_theme", false);
    });
    wordle_theme_action->set_checkable(true);
    wordle_theme_action->set_checked(!use_system_theme);
    TRY(theme_menu->try_add_action(wordle_theme_action));

    GUI::ActionGroup theme_actions;
    theme_actions.set_exclusive(true);
    theme_actions.set_unchecking_allowed(false);
    theme_actions.add_action(system_theme_action);
    theme_actions.add_action(wordle_theme_action);

    auto help_menu = TRY(window->try_add_menu("&Help"));
    TRY(help_menu->try_add_action(GUI::CommonActions::make_help_action([](auto&) {
        Desktop::Launcher::open(URL::create_with_file_protocol("/usr/share/man/man6/MasterWord.md"), "/bin/Help");
    })));
    TRY(help_menu->try_add_action(GUI::CommonActions::make_about_action("MasterWord", app_icon, window)));

    window->show();

    window->set_icon(app_icon.bitmap_for_size(16));

    return app->exec();
}
