/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@cs.toronto.edu>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MainWidget.h"
#include <LibConfig/Client.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Statusbar.h>
#include <LibGUI/Window.h>
#include <LibGfx/Painter.h>
#include <stdio.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio thread recvfd sendfd rpath unix wpath cpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);
    Config::pledge_domains("PixelPaint");

    const char* image_file = nullptr;
    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(image_file, "Image file to open", "path", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    if (unveil("/res", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/tmp/portal/clipboard", "rw") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/tmp/portal/filesystemaccess", "rw") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/tmp/portal/image", "rw") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil(nullptr, nullptr) < 0) {
        perror("unveil");
        return 1;
    }

    auto app_icon = GUI::Icon::default_icon("app-pixel-paint");

    auto window = GUI::Window::construct();
    window->set_title("Pixel Paint");
    window->resize(800, 510);
    window->set_icon(app_icon.bitmap_for_size(16));

    auto& main_widget = window->set_main_widget<PixelPaint::MainWidget>();
    main_widget.initialize_menubar(*window);

    auto& statusbar = *main_widget.find_descendant_of_type_named<GUI::Statusbar>("statusbar");

    app->on_action_enter = [&statusbar](GUI::Action& action) {
        auto text = action.status_tip();
        if (text.is_empty())
            text = Gfx::parse_ampersand_string(action.text());
        statusbar.set_override_text(move(text));
    };

    app->on_action_leave = [&statusbar](GUI::Action&) {
        statusbar.set_override_text({});
    };

    window->show();

    if (image_file) {
        auto response = FileSystemAccessClient::Client::the().request_file_read_only_approved(window->window_id(), image_file);
        if (response.error != 0) {
            if (response.error != -1)
                GUI::MessageBox::show_error(window, String::formatted("Opening \"{}\" failed: {}", *response.chosen_file, strerror(response.error)));
            return 1;
        }
        main_widget.open_image_fd(*response.fd, *response.chosen_file);
    } else {
        main_widget.create_default_image();
    }

    return app->exec();
}
