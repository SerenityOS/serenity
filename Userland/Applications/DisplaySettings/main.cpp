/*
 * Copyright (c) 2019-2020, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BackgroundSettingsWidget.h"
#include "DesktopSettingsWidget.h"
#include "FontSettingsWidget.h"
#include "MonitorSettingsWidget.h"
#include "ThemesSettingsWidget.h"
#include <LibConfig/Client.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/SettingsWindow.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio thread recvfd sendfd rpath cpath wpath unix"));

    auto app = TRY(GUI::Application::try_create(arguments));
    Config::pledge_domain("WindowManager");

    StringView selected_tab;
    Core::ArgsParser args_parser;
    args_parser.add_option(selected_tab, "Tab, one of 'background', 'fonts', 'monitor', 'themes', or 'workspaces'", "open-tab", 't', "tab");
    args_parser.parse(arguments);

    auto app_icon = GUI::Icon::default_icon("app-display-settings");

    bool background_settings_changed = false;

    auto window = TRY(GUI::SettingsWindow::create("Display Settings"));
    (void)TRY(window->add_tab<DisplaySettings::BackgroundSettingsWidget>("Background", "background", background_settings_changed));
    (void)TRY(window->add_tab<DisplaySettings::ThemesSettingsWidget>("Themes", "themes", background_settings_changed));
    (void)TRY(window->add_tab<DisplaySettings::FontSettingsWidget>("Fonts", "fonts"));
    (void)TRY(window->add_tab<DisplaySettings::MonitorSettingsWidget>("Monitor", "monitor"));
    (void)TRY(window->add_tab<DisplaySettings::DesktopSettingsWidget>("Workspaces", "workspaces"));
    window->set_active_tab(selected_tab);

    window->set_icon(app_icon.bitmap_for_size(16));

    window->show();
    return app->exec();
}
