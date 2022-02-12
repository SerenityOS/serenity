/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "TerminalSettingsWidget.h"
#include <LibCore/System.h>
#include <LibGUI/Application.h>
#include <LibGUI/SettingsWindow.h>
#include <LibGUI/WindowServerConnection.h>
#include <LibMain/Main.h>

// Including this after to avoid LibIPC errors
#include <LibConfig/Client.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath recvfd sendfd unix"));
    auto app = TRY(GUI::Application::try_create(arguments));
    Config::pledge_domain("Terminal");

    TRY(Core::System::pledge("stdio rpath recvfd sendfd"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto app_icon = GUI::Icon::default_icon("app-terminal");

    auto window = TRY(GUI::SettingsWindow::create("Terminal Settings"));
    window->set_icon(app_icon.bitmap_for_size(16));
    (void)TRY(window->add_tab<TerminalSettingsMainWidget>("Terminal"));
    (void)TRY(window->add_tab<TerminalSettingsViewWidget>("View"));

    window->show();
    return app->exec();
}
