/*
 * Copyright (c) 2020, Till Mayer <till.mayer@web.de>
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

#include "SolitaireWidget.h"
#include <LibGUI/AboutDialog.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MenuBar.h>
#include <LibGUI/Window.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    GUI::Application app(argc, argv);

    if (pledge("stdio rpath shared_buffer", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto window = GUI::Window::construct();

    window->set_resizable(false);
    window->set_rect(300, 100, SolitaireWidget::width, SolitaireWidget::height);

    auto widget = SolitaireWidget::construct(window, [&](uint32_t score) {
        window->set_title(String::format("Score: %u - Solitaire", score));
    });

    auto menu_bar = make<GUI::MenuBar>();
    auto app_menu = GUI::Menu::construct("Solitaire");
    auto help_menu = GUI::Menu::construct("Help");

    app_menu->add_action(GUI::Action::create("Restart game", [&](auto&) { widget->setup(); }));
    app_menu->add_separator();
    app_menu->add_action(GUI::CommonActions::make_quit_action([&](auto&) { app.quit(); }));

    help_menu->add_action(GUI::Action::create("About", [&](auto&) {
        GUI::AboutDialog::show("Solitaire", Gfx::Bitmap::load_from_file("/res/icons/32x32/app-solitaire.png"));
    }));

    menu_bar->add_menu(move(app_menu));
    menu_bar->add_menu(move(help_menu));

    app.set_menubar(move(menu_bar));
    window->set_main_widget(widget);
    window->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-solitaire.png"));
    window->show();
    widget->setup();

    return app.exec();
}
