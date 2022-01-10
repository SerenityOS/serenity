/*
 * Copyright (c) 2022, Simon Holm <simholm@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ClockSettingsWidget.h"
#include <LibConfig/Client.h>
#include <LibCore/System.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/SettingsWindow.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath recvfd sendfd unix"));

    auto app = TRY(GUI::Application::try_create(arguments));

    Config::pledge_domains("Taskbar");

    TRY(Core::System::pledge("stdio rpath recvfd sendfd"));

    auto app_icon = GUI::Icon::default_icon("app-settings");

    auto window = TRY(GUI::SettingsWindow::create("Taskbar Settings"));
    (void)TRY(window->add_tab<ClockSettingsWidget>("Clock"));
    window->set_icon(app_icon.bitmap_for_size(16));

    window->show();
    return app->exec();
}
