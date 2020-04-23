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

#include "WindowActions.h"
#include "InspectorWidget.h"
#include "Tab.h"
#include <LibCore/File.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/TabWidget.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <LibWeb/ResourceLoader.h>
#include <stdio.h>
#include <stdlib.h>

static const char* home_url = "file:///home/anon/www/welcome.html";

int main(int argc, char** argv)
{
    if (getuid() == 0) {
        fprintf(stderr, "Refusing to run as root\n");
        return 1;
    }

    if (pledge("stdio shared_buffer accept unix cpath rpath wpath fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    GUI::Application app(argc, argv);

    // Connect to the ProtocolServer immediately so we can drop the "unix" pledge.
    Web::ResourceLoader::the();

    if (pledge("stdio shared_buffer accept cpath rpath wpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/home", "rwc") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/res", "r") < 0) {
        perror("unveil");
        return 1;
    }

    unveil(nullptr, nullptr);

    auto window = GUI::Window::construct();
    window->set_rect(100, 100, 640, 480);

    auto& widget = window->set_main_widget<GUI::Widget>();
    widget.set_fill_with_background_color(true);
    widget.set_layout<GUI::VerticalBoxLayout>();
    widget.layout()->set_spacing(2);

    auto& tab_widget = widget.add<GUI::TabWidget>();

    tab_widget.on_change = [&](auto& active_widget) {
        auto& tab = static_cast<Browser::Tab&>(active_widget);
        window->set_title(String::format("%s - Browser", tab.title().characters()));
        tab.did_become_active();
    };

    Browser::WindowActions window_actions(*window);

    auto create_new_tab = [&](bool activate = true) {
        auto& new_tab = tab_widget.add_tab<Browser::Tab>("New tab");

        new_tab.on_title_change = [&](auto title) {
            tab_widget.set_tab_title(new_tab, title);
            if (tab_widget.active_widget() == &new_tab)
                window->set_title(String::format("%s - Browser", title.characters()));
        };

        new_tab.on_tab_close_request = [&](auto& tab) {
            tab_widget.deferred_invoke([&](auto&) {
                tab_widget.remove_tab(tab);
                if (tab_widget.children().is_empty())
                    app.quit();
            });
        };

        window->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/filetype-html.png"));

        window->set_title("Browser");
        window->show();

        URL url_to_load = home_url;

        if (app.args().size() >= 1) {
            url_to_load = URL::create_with_url_or_path(app.args()[0]);
        }

        new_tab.load(url_to_load);

        dbg() << "Added new tab " << &new_tab << ", loading " << url_to_load;

        if (activate)
            tab_widget.set_active_widget(&new_tab);
    };

    window_actions.on_create_new_tab = [&] {
        create_new_tab();
    };

    window_actions.on_next_tab = [&] {
        tab_widget.activate_next_tab();
    };

    window_actions.on_previous_tab = [&] {
        tab_widget.activate_previous_tab();
    };

    create_new_tab();

    return app.exec();
}
