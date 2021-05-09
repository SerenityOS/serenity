/*
 * Copyright (c) 2021, Richard Gráčik <r.gracik@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CatDog.h"
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/Window.h>

int main(int argc, char** argv)
{
    if (pledge("stdio recvfd sendfd rpath wpath cpath accept unix fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);
    auto app_icon = GUI::Icon::default_icon("app-catdog");

    if (pledge("stdio recvfd sendfd rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/res", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil(nullptr, nullptr) < 0) {
        perror("unveil");
        return 1;
    }

    auto window = GUI::Window::construct();
    window->set_title("CatDog Demo");
    window->resize(32, 32);
    window->set_frameless(true);
    window->set_resizable(false);
    window->set_has_alpha_channel(true);
    window->set_alpha_hit_threshold(1.0f);
    window->set_icon(app_icon.bitmap_for_size(16));

    auto& root_widget = window->set_main_widget<CatDog>();
    root_widget.set_layout<GUI::VerticalBoxLayout>();
    root_widget.layout()->set_spacing(0);

    auto menubar = GUI::Menubar::construct();
    auto& file_menu = menubar->add_menu("File");
    file_menu.add_action(GUI::CommonActions::make_quit_action([&](auto&) { app->quit(); }));
    auto& help_menu = menubar->add_menu("Help");
    help_menu.add_action(GUI::CommonActions::make_about_action("CatDog Demo", app_icon, window));
    window->set_menubar(move(menubar));

    window->show();
    root_widget.track_cursor_globally();
    root_widget.start_timer(250, Core::TimerShouldFireWhenNotVisible::Yes);
    root_widget.start_the_timer(); // timer for "mouse sleep detection"

    return app->exec();
}
