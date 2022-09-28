/*
 * Copyright (c) 2020, Till Mayer <till.mayer@web.de>
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Game.h"
#include "SettingsDialog.h"
#include <AK/URL.h>
#include <Games/Hearts/HeartsGML.h>
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
#include <stdio.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    auto app = TRY(GUI::Application::try_create(arguments));
    auto app_icon = TRY(GUI::Icon::try_create_default_icon("app-hearts"sv));

    Config::pledge_domains({ "Games", "Hearts" });
    Config::monitor_domain("Games");

    TRY(Core::System::pledge("stdio recvfd sendfd rpath unix"));

    TRY(Desktop::Launcher::add_allowed_handler_with_only_specific_urls("/bin/Help", { URL::create_with_file_scheme("/usr/share/man/man6/Hearts.md") }));
    TRY(Desktop::Launcher::seal_allowlist());

    TRY(Core::System::pledge("stdio recvfd sendfd rpath"));

    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil("/tmp/user/%uid/portal/launch", "rw"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto window = TRY(GUI::Window::try_create());
    window->set_title("Hearts");

    auto widget = TRY(window->try_set_main_widget<GUI::Widget>());
    widget->load_from_gml(hearts_gml);

    auto& game = *widget->find_descendant_of_type_named<Hearts::Game>("game");
    game.set_focus(true);

    auto& statusbar = *widget->find_descendant_of_type_named<GUI::Statusbar>("statusbar");
    statusbar.set_text(0, "Score: 0");

    String player_name = Config::read_string("Hearts"sv, ""sv, "player_name"sv, "Gunnar"sv);

    game.on_status_change = [&](const AK::StringView& status) {
        statusbar.set_override_text(status);
    };

    app->on_action_enter = [&](GUI::Action& action) {
        auto text = action.status_tip();
        if (text.is_empty())
            text = Gfx::parse_ampersand_string(action.text());
        statusbar.set_override_text(move(text));
    };

    app->on_action_leave = [&](GUI::Action&) {
        statusbar.set_override_text({});
    };

    auto change_settings = [&] {
        auto settings_dialog = SettingsDialog::construct(window, player_name);
        if (settings_dialog->exec() != GUI::Dialog::ExecResult::OK)
            return;

        player_name = settings_dialog->player_name();

        Config::write_string("Hearts"sv, ""sv, "player_name"sv, player_name);

        GUI::MessageBox::show(window, "Settings have been successfully saved and will take effect in the next game."sv, "Settings Changed Successfully"sv, GUI::MessageBox::Type::Information);
    };

    auto game_menu = TRY(window->try_add_menu("&Game"));

    TRY(game_menu->try_add_action(GUI::Action::create("&New Game", { Mod_None, Key_F2 }, TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/reload.png"sv)), [&](auto&) {
        game.setup(player_name);
    })));
    TRY(game_menu->try_add_separator());
    TRY(game_menu->try_add_action(GUI::Action::create("&Settings", TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/settings.png"sv)), [&](auto&) {
        change_settings();
    })));
    TRY(game_menu->try_add_separator());
    TRY(game_menu->try_add_action(GUI::CommonActions::make_quit_action([&](auto&) { app->quit(); })));

    auto help_menu = TRY(window->try_add_menu("&Help"));
    TRY(help_menu->try_add_action(GUI::CommonActions::make_help_action([](auto&) {
        Desktop::Launcher::open(URL::create_with_file_scheme("/usr/share/man/man6/Hearts.md"), "/bin/Help");
    })));
    TRY(help_menu->try_add_action(GUI::CommonActions::make_about_action("Hearts", app_icon, window)));

    window->set_resizable(false);
    window->resize(Hearts::Game::width, Hearts::Game::height + statusbar.max_height().as_int());
    window->set_icon(app_icon.bitmap_for_size(16));
    window->show();
    game.setup(player_name);

    return app->exec();
}
