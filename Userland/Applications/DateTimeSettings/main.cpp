/*
 * Copyright (c) 2021, The SerenityOS developers
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DateTimeSettingsWindow.h"
#include <AK/Vector.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/Menu.h>

int main(int argc, char** argv)
{
    auto app = GUI::Application::construct(argc, argv);
    auto app_icon = GUI::Icon::default_icon("app-date-time-settings");

    auto window = DateTimeSettingsWindow::construct();
    window->set_title("Date/Time Settings");
    window->resize(205, 78);
    window->set_resizable(false);
    window->set_minimizable(false);
    window->set_icon(app_icon.bitmap_for_size(16));

    auto quit_action = GUI::CommonActions::make_quit_action(
        [&](auto&) {
            app->quit();
        });

    auto& file_menu = window->add_menu("&File");
    file_menu.add_action(quit_action);

    auto& help_menu = window->add_menu("&Help");
    help_menu.add_action(GUI::CommonActions::make_about_action("Date/Time Settings", app_icon, window));

    window->show();
    return app->exec();
}
