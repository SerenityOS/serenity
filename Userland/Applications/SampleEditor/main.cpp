/*
 * Copyright (c) 2025, Lee Hanken
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibConfig/Client.h>
#include <LibCore/System.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibMain/Main.h>
#include <LibURL/URL.h>

#include "MainWidget.h"

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge(
        "stdio recvfd sendfd rpath unix cpath wpath thread fattr proc"));

    auto app = TRY(GUI::Application::create(arguments));

    auto const man_file = "/usr/share/man/man1/Applications/SampleEditor.md";

    TRY(Desktop::Launcher::add_allowed_handler_with_only_specific_urls("/bin/Help", { URL::create_with_file_scheme(man_file) }));
    TRY(Desktop::Launcher::seal_allowlist());

    Config::pledge_domain("SampleEditor");

    auto app_icon = GUI::Icon::default_icon("app-sample-editor"sv);
    auto window = GUI::Window::construct();
    window->set_title("Sample Editor");
    window->resize(720, 360);
    window->set_icon(app_icon.bitmap_for_size(16));

    auto main_widget = TRY(MainWidget::try_create());
    window->set_main_widget(main_widget);
    TRY(main_widget->initialize_menu_and_toolbar(window));

    TRY(Core::System::unveil("/", "r"));
    TRY(Core::System::unveil("/etc", "r"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil("/home", "rwc"));
    TRY(Core::System::unveil("/home/anon", "rwc"));
    TRY(Core::System::unveil("/tmp", "rwc"));
    TRY(Core::System::unveil(nullptr, nullptr));

    if (arguments.argc > 1) {
        TRY(main_widget->open(arguments.strings[1]));
    }

    window->show();
    return app->exec();
}
