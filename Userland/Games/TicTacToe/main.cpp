/*
 * Copyright (c) 2021, Leonardo Nicolas <leonicolas@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Game.h"
#include "Board.h"
#include <Games/TicTacToe/TicTacToeGML.h>
#include <LibConfig/Client.h>
#include <LibCore/System.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/SeparatorWidget.h>
#include <LibGUI/Window.h>
#include <LibMain/Main.h>
#include <stdio.h>

ErrorOr<NonnullRefPtr<GUI::Window>> try_create_window();
ErrorOr<void> try_create_menu(GUI::Application&, GUI::Window&);

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath recvfd sendfd unix"));

    auto app = TRY(GUI::Application::try_create(arguments));

    auto window = TRY(try_create_window());
    auto widget = TRY(window->try_set_main_widget<GUI::Widget>());
    widget->load_from_gml(tictactoe_gml);

    auto board = widget->find_descendant_of_type_named<TicTacToe::Board>("board");
    TicTacToe::Game::the().on_move = [board](int cell_index, TicTacToe::Game::Player player) {
        board->make_move(cell_index, player);
    };;

    //Config::pledge_domains("tictactoe");

    TRY(Core::System::pledge("stdio rpath recvfd sendfd"));

    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    TRY(try_create_menu(app, window));

    window->show();
    return app->exec();
}

ErrorOr<NonnullRefPtr<GUI::Window>> try_create_window() {
    auto window = TRY(GUI::Window::try_create());
    window->set_double_buffering_enabled(false);
    window->set_title("Tic Tac Toe");
    window->set_resizable(false);
    window->resize(TicTacToe::Game::width, TicTacToe::Game::height);

    auto app_icon = GUI::Icon::default_icon("app-tictactoe");
    window->set_icon(app_icon.bitmap_for_size(16));
    return window;
}

ErrorOr<void> try_create_menu(GUI::Application& app, GUI::Window& window) {
    auto game_menu = TRY(window.try_add_menu("&Game"));
    TRY(game_menu->try_add_action(GUI::Action::create("&New Game", { Mod_None, Key_F2 }, [&](auto&) {
    })));
    TRY(game_menu->try_add_separator());
    TRY(game_menu->try_add_action(GUI::CommonActions::make_quit_action([&](auto&) { app.quit(); })));
    return {};
}
