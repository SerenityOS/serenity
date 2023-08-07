/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ChessEngine.h"
#include "MCTSTree.h"
#include <AK/Random.h>
#include <LibCore/ElapsedTimer.h>

using namespace Chess::UCI;

void ChessEngine::handle_uci()
{
    send_command(IdCommand(IdCommand::Type::Name, "ChessEngine"_string));
    send_command(IdCommand(IdCommand::Type::Author, "the SerenityOS developers"_string));
    send_command(UCIOkCommand());
}

void ChessEngine::handle_position(PositionCommand const& command)
{
    // FIXME: Implement fen board position.
    VERIFY(!command.fen().has_value());
    m_board = Chess::Board();
    for (auto& move : command.moves()) {
        VERIFY(m_board.apply_move(move));
    }
}

void ChessEngine::handle_go(GoCommand const& command)
{
    // FIXME: A better algorithm than naive mcts.
    // FIXME: Add different ways to terminate search.
    VERIFY(command.movetime.has_value());

    srand(get_random<u32>());

    auto elapsed_time = Core::ElapsedTimer::start_new();

    auto mcts = [this]() -> MCTSTree {
        if (!m_last_tree.has_value())
            return { m_board };
        auto x = m_last_tree.value().child_with_move(m_board.last_move().value());
        if (x.has_value())
            return move(x.value());
        return { m_board };
    }();

    int rounds = 0;
    while (elapsed_time.elapsed() <= command.movetime.value()) {
        mcts.do_round();
        ++rounds;
    }
    dbgln("MCTS finished {} rounds.", rounds);
    dbgln("MCTS evaluation {}", mcts.expected_value());
    auto& best_node = mcts.best_node();
    auto const& best_move = best_node.last_move();
    dbgln("MCTS best move {}", best_move.to_long_algebraic());
    send_command(BestMoveCommand(best_move));

    m_last_tree = move(best_node);
}

void ChessEngine::handle_quit()
{
    if (on_quit)
        on_quit(ESUCCESS);
}

void ChessEngine::handle_unexpected_eof()
{
    if (on_quit)
        on_quit(EPIPE);
}

void ChessEngine::handle_ucinewgame()
{
    m_board = Chess::Board();
    m_last_tree = {};
}
