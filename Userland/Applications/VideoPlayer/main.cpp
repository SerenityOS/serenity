/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
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

    auto app = TRY(GUI::Application::try_create(arguments));
    auto window = TRY(GUI::Window::try_create());
    window->resize(640, 480);
    window->set_resizable(true);

    auto main_widget = TRY(window->set_main_widget<VideoPlayer::VideoPlayerWidget>(window));
    main_widget->update_title();
    main_widget->initialize_menubar(window);

    if (!filename.is_empty())
        main_widget->open_file(filename);

    window->show();
    window->set_icon(GUI::Icon::default_icon("app-video-player"sv).bitmap_for_size(16));

    return app->exec();
}
