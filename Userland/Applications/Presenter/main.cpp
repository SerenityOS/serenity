/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PresenterWidget.h"
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Window.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    // rpath is required to load .presenter files, unix, sendfd and recvfd are required to talk to WindowServer and WebContent.
    TRY(Core::System::pledge("stdio rpath unix sendfd recvfd"));

    DeprecatedString file_to_load;
    Core::ArgsParser argument_parser;
    argument_parser.add_positional_argument(file_to_load, "Presentation to load", "file", Core::ArgsParser::Required::No);
    argument_parser.parse(arguments);

    auto app = TRY(GUI::Application::create(arguments));
    auto window = GUI::Window::construct();
    window->set_title("Presenter");
    window->set_icon(GUI::Icon::default_icon("app-presenter"sv).bitmap_for_size(16));
    auto main_widget = TRY(window->set_main_widget<PresenterWidget>());
    TRY(main_widget->initialize_menubar());
    window->show();

    if (!file_to_load.is_empty())
        main_widget->set_file(file_to_load);

    return app->exec();
}
