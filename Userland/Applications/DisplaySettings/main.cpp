/*
 * Copyright (c) 2019-2020, Jesse Buhagiar <jooster669@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "DisplaySettings.h"
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MenuBar.h>
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

    auto menubar = GUI::MenuBar::construct();

    auto& app_menu = menubar->add_menu("File");
    app_menu.add_action(GUI::CommonActions::make_quit_action([&](const GUI::Action&) {
        app->quit();
    }));

    auto& help_menu = menubar->add_menu("Help");
    help_menu.add_action(GUI::CommonActions::make_about_action("Display Settings", app_icon, window));

    window->set_menubar(move(menubar));
    window->show();
    return app->exec();
}
