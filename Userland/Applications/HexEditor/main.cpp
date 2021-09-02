/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@cs.toronto.edu>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "HexEditorWidget.h"
#include <LibConfig/Client.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <stdio.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio recvfd sendfd rpath unix cpath wpath thread", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

    Config::pledge_domains("HexEditor");

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

    String file_to_edit = (argc > 1) ? Core::File::absolute_path(argv[1]) : "";

    if (!file_to_edit.is_empty()) {
        if (Core::File::exists(file_to_edit)) {
            dbgln("unveil for: {}", file_to_edit);
            if (unveil(file_to_edit.characters(), "r") < 0) {
                perror("unveil");
                return 1;
            }
        } else {
            file_to_edit = {};
        }
    }

    if (unveil("/res", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/tmp/portal/filesystemaccess", "rw") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil(nullptr, nullptr) < 0) {
        perror("unveil");
        return 1;
    }

    hex_editor_widget.initialize_menubar(*window);
    window->show();
    window->set_icon(app_icon.bitmap_for_size(16));

    if (!file_to_edit.is_empty()) {
        auto file = Core::File::open(file_to_edit, Core::OpenMode::ReadOnly);

        if (file.is_error()) {
            GUI::MessageBox::show_error(window, String::formatted("Opening \"{}\" failed: {}", file_to_edit, file.error()));
            return 1;
        }

        hex_editor_widget.open_file(file.value()->leak_fd(), file_to_edit);
    }

    return app->exec();
}
