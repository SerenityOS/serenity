/*
 * Copyright (c) 2019-2020, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DisplaySettings.h"
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/TabWidget.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <stdio.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio thread recvfd sendfd rpath accept cpath wpath unix fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

    if (pledge("stdio thread recvfd sendfd rpath accept cpath wpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app_icon = GUI::Icon::default_icon("app-display-settings");

    // Let's create the tab pane that we'll hook our widgets up to :^)
    auto tab_widget = GUI::TabWidget::construct();
    tab_widget->add_tab<DisplaySettingsWidget>("Display Settings");
    tab_widget->set_fill_with_background_color(true); // No black backgrounds!

    auto window = GUI::Window::construct();
    dbgln("main window: {}", window);
    window->set_title("Display Settings");
    window->resize(360, 410);
    window->set_resizable(false);
    window->set_main_widget(tab_widget.ptr());
    window->set_icon(app_icon.bitmap_for_size(16));

    auto menubar = GUI::Menubar::construct();

    auto& file_menu = menubar->add_menu("File");
    file_menu.add_action(GUI::CommonActions::make_quit_action([&](const GUI::Action&) {
        app->quit();
    }));

    auto& help_menu = menubar->add_menu("Help");
    help_menu.add_action(GUI::CommonActions::make_about_action("Display Settings", app_icon, window));

    window->set_menubar(move(menubar));
    window->show();
    return app->exec();
}
