/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include "DisplayProperties.h"
#include <LibGUI/AboutDialog.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MenuBar.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    if (pledge("stdio shared_buffer rpath accept unix cpath wpath fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    GUI::Application app(argc, argv);

    if (pledge("stdio shared_buffer rpath accept cpath wpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    DisplayPropertiesWidget instance;

    auto window = GUI::Window::construct();
    window->set_title("Display Properties");
    window->move_to(100, 100);
    window->resize(400, 448);
    window->set_resizable(false);
    window->set_main_widget(instance.root_widget());
    window->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-display-properties.png"));

    // Let's create the menubar first
    auto menubar = make<GUI::MenuBar>();

    auto app_menu = GUI::Menu::construct("Display Properties");
    app_menu->add_action(GUI::CommonActions::make_quit_action([&](const GUI::Action&) {
        app.quit();
    }));
    menubar->add_menu(move(app_menu));

    auto help_menu = GUI::Menu::construct("Help");
    help_menu->add_action(GUI::Action::create("About", [&](const GUI::Action&) {
        GUI::AboutDialog::show("Display Properties", Gfx::Bitmap::load_from_file("/res/icons/32x32/app-display-properties.png"), window);
    }));
    menubar->add_menu(move(help_menu));

    app.set_menubar(move(menubar));
    window->show();
    return app.exec();
}
