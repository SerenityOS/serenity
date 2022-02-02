/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ClockSettingsWidget.h"
#include <LibCore/System.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/SettingsWindow.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath recvfd sendfd unix proc exec"));

    auto app = TRY(GUI::Application::try_create(arguments));

    TRY(Core::System::pledge("stdio rpath recvfd sendfd proc exec"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil("/bin/timezone", "x"));
    TRY(Core::System::unveil("/etc/timezone", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto app_icon = GUI::Icon::default_icon("app-analog-clock"); // FIXME: Create a ClockSettings icon.

    auto window = TRY(GUI::SettingsWindow::create("Clock Settings", GUI::SettingsWindow::ShowDefaultsButton::Yes));
    (void)TRY(window->add_tab<ClockSettingsWidget>("Clock"));
    window->set_icon(app_icon.bitmap_for_size(16));
    window->resize(540, 570);

    window->show();
    return app->exec();
}
