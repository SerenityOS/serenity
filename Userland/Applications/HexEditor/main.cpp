/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "HexEditorWidget.h"
#include <LibConfig/Client.h>
#include <LibFileSystemAccessClient/Client.h>
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

    if (argc > 1) {
        auto response = FileSystemAccessClient::Client::the().request_file_read_only_approved(window->window_id(), argv[1]);

        if (response.error != 0) {
            if (response.error != -1)
                GUI::MessageBox::show_error(window, String::formatted("Opening \"{}\" failed: {}", *response.chosen_file, strerror(response.error)));
            return 1;
        }

        hex_editor_widget.open_file(*response.fd, *response.chosen_file);
    }

    return app->exec();
}
