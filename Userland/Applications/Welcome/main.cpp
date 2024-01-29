/*
 * Copyright (c) 2021-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "WelcomeWidget.h"
#include <LibConfig/Client.h>
#include <LibCore/System.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Window.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd rpath unix proc exec"));
    auto app = TRY(GUI::Application::create(arguments));

    Config::pledge_domain("SystemServer");

    TRY(Core::System::unveil("/tmp/session/%sid/portal/webcontent", "rw"));
    TRY(Core::System::unveil("/tmp/session/%sid/portal/filesystemaccess", "rw"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil("/home", "r"));
    TRY(Core::System::unveil("/usr/share/Welcome", "r"));
    TRY(Core::System::unveil("/bin/Help", "x"));
    TRY(Core::System::unveil(nullptr, nullptr));
    auto app_icon = TRY(GUI::Icon::try_create_default_icon("app-welcome"sv));

    auto window = GUI::Window::construct();
    window->resize(480, 250);
    window->center_on_screen();
    window->set_title("Welcome");
    window->set_icon(app_icon.bitmap_for_size(16));
    auto welcome_widget = TRY(Welcome::WelcomeWidget::create());
    window->set_main_widget(welcome_widget);

    window->show();

    return app->exec();
}
