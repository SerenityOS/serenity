/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MailWidget.h"
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/Window.h>
#include <stdio.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio recvfd sendfd rpath unix cpath wpath thread inet", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

    auto window = GUI::Window::construct();

    auto app_icon = GUI::Icon::default_icon("app-mail");
    window->set_icon(app_icon.bitmap_for_size(16));

    auto& mail_widget = window->set_main_widget<MailWidget>();

    window->set_title("Mail");
    window->resize(640, 400);

    auto menubar = GUI::Menubar::construct();

    auto& file_menu = menubar->add_menu("&File");

    file_menu.add_action(GUI::CommonActions::make_quit_action([&](auto&) {
        mail_widget.on_window_close();
        app->quit();
    }));

    auto& help_menu = menubar->add_menu("&Help");
    help_menu.add_action(GUI::CommonActions::make_about_action("Mail", app_icon, window));

    window->on_close_request = [&] {
        mail_widget.on_window_close();
        return GUI::Window::CloseRequestDecision::Close;
    };

    window->set_menubar(menubar);

    window->show();

    bool should_continue = mail_widget.connect_and_login();
    if (!should_continue)
        return 1;

    return app->exec();
}
