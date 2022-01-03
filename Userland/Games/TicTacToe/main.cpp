/*
 * Copyright (c) 2021, Leonardo Nicolas <leonicolas@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Board.h"
#include "Game.h"
#include <Games/TicTacToe/TicTacToeGML.h>
#include <LibConfig/Client.h>
#include <LibCore/System.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/SeparatorWidget.h>
#include <LibGUI/Statusbar.h>
#include <LibGUI/Window.h>
#include <LibMain/Main.h>
#include <stdio.h>

#define INITIAL_MESSAGE "Press F2 to start the game"
#define X_VICTORY_MESSAGE "X: {} victory"
#define O_VICTORY_MESSAGE "O: {} victory"
#define X_VICTORIES_MESSAGE "X: {} victories"
#define O_VICTORIES_MESSAGE "O: {} victories"
#define TURN_MESSAGE "It's {} turn"
#define WINNER_MESSAGE "Player {} won!"
#define TIE_MESSAGE "Tie!"

GUI::Statusbar* initialize_and_get_statusbar(GUI::Widget&);
void initialize_game(TicTacToe::Board&, GUI::Statusbar&);
ErrorOr<NonnullRefPtr<GUI::Window>> try_create_window();
ErrorOr<void> try_create_app_menu(GUI::Application&, GUI::Window&, TicTacToe::Board&, GUI::Statusbar&);

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath recvfd sendfd unix"));

    auto app = TRY(GUI::Application::try_create(arguments));
    auto window = TRY(try_create_window());

    TRY(Core::System::pledge("stdio rpath recvfd sendfd"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto widget = TRY(window->try_set_main_widget<GUI::Widget>());
    widget->load_from_gml(tictactoe_gml);

    auto statusbar = initialize_and_get_statusbar(widget);
    auto board = widget->find_descendant_of_type_named<TicTacToe::Board>("board");
    VERIFY(board);
    board->draw_presentation_pattern();

    initialize_game(*board, *statusbar);

    TRY(try_create_app_menu(*app, *window, *board, *statusbar));

    window->resize(TicTacToe::Game::width, TicTacToe::Game::height + statusbar->max_height());
    window->show();

    return app->exec();
}

ErrorOr<void> try_create_app_menu(GUI::Application& app, GUI::Window& window, TicTacToe::Board& board, GUI::Statusbar& statusbar)
{
    auto game = &TicTacToe::Game::the();

    // Game mode actions
    auto game_modes = new GUI::ActionGroup();
    game_modes = move(game_modes);
    game_modes->set_exclusive(true);

    auto human_vs_machine_action = GUI::Action::create_checkable("Human vs &Machine", [game, &board, &statusbar](auto&) {
        game->set_mode(TicTacToe::Game::Mode::HumanVsMachine);
        board.draw_presentation_pattern();
        statusbar.set_text(0, INITIAL_MESSAGE);
    });
    human_vs_machine_action->set_checked(game->mode() == TicTacToe::Game::Mode::HumanVsMachine);
    game_modes->add_action(human_vs_machine_action);

    auto human_vs_human_action = GUI::Action::create_checkable("&Human vs Human", [game, &board, &statusbar](auto&) {
        game->set_mode(TicTacToe::Game::Mode::HumanVsHuman);
        board.draw_presentation_pattern();
        statusbar.set_text(0, INITIAL_MESSAGE);
    });
    human_vs_human_action->set_checked(game->mode() == TicTacToe::Game::Mode::HumanVsHuman);
    game_modes->add_action(human_vs_human_action);

    // Game menu
    auto game_menu = TRY(window.try_add_menu("&Game"));
    TRY(game_menu->try_add_action(GUI::Action::create("&New Game", { Mod_None, Key_F2 }, [game](auto&) {
        game->start_new_game();
    })));
    TRY(game_menu->try_add_separator());
    TRY(game_menu->try_add_action(human_vs_machine_action));
    TRY(game_menu->try_add_action(human_vs_human_action));
    TRY(game_menu->try_add_separator());
    TRY(game_menu->try_add_action(GUI::CommonActions::make_quit_action([&app](auto&) { app.quit(); })));

    return {};
}

ErrorOr<NonnullRefPtr<GUI::Window>> try_create_window()
{
    auto window = TRY(GUI::Window::try_create());
    window->set_double_buffering_enabled(false);
    window->set_title("Tic Tac Toe");
    window->set_resizable(false);

    auto app_icon = GUI::Icon::default_icon("app-tictactoe");
    window->set_icon(app_icon.bitmap_for_size(16));
    return window;
}

void initialize_game(TicTacToe::Board& board, GUI::Statusbar& statusbar)
{
    auto game = &TicTacToe::Game::the();
    game->on_move = [game, &board, &statusbar](auto cell_index, auto current_player, auto next_player) {
        board.do_move(cell_index, current_player);
        if (game->moves_remaining())
            statusbar.set_text(0, String::formatted(TURN_MESSAGE, next_player == TicTacToe::Game::Player::X ? 'X' : 'O'));
    };

    game->on_new_game = [game, &board, &statusbar]() {
        board.clear();
        char current_player = game->current_player() == TicTacToe::Game::Player::X ? 'X' : 'O';
        statusbar.set_text(0, String::formatted(TURN_MESSAGE, current_player));
    };

    game->on_win = [&board, &statusbar](auto winner_cells, auto player, auto num_victories) {
        for (uint8_t i = 0; i < 3; i++)
            board.highlight_cell(winner_cells[i]);

        statusbar.set_text(0, String::formatted(WINNER_MESSAGE, player == TicTacToe::Game::Player::X ? 'X' : 'O'));

        if (player == TicTacToe::Game::Player::X)
            statusbar.set_text(1, String::formatted(num_victories == 1 ? X_VICTORY_MESSAGE : X_VICTORIES_MESSAGE, num_victories));
        else
            statusbar.set_text(2, String::formatted(num_victories == 1 ? O_VICTORY_MESSAGE : O_VICTORIES_MESSAGE, num_victories));
    };

    game->on_tie = [&board, &statusbar](auto) {
        statusbar.set_text(0, TIE_MESSAGE);
    };
}

GUI::Statusbar* initialize_and_get_statusbar(GUI::Widget& widget)
{
    auto statusbar = widget.find_descendant_of_type_named<GUI::Statusbar>("statusbar");
    VERIFY(statusbar);
    statusbar->set_text(0, INITIAL_MESSAGE);
    statusbar->set_text(1, String::formatted(X_VICTORIES_MESSAGE, 0));
    statusbar->set_width(1, TicTacToe::Game::width * 0.27f);
    statusbar->set_text(2, String::formatted(O_VICTORIES_MESSAGE, 0));
    statusbar->set_width(2, TicTacToe::Game::width * 0.27f);
    return statusbar;
}
