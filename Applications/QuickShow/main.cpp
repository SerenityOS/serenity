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

#include "QSWidget.h"
#include <AK/URL.h>
#include <LibCore/MimeData.h>
#include <LibGUI/AboutDialog.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/Label.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MenuBar.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    if (pledge("stdio shared_buffer accept rpath unix cpath fattr proc exec thread", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    GUI::Application app(argc, argv);

    if (pledge("stdio shared_buffer accept rpath proc exec thread", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

#if 0
    if (argc != 2) {
        printf("usage: qs <image-file>\n");
        return 0;
    }
#endif

    auto window = GUI::Window::construct();
    window->set_double_buffering_enabled(true);
    window->set_rect(200, 200, 300, 200);
    window->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/filetype-image.png"));

    auto& widget = window->set_main_widget<QSWidget>();
    if (argc > 1)
        widget.load_from_file(argv[1]);

    auto update_window_title = [&](int scale) {
        if (widget.bitmap())
            window->set_title(String::format("%s %s %d%% - QuickShow", widget.path().characters(), widget.bitmap()->size().to_string().characters(), scale));
        else
            window->set_title("QuickShow");
    };

    update_window_title(100);

    widget.on_scale_change = [&](int scale) {
        update_window_title(scale);
    };

    widget.on_drop = [&](auto& event) {
        window->move_to_front();

        if (event.mime_data().has_urls()) {
            auto urls = event.mime_data().urls();

            if (!urls.is_empty()) {
                auto url = urls.first();
                widget.load_from_file(url.path());
            }

            for (size_t i = 1; i < urls.size(); ++i) {
                if (fork() == 0) {
                    execl("/bin/QuickShow", "/bin/QuickShow", urls[i].path().characters(), nullptr);
                    ASSERT_NOT_REACHED();
                }
            }
        }
    };

    auto menubar = make<GUI::MenuBar>();

    auto& app_menu = menubar->add_menu("QuickShow");
    app_menu.add_action(GUI::CommonActions::make_open_action([&](auto&) {
        Optional<String> path = GUI::FilePicker::get_open_filepath("Open image...");
        if (path.has_value()) {
            widget.load_from_file(path.value());
        }
    }));
    app_menu.add_separator();
    app_menu.add_action(GUI::CommonActions::make_quit_action([&](auto&) {
        app.quit();
    }));

    auto& help_menu = menubar->add_menu("Help");
    help_menu.add_action(GUI::Action::create("About", [&](auto&) {
        GUI::AboutDialog::show("QuickShow", Gfx::Bitmap::load_from_file("/res/icons/32x32/filetype-image.png"), window);
    }));

    app.set_menubar(move(menubar));

    window->show();

    return app.exec();
}
