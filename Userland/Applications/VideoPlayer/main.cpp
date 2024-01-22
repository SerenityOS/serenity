/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibConfig/Client.h>
#include <LibCore/ArgsParser.h>
#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Window.h>
#include <LibMain/Main.h>

#include "VideoPlayerWidget.h"

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    StringView filename = ""sv;
    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(filename, "The video file to display.", "filename", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    Config::pledge_domain("VideoPlayer");

    auto app = TRY(GUI::Application::create(arguments));
    app->set_config_domain("VideoPlayer"_string);

    auto window = GUI::Window::construct();
    window->resize(640, 480);
    window->set_resizable(true);

    TRY(Core::System::unveil("/tmp/session/%sid/portal/filesystemaccess", "rw"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto main_widget = TRY(VideoPlayer::VideoPlayerWidget::try_create());
    window->set_main_widget(main_widget);
    main_widget->update_title();
    TRY(main_widget->initialize_menubar(window));

    window->show();
    window->set_icon(GUI::Icon::default_icon("app-video-player"sv).bitmap_for_size(16));

    if (!filename.is_empty()) {
        auto response = FileSystemAccessClient::Client::the().request_file_read_only_approved(window, filename);
        if (!response.is_error()) {
            main_widget->open_file(response.release_value());
        }
    }

    return app->exec();
}
