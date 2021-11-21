/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "TerminalSettingsWidget.h"
#include <LibGUI/Application.h>
#include <LibGUI/SettingsWindow.h>
#include <LibGUI/WindowServerConnection.h>

// Including this after to avoid LibIPC errors
#include <LibConfig/Client.h>

int main(int argc, char** argv)
{
    if (pledge("stdio rpath cpath wpath recvfd sendfd unix proc exec", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);
    Config::pledge_domains("Terminal");

    if (pledge("stdio rpath cpath wpath recvfd sendfd proc exec", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/res", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/bin/keymap", "x") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/proc/keymap", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil(nullptr, nullptr)) {
        perror("unveil");
        return 1;
    }

    auto app_icon = GUI::Icon::default_icon("app-terminal");

    auto window = GUI::SettingsWindow::construct("Terminal Settings");
    window->set_icon(app_icon.bitmap_for_size(16));
    window->add_tab<TerminalSettingsMainWidget>("Terminal");
    window->add_tab<TerminalSettingsViewWidget>("View");

    window->show();
    return app->exec();
}
