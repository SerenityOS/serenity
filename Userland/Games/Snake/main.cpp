/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Game.h"
#include <AK/URL.h>
#include <Games/Snake/SnakeGML.h>
#include <LibConfig/Client.h>
#include <LibCore/System.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/ColorPicker.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/Statusbar.h>
#include <LibGUI/Window.h>
#include <LibMain/Main.h>
#include <stdio.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath recvfd sendfd unix"));

    auto app = TRY(GUI::Application::try_create(arguments));

    Config::pledge_domain("Snake");

    TRY(Desktop::Launcher::add_allowed_handler_with_only_specific_urls("/bin/Help", { URL::create_with_file_scheme("/usr/share/man/man6/Snake.md") }));
    TRY(Desktop::Launcher::seal_allowlist());

    TRY(Core::System::pledge("stdio rpath recvfd sendfd"));

    TRY(Core::System::unveil("/tmp/session/%sid/portal/launch", "rw"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto app_icon = TRY(GUI::Icon::try_create_default_icon("app-snake"sv));

    auto window = TRY(GUI::Window::try_create());

    window->set_double_buffering_enabled(false);
    window->set_title("Snake");
    window->resize(324, 345);

    auto widget = TRY(window->try_set_main_widget<GUI::Widget>());
    widget->load_from_gml(snake_gml);

    auto& game = *widget->find_descendant_of_type_named<Snake::Game>("game");
    game.set_focus(true);

    auto high_score = Config::read_u32("Snake"sv, "Snake"sv, "HighScore"sv, 0);

    auto& statusbar = *widget->find_descendant_of_type_named<GUI::Statusbar>("statusbar"sv);
    statusbar.set_text(0, "Score: 0"sv);
    statusbar.set_text(1, DeprecatedString::formatted("High Score: {}", high_score));

    game.on_score_update = [&](auto score) {
        statusbar.set_text(0, DeprecatedString::formatted("Score: {}", score));
        if (score <= high_score)
            return false;

        statusbar.set_text(1, DeprecatedString::formatted("High Score: {}", score));
        Config::write_u32("Snake"sv, "Snake"sv, "HighScore"sv, score);

        high_score = score;
        return true;
    };

    auto game_menu = TRY(window->try_add_menu("&Game"));

    TRY(game_menu->try_add_action(GUI::Action::create("&New Game", { Mod_None, Key_F2 }, TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/reload.png"sv)), [&](auto&) {
        game.reset();
    })));
    static DeprecatedString const pause_text = "&Pause Game"sv;
    auto const pause_icon = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/pause.png"sv));
    static DeprecatedString const continue_text = "&Continue Game"sv;
    auto const continue_icon = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/play.png"sv));
    TRY(game_menu->try_add_action(GUI::Action::create(pause_text, { Mod_None, Key_Space }, pause_icon, [&](auto& action) {
        if (game.has_timer()) {
            game.pause();
            action.set_text(continue_text);
            action.set_icon(continue_icon);
        } else {
            game.start();
            action.set_text(pause_text);
            action.set_icon(pause_icon);
        }
    })));
    TRY(game_menu->try_add_action(GUI::Action::create("&Change snake color", TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/color-chooser.png"sv)), [&](auto&) {
        game.pause();
        auto dialog = GUI::ColorPicker::construct(Gfx::Color::White, window);
        if (dialog->exec() == GUI::Dialog::ExecResult::OK)
            game.set_snake_base_color(dialog->color());
        game.start();
    })));
    TRY(game_menu->try_add_separator());
    TRY(game_menu->try_add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the()->quit();
    })));

    auto help_menu = TRY(window->try_add_menu("&Help"));
    TRY(help_menu->try_add_action(GUI::CommonActions::make_command_palette_action(window)));
    TRY(help_menu->try_add_action(GUI::CommonActions::make_help_action([](auto&) {
        Desktop::Launcher::open(URL::create_with_file_scheme("/usr/share/man/man6/Snake.md"), "/bin/Help");
    })));
    TRY(help_menu->try_add_action(GUI::CommonActions::make_about_action("Snake", app_icon, window)));

    window->show();

    window->set_icon(app_icon.bitmap_for_size(16));

    return app->exec();
}
