/*
 * Copyright (c) 2023, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MapsSettingsWidget.h"
#include <LibConfig/Client.h>
#include <LibCore/System.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/SettingsWindow.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd rpath unix"));

    auto app = TRY(GUI::Application::create(arguments));

    Config::pledge_domain("Maps");
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil("/usr/share/Maps", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto app_icon = GUI::Icon::default_icon("app-maps"sv);
    auto window = TRY(GUI::SettingsWindow::create("Maps Settings", GUI::SettingsWindow::ShowDefaultsButton::Yes));
    window->set_icon(app_icon.bitmap_for_size(16));

    (void)TRY(window->add_tab<MapsSettings::MapsSettingsWidget>("Maps"_string, "maps"sv));

    window->show();
    return app->exec();
}
