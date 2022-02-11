/*
 * Copyright (c) 2019-2020, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BackgroundSettingsWidget.h"
#include "DesktopSettingsWidget.h"
#include "FontSettingsWidget.h"
#include "MonitorSettingsWidget.h"
#include <LibConfig/Client.h>
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

    auto app_icon = GUI::Icon::default_icon("app-display-settings");

    auto window = TRY(GUI::SettingsWindow::create("Display Settings"));
    (void)TRY(window->add_tab<DisplaySettings::BackgroundSettingsWidget>("Background"));
    (void)TRY(window->add_tab<DisplaySettings::FontSettingsWidget>("Fonts"));
    (void)TRY(window->add_tab<DisplaySettings::MonitorSettingsWidget>("Monitor"));
    (void)TRY(window->add_tab<DisplaySettings::DesktopSettingsWidget>("Workspaces"));

    window->set_icon(app_icon.bitmap_for_size(16));

    window->show();
    return app->exec();
}
