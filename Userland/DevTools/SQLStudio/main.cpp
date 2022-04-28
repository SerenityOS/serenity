/*
 * Copyright (c) 2022, Dylan Katz <dykatz@uw.edu>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringUtils.h>
#include <LibCore/ArgsParser.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Window.h>
#include <LibMain/Main.h>

#include "MainWidget.h"

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    char const* file_to_open = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(file_to_open, "Path to SQL script or DB", "file", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    auto app = TRY(GUI::Application::try_create(arguments));

    auto app_icon = GUI::Icon::default_icon("app-sql-studio");

    auto window = TRY(GUI::Window::try_create());
    window->resize(640, 480);
    window->set_icon(app_icon.bitmap_for_size(16));
    window->set_title("SQL Studio");

    auto main_widget = TRY(window->try_set_main_widget<MainWidget>());
    main_widget->initialize_menu(window);

    window->on_close_request = [&] {
        if (main_widget->request_close())
            return GUI::Window::CloseRequestDecision::Close;
        return GUI::Window::CloseRequestDecision::StayOpen;
    };

    auto needs_new_script = true;
    if (file_to_open) {
        auto path = LexicalPath(file_to_open);
        if (path.extension().equals_ignoring_case("sql"sv)) {
            main_widget->open_script_from_file(path);
            needs_new_script = false;
        } else if (path.extension().equals_ignoring_case("db"sv)) {
            main_widget->open_database_from_file(path);
        }
    }
    if (needs_new_script)
        main_widget->open_new_script();

    window->show();
    return app->exec();
}
