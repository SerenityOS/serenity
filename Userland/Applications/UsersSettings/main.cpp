/*
 * Copyright (c) 2026, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "UsersTab.h"
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/SettingsWindow.h>
#include <LibMain/Main.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd rpath wpath cpath fattr chown unix proc exec"));

    auto app = TRY(GUI::Application::create(arguments));

    TRY(Core::System::unveil("/bin/useradd", "x"));
    TRY(Core::System::unveil("/bin/userdel", "x"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil("/etc", "rwc"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto app_icon = TRY(GUI::Icon::try_create_default_icon("app-user-settings"sv));

    auto window = TRY(GUI::SettingsWindow::create("Users Settings"));
    window->set_icon(app_icon.bitmap_for_size(16));

    if (getuid() != 0) {
        auto error_message = "UsersSettings must be run as root in order to manage users."sv;
        GUI::MessageBox::show_error(window, error_message);
        return Error::from_string_view(error_message);
    }

    auto users_tab = TRY(UsersSettings::UsersTab::try_create());
    TRY(window->add_tab(users_tab, "Users"_string, "users"sv));

    window->show();
    return app->exec();
}
