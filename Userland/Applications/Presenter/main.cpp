/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PresenterWidget.h"
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Window.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    // rpath is required to load .presenter files, unix, sendfd and recvfd are required to talk to WindowServer and WebContent.
    TRY(Core::System::pledge("stdio rpath unix sendfd recvfd"));

    ByteString file_to_load;
    Core::ArgsParser argument_parser;
    argument_parser.add_positional_argument(file_to_load, "Presentation to load", "file", Core::ArgsParser::Required::No);
    argument_parser.parse(arguments);

    auto app = TRY(GUI::Application::create(arguments));

    TRY(Desktop::Launcher::add_allowed_handler_with_only_specific_urls("/bin/Help", { URL::create_with_file_scheme("/usr/share/man/man1/Applications/Presenter.md") }));
    TRY(Desktop::Launcher::seal_allowlist());

    auto window = GUI::Window::construct();
    window->set_title("Presenter");
    window->set_icon(GUI::Icon::default_icon("app-presenter"sv).bitmap_for_size(16));
    window->restore_size_and_position("Presenter"sv, "Window"sv, { { 640, 400 } });
    window->save_size_and_position_on_close("Presenter"sv, "Window"sv);
    auto main_widget = window->set_main_widget<PresenterWidget>();
    TRY(main_widget->initialize_menubar());
    window->show();

    if (!file_to_load.is_empty())
        main_widget->set_file(file_to_load);

    return app->exec();
}
