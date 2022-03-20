/*
 * Copyright (c) 2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "EyesWidget.h"
#include <AK/URL.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/Window.h>
#include <LibMain/Main.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    int num_eyes = 2;
    int max_in_row = 13;

    // Alternatively, allow the user to ask for a grid.
    int grid_rows = -1;
    int grid_columns = -1;

    Core::ArgsParser args_parser;
    args_parser.add_option(num_eyes, "Number of eyes", "num-eyes", 'n', "number");
    args_parser.add_option(max_in_row, "Maximum number of eyes in a row", "max-in-row", 'm', "number");
    args_parser.add_option(grid_rows, "Number of rows in grid (incompatible with --number)", "grid-rows", 'r', "number");
    args_parser.add_option(grid_columns, "Number of columns in grid (incompatible with --number)", "grid-cols", 'c', "number");
    args_parser.parse(arguments);

    TRY(Core::System::pledge("stdio recvfd sendfd rpath unix cpath wpath thread"));

    auto app = TRY(GUI::Application::try_create(arguments));

    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil("/tmp/portal/launch", "rw"));
    TRY(Core::System::unveil(nullptr, nullptr));

    if ((grid_rows > 0) ^ (grid_columns > 0)) {
        warnln("Expected either both or none of 'grid-rows' and 'grid-cols' to be passed.");
        return 1;
    }

    int full_rows, extra_columns;

    if (grid_rows > 0) {
        full_rows = grid_rows;
        extra_columns = 0;
        num_eyes = grid_rows * grid_columns;
        max_in_row = grid_columns;
    } else {
        full_rows = num_eyes / max_in_row;
        extra_columns = num_eyes % max_in_row;
    }

    auto app_icon = TRY(GUI::Icon::try_create_default_icon("app-eyes"));

    auto window = TRY(GUI::Window::try_create());
    window->set_title("Eyes");
    window->set_icon(app_icon.bitmap_for_size(16));
    window->resize(75 * (full_rows > 0 ? max_in_row : extra_columns), 100 * (full_rows + (extra_columns > 0 ? 1 : 0)));
    window->set_has_alpha_channel(true);

    (void)TRY(window->try_set_main_widget<EyesWidget>(num_eyes, full_rows, extra_columns));

    auto file_menu = TRY(window->try_add_menu("&File"));
    TRY(file_menu->try_add_action(GUI::CommonActions::make_quit_action([&](auto&) { app->quit(); })));

    auto help_menu = TRY(window->try_add_menu("&Help"));
    TRY(help_menu->try_add_action(GUI::CommonActions::make_help_action([](auto&) {
        Desktop::Launcher::open(URL::create_with_file_protocol("/usr/share/man/man1/Eyes.md"), "/bin/Help");
    })));
    TRY(help_menu->try_add_action(GUI::CommonActions::make_about_action("Eyes Demo", app_icon, window)));

    window->show();

    return app->exec();
}
