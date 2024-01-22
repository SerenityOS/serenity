/*
 * Copyright (c) 2022-2022, Olivier De Cannière <olivier.decanniere96@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CalendarSettingsWidget.h"
#include <LibConfig/Client.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/SettingsWindow.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath recvfd sendfd unix"));

    auto app = TRY(GUI::Application::create(arguments));

    Config::pledge_domain("Calendar");

    StringView selected_tab;
    Core::ArgsParser args_parser;
    args_parser.add_option(selected_tab, "Tab, only option is 'calendar'", "open-tab", 't', "tab");
    args_parser.parse(arguments);

    TRY(Core::System::pledge("stdio rpath recvfd sendfd"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto app_icon = GUI::Icon::default_icon("app-calendar"sv);

    auto window = TRY(GUI::SettingsWindow::create("Calendar Settings", GUI::SettingsWindow::ShowDefaultsButton::Yes));
    (void)TRY(window->add_tab<CalendarSettings::CalendarSettingsWidget>("Calendar"_string, "Calendar"sv));
    window->set_icon(app_icon.bitmap_for_size(16));
    window->set_active_tab(selected_tab);

    window->show();
    return app->exec();
}
