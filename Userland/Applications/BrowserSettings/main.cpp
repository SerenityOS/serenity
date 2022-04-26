/*
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BrowserSettingsWidget.h"
#include "ContentFilterSettingsWidget.h"
#include <LibConfig/Client.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/SettingsWindow.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath wpath cpath recvfd sendfd unix"));
    auto app = TRY(GUI::Application::try_create(arguments));
    Config::pledge_domain("Browser");

    StringView selected_tab;
    Core::ArgsParser args_parser;
    args_parser.add_option(selected_tab, "Tab, one of 'browser' or 'content-filtering'", "open-tab", 't', "tab");
    args_parser.parse(arguments);

    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil("/home", "r"));
    TRY(Core::System::unveil("/home/anon/.config/BrowserContentFilters.txt", "rwc"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto app_icon = GUI::Icon::default_icon("app-browser");

    auto window = TRY(GUI::SettingsWindow::create("Browser Settings", GUI::SettingsWindow::ShowDefaultsButton::Yes));
    window->set_icon(app_icon.bitmap_for_size(16));
    (void)TRY(window->add_tab<BrowserSettingsWidget>("Browser", "browser"));
    (void)TRY(window->add_tab<ContentFilterSettingsWidget>("Content Filtering", "content-filtering"));
    window->set_active_tab(selected_tab);

    window->show();
    return app->exec();
}
