/*
 * Copyright (c) 2022, Oleg Kosenkov <oleg@kosenkov.ca>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ColorLines.h"
#include <LibConfig/Client.h>
#include <LibCore/System.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/Window.h>
#include <LibMain/Main.h>
#include <LibURL/URL.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath recvfd sendfd unix"));

    auto app = TRY(GUI::Application::create(arguments));

    auto const app_name = "ColorLines"sv;
    auto const title = "Color Lines"_string;
    auto const man_file = "/usr/share/man/man6/ColorLines.md"sv;

    Config::pledge_domain(app_name);

    TRY(Desktop::Launcher::add_allowed_handler_with_only_specific_urls("/bin/Help", { URL::create_with_file_scheme(man_file) }));
    TRY(Desktop::Launcher::seal_allowlist());

    TRY(Core::System::pledge("stdio rpath recvfd sendfd"));

    TRY(Core::System::unveil("/tmp/session/%sid/portal/launch", "rw"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto app_icon = TRY(GUI::Icon::try_create_default_icon("app-colorlines"sv));

    auto window = GUI::Window::construct();

    window->set_double_buffering_enabled(false);
    window->set_title(title.bytes_as_string_view());
    window->resize(436, 481);
    window->set_resizable(false);

    auto game = window->set_main_widget<ColorLines>(app_name);

    auto game_menu = window->add_menu("&Game"_string);

    game_menu->add_action(GUI::Action::create("&New Game", { Mod_None, Key_F2 }, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/reload.png"sv)), [&](auto&) {
        game->reset();
    }));
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
    help_menu->add_action(GUI::CommonActions::make_help_action([&man_file](auto&) {
        Desktop::Launcher::open(URL::create_with_file_scheme(man_file), "/bin/Help");
    }));
    help_menu->add_action(GUI::CommonActions::make_about_action(title, app_icon, window));

    window->show();

    window->set_icon(app_icon.bitmap_for_size(16));

    return app->exec();
}
