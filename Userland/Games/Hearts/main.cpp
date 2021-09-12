/*
 * Copyright (c) 2020, Till Mayer <till.mayer@web.de>
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@cs.toronto.edu>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Game.h"
#include "SettingsDialog.h"
#include <Games/Hearts/HeartsGML.h>
#include <LibConfig/Client.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Statusbar.h>
#include <LibGUI/Window.h>
#include <stdio.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    auto app = GUI::Application::construct(argc, argv);
    auto app_icon = GUI::Icon::default_icon("app-hearts");

    Config::pledge_domains("Hearts");

    if (pledge("stdio recvfd sendfd rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/res", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil(nullptr, nullptr) < 0) {
        perror("unveil");
        return 1;
    }

    auto window = GUI::Window::construct();
    window->set_title("Hearts");

    auto& widget = window->set_main_widget<GUI::Widget>();
    widget.load_from_gml(hearts_gml);

    auto& game = *widget.find_descendant_of_type_named<Hearts::Game>("game");
    game.set_focus(true);

    auto& statusbar = *widget.find_descendant_of_type_named<GUI::Statusbar>("statusbar");
    statusbar.set_text(0, "Score: 0");

    String player_name = Config::read_string("Hearts", "", "player_name", "Gunnar");

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
        if (settings_dialog->exec() || settings_dialog->result() != GUI::Dialog::ExecOK)
            return;

        player_name = settings_dialog->player_name();

        Config::write_string("Hearts", "", "player_name", player_name);

        GUI::MessageBox::show(window, "Settings have been successfully saved and will take effect in the next game.", "Settings Changed Successfully", GUI::MessageBox::Type::Information);
    };

    auto& game_menu = window->add_menu("&Game");

    game_menu.add_action(GUI::Action::create("&New Game", { Mod_None, Key_F2 }, [&](auto&) {
        game.setup(player_name);
    }));
    game_menu.add_separator();
    game_menu.add_action(GUI::Action::create("&Settings...", [&](auto&) {
        change_settings();
    }));
    game_menu.add_separator();
    game_menu.add_action(GUI::CommonActions::make_quit_action([&](auto&) { app->quit(); }));

    auto& help_menu = window->add_menu("&Help");
    help_menu.add_action(GUI::CommonActions::make_about_action("Hearts", app_icon, window));

    window->set_resizable(false);
    window->resize(Hearts::Game::width, Hearts::Game::height + statusbar.max_height());
    window->set_icon(app_icon.bitmap_for_size(16));
    window->show();
    game.setup(player_name);

    return app->exec();
}
