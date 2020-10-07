/*
 * Copyright (c) 2020, Sergey Bugaev <bugaevc@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "EyesWidget.h"
#include <LibCore/ArgsParser.h>
#include <LibGUI/AboutDialog.h>
#include <LibGUI/Application.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MenuBar.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>

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

    if (pledge("stdio shared_buffer accept rpath unix cpath wpath fattr thread", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

    if (pledge("stdio shared_buffer accept rpath cpath wpath thread", nullptr) < 0) {
        perror("pledge");
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

    auto window = GUI::Window::construct();
    window->set_title("Eyes");
    window->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-eyes.png"));
    window->resize(75 * (full_rows > 0 ? max_in_row : extra_columns), 100 * (full_rows + (extra_columns > 0 ? 1 : 0)));
    window->set_has_alpha_channel(true);

    auto& eyes = window->set_main_widget<EyesWidget>(num_eyes, full_rows, extra_columns);

    auto menubar = GUI::MenuBar::construct();
    auto& app_menu = menubar->add_menu("Eyes Demo");
    app_menu.add_action(GUI::CommonActions::make_quit_action([&](auto&) { app->quit(); }));

    auto& help_menu = menubar->add_menu("Help");
    help_menu.add_action(GUI::Action::create("About", [&](auto&) {
        GUI::AboutDialog::show("Mouse Demo", Gfx::Bitmap::load_from_file("/res/icons/32x32/app-eyes.png"), window);
    }));

    app->set_menubar(move(menubar));
    window->show();
    eyes.track_cursor_globally();

    return app->exec();
}
