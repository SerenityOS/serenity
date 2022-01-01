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
#include <LibGUI/Statusbar.h>
#include <LibGUI/Window.h>
#include <LibMain/Main.h>
#include <stdio.h>

#define INITIAL_MESSAGE     "Press F2 to start the game"
#define X_VICTORY_MESSAGE   "X: {} victory"
#define O_VICTORY_MESSAGE   "O: {} victory"
#define X_VICTORIES_MESSAGE "X: {} victories"
#define O_VICTORIES_MESSAGE "O: {} victories"
#define TURN_MESSAGE        "It's {} turn"
#define WINNER_MESSAGE      "Player {} won!"
#define TIE_MESSAGE         "Tie!"

TicTacToe::Board* initialize_and_get_board(GUI::Widget* widget);
void initialize_game(TicTacToe::Board* board, GUI::Statusbar* statusbar);
ErrorOr<NonnullRefPtr<GUI::Window>> try_create_window();
ErrorOr<void> try_create_menu(GUI::Application*, GUI::Window*);

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath recvfd sendfd unix"));

    auto app = TRY(GUI::Application::try_create(arguments));

    auto window = TRY(try_create_window());

    auto widget = TRY(window->try_set_main_widget<GUI::Widget>());
    widget->load_from_gml(tictactoe_gml);

    auto statusbar = widget->find_descendant_of_type_named<GUI::Statusbar>("statusbar");
    VERIFY(statusbar);
    statusbar->set_text(0, INITIAL_MESSAGE);
    statusbar->set_text(1, String::formatted(X_VICTORIES_MESSAGE, 0));
    statusbar->set_width(1, TicTacToe::Game::width * 0.27f);
    statusbar->set_text(2, String::formatted(O_VICTORIES_MESSAGE, 0));
    statusbar->set_width(2, TicTacToe::Game::width * 0.27f);

    window->resize(TicTacToe::Game::width, TicTacToe::Game::height + statusbar->max_height());

    auto board = initialize_and_get_board(widget);
    initialize_game(board, statusbar);

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
    VERIFY(board);
    TicTacToe::Game::Player player = TicTacToe::Game::Player::X;
    for(uint8_t cell_index = 0; cell_index < 9; cell_index++) {
        board->make_move(cell_index, player);
        player = player == TicTacToe::Game::Player::X
            ? TicTacToe::Game::Player::O
            : TicTacToe::Game::Player::X;
    }
    return board;
}

void initialize_game(TicTacToe::Board* board, GUI::Statusbar* statusbar) {
    auto game = &TicTacToe::Game::the();
    game->on_move = [board, statusbar](int const cell_index, TicTacToe::Game::Player const player) {
        board->make_move(cell_index, player);
        char next_player = player == TicTacToe::Game::Player::X ? 'O' : 'X';
        statusbar->set_text(0, String::formatted(TURN_MESSAGE, next_player));
    };

    game->on_new_game = [game, board, statusbar]() {
        board->clear();
        char current_player = game->get_current_player() == TicTacToe::Game::Player::X ? 'X' : 'O';
        statusbar->set_text(0, String::formatted(TURN_MESSAGE, current_player));
    };

    game->on_win = [board, statusbar](uint8_t* const winner_cells, TicTacToe::Game::Player const player, uint16_t num_victories) {
        for(uint8_t i = 0; i < 3; i++) {
            board->highlight_cell(winner_cells[i]);
        }
        statusbar->set_text(0, String::formatted(WINNER_MESSAGE, player == TicTacToe::Game::Player::X ? 'X' : 'O'));

        if(player == TicTacToe::Game::Player::X) {
            statusbar->set_text(1, String::formatted(num_victories == 1 ? X_VICTORY_MESSAGE : X_VICTORIES_MESSAGE, num_victories));
        } else {
            statusbar->set_text(2, String::formatted(num_victories == 1 ? O_VICTORY_MESSAGE : O_VICTORIES_MESSAGE, num_victories));
        }
    };

    game->on_tie = [board, statusbar](uint16_t) {
        statusbar->set_text(0, TIE_MESSAGE);
    };
}
