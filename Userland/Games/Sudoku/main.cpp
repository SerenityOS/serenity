/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Board.h"
#include "SudokuWidget.h"
#include <LibConfig/Client.h>
#include <LibCore/System.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Application.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Window.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge(
        "stdio rpath wpath cpath recvfd sendfd thread proc exec unix"));

    auto app = TRY(GUI::Application::try_create(arguments));

    Config::pledge_domain("Sudoku");

    TRY(Desktop::Launcher::add_allowed_handler_with_only_specific_urls(
        "/bin/Help",
        { URL::create_with_file_protocol("/usr/share/man/man6/Sudoku.md") }));
    TRY(Desktop::Launcher::seal_allowlist());

    TRY(Core::System::pledge(
        "stdio rpath wpath cpath recvfd sendfd thread proc exec"));

    auto app_icon = TRY(GUI::Icon::try_create_default_icon("app-sudoku"));

    auto window = TRY(GUI::Window::try_create());
    auto widget = TRY(window->try_set_main_widget<SudokuWidget>());

    Board board = Board();
    widget->set_board(&board);

    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil("/etc/passwd", "r"));
    TRY(Core::System::unveil("/tmp/portal/launch", "rw"));
    TRY(Core::System::unveil(Core::StandardPaths::home_directory().characters(),
        "wcbr"));
    TRY(Core::System::unveil(nullptr, nullptr));

    window->set_title("Sudoku");
    window->set_base_size({ 4, 4 });
    window->set_size_increment({ 8, 8 });
    window->resize(444, 444);

    window->set_icon(app_icon.bitmap_for_size(16));

    auto game_menu = TRY(window->try_add_menu("&Game"));

    auto help_menu = TRY(window->try_add_menu("&Help"));
    TRY(help_menu->try_add_action(
        GUI::CommonActions::make_help_action([](auto&) {
            Desktop::Launcher::open(
                URL::create_with_file_protocol("/usr/share/man/man6/Sudoku.md"),
                "/bin/Help");
        })));
    TRY(help_menu->try_add_action(
        GUI::CommonActions::make_about_action("Sudoku", app_icon, window)));

    TRY(game_menu->try_add_action(GUI::Action::create(
        "&New Game", { Mod_None, Key_F2 },
        TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/reload.png")),
        [&](auto&) { widget->new_game(); })));
    TRY(game_menu->try_add_action(GUI::CommonActions::make_quit_action(
        [](auto&) { GUI::Application::the()->quit(); })));

    widget->on_win = [&] {
        auto play_again = GUI::MessageBox::show(
            window, String::formatted("Well Done. Would you like to play again?"),
            "Congratulations!", GUI::MessageBox::Type::Question,
            GUI::MessageBox::InputType::YesNo);
        if (play_again == GUI::MessageBox::ExecYes)
            widget->new_game();
    };

    window->show();

    return app->exec();
}
