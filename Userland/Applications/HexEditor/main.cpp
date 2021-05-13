/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "HexEditorWidget.h"
#include <LibGUI/Icon.h>
#include <LibGUI/Menubar.h>
#include <LibGfx/Bitmap.h>
#include <stdio.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio recvfd sendfd rpath unix cpath wpath thread", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

    if (pledge("stdio recvfd sendfd rpath cpath wpath thread", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app_icon = GUI::Icon::default_icon("app-hex-editor");

    auto window = GUI::Window::construct();
    window->set_title("Hex Editor");
    window->resize(640, 400);

    auto& hex_editor_widget = window->set_main_widget<HexEditorWidget>();

    window->on_close_request = [&]() -> GUI::Window::CloseRequestDecision {
        if (hex_editor_widget.request_close())
            return GUI::Window::CloseRequestDecision::Close;
        return GUI::Window::CloseRequestDecision::StayOpen;
    };

    auto menubar = GUI::Menubar::construct();
    hex_editor_widget.initialize_menubar(menubar);
    window->set_menubar(menubar);
    window->show();
    window->set_icon(app_icon.bitmap_for_size(16));

    if (argc >= 2)
        hex_editor_widget.open_file(argv[1]);

    return app->exec();
}
