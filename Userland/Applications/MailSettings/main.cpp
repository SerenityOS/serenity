/*
 * Copyright (c) 2021, the SerenityOS developers.
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MailSettingsWidget.h"
#include <LibConfig/Client.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/SettingsWindow.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio rpath recvfd sendfd unix", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

    Config::pledge_domains("Mail");

    if (pledge("stdio rpath recvfd sendfd", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/res", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil(nullptr, nullptr)) {
        perror("unveil");
        return 1;
    }

    auto app_icon = GUI::Icon::default_icon("app-mail");

    auto window = GUI::SettingsWindow::construct("Mail Settings", GUI::SettingsWindow::ShowDefaultsButton::Yes);
    window->add_tab<MailSettingsWidget>("Mail");
    window->set_icon(app_icon.bitmap_for_size(16));

    window->show();
    return app->exec();
}
