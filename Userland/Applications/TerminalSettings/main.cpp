/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MainWidget.h"
#include "ViewWidget.h"
#include <LibConfig/Client.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibGUI/Application.h>
#include <LibGUI/ConnectionToWindowServer.h>
#include <LibGUI/SettingsWindow.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath recvfd sendfd unix"));
    auto app = TRY(GUI::Application::create(arguments));
    Config::pledge_domain("Terminal");

    StringView selected_tab;
    Core::ArgsParser args_parser;
    args_parser.add_option(selected_tab, "Tab, one of 'terminal' or 'view'", "open-tab", 't', "tab");
    args_parser.parse(arguments);

    TRY(Core::System::pledge("stdio rpath recvfd sendfd"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto app_icon = GUI::Icon::default_icon("app-terminal"sv);

    auto window = TRY(GUI::SettingsWindow::create("Terminal Settings"));
    window->set_icon(app_icon.bitmap_for_size(16));
    (void)TRY(window->add_tab(TRY(TerminalSettings::ViewWidget::create()), "View"_string, "view"sv));
    (void)TRY(window->add_tab(TRY(TerminalSettings::MainWidget::create()), "Terminal"_string, "terminal"sv));
    window->set_active_tab(selected_tab);

    window->show();
    return app->exec();
}
