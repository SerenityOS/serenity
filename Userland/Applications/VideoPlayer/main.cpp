/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "LibVideo/Color/CodingIndependentCodePoints.h"
#include "LibVideo/MatroskaDemuxer.h"
#include <LibCore/ArgsParser.h>
#include <LibGUI/Application.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Window.h>
#include <LibMain/Main.h>
#include <LibVideo/PlaybackManager.h>

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

    auto main_widget = TRY(window->try_set_main_widget<VideoPlayer::VideoPlayerWidget>(window));
    main_widget->update_title();

    if (!filename.is_empty())
        main_widget->open_file(filename);

    auto file_menu = TRY(window->try_add_menu("&File"));
    TRY(file_menu->try_add_action(GUI::CommonActions::make_open_action([&](auto&) {
        Optional<String> path = GUI::FilePicker::get_open_filepath(window, "Open video file...");
        if (path.has_value())
            main_widget->open_file(path.value());
    })));

    window->show();
    return app->exec();
}
