/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Game.h"
#include "MainWidget.h"
#include "Skins/SnakeSkin.h"
#include <LibConfig/Client.h>
#include <LibCore/Directory.h>
#include <LibCore/System.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/Action.h>
#include <LibGUI/ActionGroup.h>
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
#include <LibURL/URL.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath recvfd sendfd unix"));

    auto app = TRY(GUI::Application::create(arguments));

    Config::pledge_domain("Snake");
    Config::monitor_domain("Snake");

    TRY(Desktop::Launcher::add_allowed_handler_with_only_specific_urls("/bin/Help", { URL::create_with_file_scheme("/usr/share/man/man6/Snake.md") }));
    TRY(Desktop::Launcher::seal_allowlist());

    TRY(Core::System::pledge("stdio rpath recvfd sendfd"));

    TRY(Core::System::unveil("/tmp/session/%sid/portal/launch", "rw"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto app_icon = TRY(GUI::Icon::try_create_default_icon("app-snake"sv));

    auto window = GUI::Window::construct();

    window->set_double_buffering_enabled(false);
    window->set_title("Snake");
    window->resize(324, 345);

    auto widget = TRY(Snake::MainWidget::try_create());
    window->set_main_widget(widget);

    auto& game = *widget->find_descendant_of_type_named<Snake::Game>("game");
    game.set_focus(true);

    auto high_score = Config::read_u32("Snake"sv, "Snake"sv, "HighScore"sv, 0);
    auto snake_skin_name = Config::read_string("Snake"sv, "Snake"sv, "SnakeSkin"sv, "Snake"sv);

    auto& statusbar = *widget->find_descendant_of_type_named<GUI::Statusbar>("statusbar"sv);
    statusbar.set_text(0, "Score: 0"_string);
    statusbar.set_text(1, TRY(String::formatted("High Score: {}", high_score)));
    GUI::Application::the()->on_action_enter = [&statusbar](GUI::Action& action) {
        statusbar.set_override_text(action.status_tip());
    };
    GUI::Application::the()->on_action_leave = [&statusbar](GUI::Action&) {
        statusbar.set_override_text({});
    };

    game.on_score_update = [&](auto score) {
        statusbar.set_text(0, String::formatted("Score: {}", score).release_value_but_fixme_should_propagate_errors());
        if (score <= high_score)
            return false;

        statusbar.set_text(1, String::formatted("High Score: {}", score).release_value_but_fixme_should_propagate_errors());
        Config::write_u32("Snake"sv, "Snake"sv, "HighScore"sv, score);

        high_score = score;
        return true;
    };

    auto game_menu = window->add_menu("&Game"_string);

    game_menu->add_action(GUI::Action::create("&New Game", { Mod_None, Key_F2 }, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/reload.png"sv)), [&](auto&) {
        game.reset();
    }));
    static String const pause_text = "&Pause Game"_string;
    auto const pause_icon = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/pause.png"sv));
    static String const continue_text = "&Continue Game"_string;
    auto const continue_icon = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/play.png"sv));
    game_menu->add_action(GUI::Action::create(pause_text.to_byte_string(), { Mod_None, Key_Space }, pause_icon, [&](auto& action) {
        if (game.has_timer()) {
            game.pause();
            action.set_text(continue_text.to_byte_string());
            action.set_icon(continue_icon);
        } else {
            game.start();
            action.set_text(pause_text.to_byte_string());
            action.set_icon(pause_icon);
        }
    }));

    auto change_snake_color = GUI::Action::create("&Change Snake Color", TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/color-chooser.png"sv)), [&](auto&) {
        auto was_paused = game.is_paused();
        if (!was_paused)
            game.pause();
        auto dialog = GUI::ColorPicker::construct(game.get_skin_color(), window);
        dialog->on_color_changed = [&game](Gfx::Color color) {
            game.set_skin_color(color);
        };
        if (dialog->exec() == GUI::Dialog::ExecResult::OK)
            Config::write_u32("Snake"sv, "Snake"sv, "BaseColor"sv, dialog->color().value());
        if (!was_paused)
            game.start();
    });
    change_snake_color->set_enabled(snake_skin_name == "Classic"sv);
    game_menu->add_action(change_snake_color);

    GUI::ActionGroup skin_action_group;
    skin_action_group.set_exclusive(true);

    auto skin_menu = game_menu->add_submenu("&Skin"_string);
    skin_menu->set_icon(app_icon.bitmap_for_size(16));

    auto add_skin_action = [&](StringView name, bool enable_color) -> ErrorOr<void> {
        auto action = GUI::Action::create_checkable(name, GUI::Shortcut {}, [&, enable_color](auto& action) {
            Config::write_string("Snake"sv, "Snake"sv, "SnakeSkin"sv, action.text());
            game.set_skin_name(String::from_byte_string(action.text()).release_value_but_fixme_should_propagate_errors());
            change_snake_color->set_enabled(enable_color);
        });

        skin_action_group.add_action(*action);
        if (snake_skin_name == name)
            action->set_checked(true);
        skin_menu->add_action(*action);
        return {};
    };

    TRY(Core::Directory::for_each_entry("/res/graphics/snake/skins/"sv, Core::DirIterator::SkipParentAndBaseDir, [&](auto& entry, auto&) -> ErrorOr<IterationDecision> {
        TRY(add_skin_action(entry.name, false));
        return IterationDecision::Continue;
    }));
    TRY(add_skin_action("Classic"sv, true));

    game_menu->add_separator();
    game_menu->add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the()->quit();
    }));

    auto view_menu = window->add_menu("&View"_string);
    view_menu->add_action(GUI::CommonActions::make_fullscreen_action([&](auto&) {
        window->set_fullscreen(!window->is_fullscreen());
    }));

    auto help_menu = window->add_menu("&Help"_string);
    help_menu->add_action(GUI::CommonActions::make_command_palette_action(window));
    help_menu->add_action(GUI::CommonActions::make_help_action([](auto&) {
        Desktop::Launcher::open(URL::create_with_file_scheme("/usr/share/man/man6/Snake.md"), "/bin/Help");
    }));
    help_menu->add_action(GUI::CommonActions::make_about_action("Snake"_string, app_icon, window));

    window->show();

    window->set_icon(app_icon.bitmap_for_size(16));

    return app->exec();
}
