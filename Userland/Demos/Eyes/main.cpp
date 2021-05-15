/*
 * Copyright (c) 2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "EyesWidget.h"
#include <LibCore/ArgsParser.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <unistd.h>

int main(int argc, char* argv[])
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
    args_parser.parse(argc, argv);

    if (pledge("stdio recvfd sendfd rpath unix cpath wpath thread", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

    if (pledge("stdio recvfd sendfd rpath cpath wpath thread", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/res", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil(nullptr, nullptr) < 0) {
        perror("unveil");
        return 1;
    }

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

    auto app_icon = GUI::Icon::default_icon("app-eyes");

    auto window = GUI::Window::construct();
    window->set_title("Eyes");
    window->set_icon(app_icon.bitmap_for_size(16));
    window->resize(75 * (full_rows > 0 ? max_in_row : extra_columns), 100 * (full_rows + (extra_columns > 0 ? 1 : 0)));
    window->set_has_alpha_channel(true);

    auto& eyes = window->set_main_widget<EyesWidget>(num_eyes, full_rows, extra_columns);

    auto menubar = GUI::Menubar::construct();
    auto& file_menu = menubar->add_menu("&File");
    file_menu.add_action(GUI::CommonActions::make_quit_action([&](auto&) { app->quit(); }));

    auto& help_menu = menubar->add_menu("&Help");
    help_menu.add_action(GUI::CommonActions::make_about_action("Eyes Demo", app_icon, window));

    window->set_menubar(move(menubar));
    window->show();
    eyes.track_cursor_globally();

    return app->exec();
}
