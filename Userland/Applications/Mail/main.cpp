/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2021, Undefine <cqundefine@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MailWidget.h"
#include <LibConfig/Client.h>
#include <LibCore/System.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/Window.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd rpath unix inet"));

    auto app = TRY(GUI::Application::create(arguments));

    Config::pledge_domain("Mail");

    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil("/etc", "r"));
    TRY(Core::System::unveil("/tmp/session/%sid/portal/webcontent", "rw"));
    TRY(Core::System::unveil("/tmp/portal/lookup", "rw"));
    TRY(Core::System::unveil("/tmp/session/%sid/portal/launch", "rw"));
    TRY(Core::System::unveil(nullptr, nullptr));

    TRY(Desktop::Launcher::add_allowed_url(URL::create_with_file_scheme("/bin/MailSettings")));
    TRY(Desktop::Launcher::add_allowed_handler_with_any_url("/bin/MailSettings"));
    TRY(Desktop::Launcher::seal_allowlist());

    auto window = GUI::Window::construct();

    auto app_icon = GUI::Icon::default_icon("app-mail"sv);
    window->set_icon(app_icon.bitmap_for_size(16));

    auto mail_widget = window->set_main_widget<MailWidget>();

    window->set_title("Mail");
    window->restore_size_and_position("Mail"sv, "Window"sv, { { 640, 400 } });
    window->save_size_and_position_on_close("Mail"sv, "Window"sv);

    auto file_menu = window->add_menu("&File"_string);

    file_menu->add_action(GUI::CommonActions::make_quit_action([&](auto&) {
        mail_widget->on_window_close();
        app->quit();
    }));

    auto view_menu = window->add_menu("&View"_string);
    view_menu->add_action(GUI::CommonActions::make_fullscreen_action([&](auto&) {
        window->set_fullscreen(!window->is_fullscreen());
    }));

    auto help_menu = window->add_menu("&Help"_string);
    help_menu->add_action(GUI::CommonActions::make_command_palette_action(window));
    help_menu->add_action(GUI::CommonActions::make_about_action("Mail"_string, app_icon, window));

    window->on_close_request = [&] {
        mail_widget->on_window_close();
        return GUI::Window::CloseRequestDecision::Close;
    };

    window->show();

    bool should_continue = TRY(mail_widget->connect_and_login());
    if (!should_continue) {
        return 1;
    }

    return app->exec();
}
