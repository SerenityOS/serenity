/*
 * Copyright (c) 2022, Oleg Kosenkov <oleg@kosenkov.ca>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ColorLines.h"
#include <AK/URL.h>
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

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath recvfd sendfd unix"));

    auto app = TRY(GUI::Application::try_create(arguments));

    auto const app_name = "ColorLines"sv;
    auto const title = "Color Lines"sv;
    auto const man_file = "/usr/share/man/man6/ColorLines.md"sv;

    Config::pledge_domain(app_name);

    TRY(Desktop::Launcher::add_allowed_handler_with_only_specific_urls("/bin/Help", { URL::create_with_file_scheme(man_file) }));
    TRY(Desktop::Launcher::seal_allowlist());

    TRY(Core::System::pledge("stdio rpath recvfd sendfd"));

    TRY(Core::System::unveil("/tmp/session/%sid/portal/launch", "rw"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto app_icon = TRY(GUI::Icon::try_create_default_icon("app-colorlines"sv));

    auto window = TRY(GUI::Window::try_create());

    window->set_double_buffering_enabled(false);
    window->set_title(title);
    window->resize(436, 481);
    window->set_resizable(false);

    auto game = TRY(window->set_main_widget<ColorLines>(app_name));

    auto game_menu = TRY(window->try_add_menu("&Game"));

    TRY(game_menu->try_add_action(GUI::Action::create("&New Game", { Mod_None, Key_F2 }, TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/reload.png"sv)), [&](auto&) {
        game->reset();
    })));
    TRY(game_menu->try_add_separator());
    TRY(game_menu->try_add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the()->quit();
    })));

    auto help_menu = TRY(window->try_add_menu("&Help"));
    TRY(help_menu->try_add_action(GUI::CommonActions::make_command_palette_action(window)));
    TRY(help_menu->try_add_action(GUI::CommonActions::make_help_action([&man_file](auto&) {
        Desktop::Launcher::open(URL::create_with_file_scheme(man_file), "/bin/Help");
    })));
    TRY(help_menu->try_add_action(GUI::CommonActions::make_about_action(title, app_icon, window)));

    window->show();

    window->set_icon(app_icon.bitmap_for_size(16));

    return app->exec();
}
