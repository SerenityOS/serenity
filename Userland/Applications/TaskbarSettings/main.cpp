/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "TaskbarSettingsWidget.h"
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
    Config::pledge_domain("Taskbar");

    TRY(Core::System::pledge("stdio rpath recvfd sendfd"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto window = TRY(GUI::SettingsWindow::create("Taskbar Settings", GUI::SettingsWindow::ShowDefaultsButton::Yes));

    auto app_icon = GUI::Icon::default_icon("desktop");
    window->set_icon(app_icon.bitmap_for_size(16));

    (void)TRY(window->add_tab<TaskbarSettingsWidget>("Taskbar"));

    window->show();
    return app->exec();
}
