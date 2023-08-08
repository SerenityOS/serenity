/*
 * Copyright (c) 2022-2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CardSettingsWidget.h"
#include "ChessSettingsWidget.h"
#include <LibConfig/Client.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/SettingsWindow.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath recvfd sendfd unix thread"));
    auto app = TRY(GUI::Application::create(arguments));
    Config::pledge_domain("Games");

    StringView selected_tab;
    Core::ArgsParser args_parser;
    args_parser.add_option(selected_tab, "Tab, one of 'cards' or 'chess'", "open-tab", 't', "tab");
    args_parser.parse(arguments);

    TRY(Core::System::unveil("/res", "r"));
    // Both of these are used by the GUI::FileSystemModel in CardSettingsWidget.
    TRY(Core::System::unveil("/etc/passwd", "r"));
    TRY(Core::System::unveil("/etc/group", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto app_icon = GUI::Icon::default_icon("games"sv);

    auto window = TRY(GUI::SettingsWindow::create("Games Settings", GUI::SettingsWindow::ShowDefaultsButton::Yes));
    window->set_icon(app_icon.bitmap_for_size(16));
    (void)TRY(window->add_tab<GamesSettings::CardSettingsWidget>("Cards"_string, "cards"sv));
    (void)TRY(window->add_tab<GamesSettings::ChessSettingsWidget>("Chess"_string, "chess"sv));
    window->set_active_tab(selected_tab);

    window->show();
    return app->exec();
}
