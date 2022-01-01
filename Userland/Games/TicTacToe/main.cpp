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

TicTacToe::Board* initialize_and_get_board(GUI::Widget* widget);
void initialize_game(TicTacToe::Board* board);
ErrorOr<NonnullRefPtr<GUI::Window>> try_create_window();
ErrorOr<void> try_create_menu(GUI::Application*, GUI::Window*);

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath recvfd sendfd unix"));

    auto app = TRY(GUI::Application::try_create(arguments));

    auto window = TRY(try_create_window());
    auto widget = TRY(window->try_set_main_widget<GUI::Widget>());
    widget->load_from_gml(tictactoe_gml);
    auto board = initialize_and_get_board(widget);
    initialize_game(board);

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

ErrorOr<void> try_create_menu(GUI::Application* app, GUI::Window* window) {
    auto game_menu = TRY(window->try_add_menu("&Game"));
    TRY(game_menu->try_add_action(GUI::Action::create("&New Game", { Mod_None, Key_F2 }, [&](auto&) {
        TicTacToe::Game::the().start_new_game();
    })));
    TRY(game_menu->try_add_separator());
    TRY(game_menu->try_add_action(GUI::CommonActions::make_quit_action([&](auto&) { app->quit(); })));
    return {};
}

TicTacToe::Board* initialize_and_get_board(GUI::Widget* widget) {
    auto board = widget->find_descendant_of_type_named<TicTacToe::Board>("board");
    TicTacToe::Game::Player player = TicTacToe::Game::Player::X;
    for(uint8_t cell_index = 0; cell_index < 9; cell_index++) {
        board->make_move(cell_index, player);
        player = player == TicTacToe::Game::Player::X
            ? TicTacToe::Game::Player::O
            : TicTacToe::Game::Player::X;
    }
    return board;
}

void initialize_game(TicTacToe::Board* board) {
    TicTacToe::Game::the().on_move = [board](int const cell_index, TicTacToe::Game::Player const player) {
        board->make_move(cell_index, player);
    };
    TicTacToe::Game::the().on_new_game = [board]() {
        board->clear();
    };
    TicTacToe::Game::the().on_win = [board](uint8_t* const winner_cells, TicTacToe::Game::Player const player) {
        for(uint8_t i = 0; i < 3; i++) {
            board->highlight_cell(winner_cells[i]);
        }
        dbgln("Winner: {}", player == TicTacToe::Game::Player::X ? 'X' : 'O');
    };
}
