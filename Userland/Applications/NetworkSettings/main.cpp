/*
 * Copyright (c) 2022, Maciej <sppmacd@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "NetworkSettingsWidget.h"
#include <LibGUI/MessageBox.h>
#include <unistd.h>

#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/SettingsWindow.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments args)
{
    TRY(Core::System::pledge("stdio rpath wpath cpath recvfd sendfd unix proc exec"));

    TRY(Core::System::unveil("/bin/NetworkServer", "x"));
    TRY(Core::System::unveil("/etc/Network.ini", "rwc"));
    TRY(Core::System::unveil("/proc/all", "r"));
    TRY(Core::System::unveil("/proc/net/adapters", "r"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil("/tmp/session/%sid/portal/clipboard", "rw"));
    TRY(Core::System::unveil("/tmp/portal/window", "rw"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto app = TRY(GUI::Application::try_create(args));

    if (getuid() != 0) {
        GUI::MessageBox::show_error(nullptr, "You need to be root to run Network Settings!"sv);
        return 1;
    }

    TRY(Core::System::pledge("stdio rpath wpath cpath recvfd sendfd proc exec"));

    auto app_icon = GUI::Icon::default_icon("network"sv);
    auto window = TRY(GUI::SettingsWindow::create("Network Settings", GUI::SettingsWindow::ShowDefaultsButton::No));
    (void)TRY(window->add_tab<NetworkSettings::NetworkSettingsWidget>("Network"sv, "network"sv));
    window->set_icon(app_icon.bitmap_for_size(16));

    window->show();
    return app->exec();
}
