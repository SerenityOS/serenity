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
    send_command(IdCommand(IdCommand::Type::Name, "ChessEngine"));
    send_command(IdCommand(IdCommand::Type::Author, "the SerenityOS developers"));
    send_command(UCIOkCommand());
}

void ChessEngine::handle_position(const PositionCommand& command)
{
    // FIXME: Implement fen board position.
    VERIFY(!command.fen().has_value());
    m_board = Chess::Board();
    for (auto& move : command.moves()) {
        VERIFY(m_board.apply_move(move));
    }
}

void ChessEngine::handle_go(const GoCommand& command)
{
    // FIXME: A better algorithm than naive mcts.
    // FIXME: Add different ways to terminate search.
    VERIFY(command.movetime.has_value());

    srand(get_random<u32>());

    auto elapsed_time = Core::ElapsedTimer::start_new();

    MCTSTree mcts(m_board);

    int rounds = 0;
    while (elapsed_time.elapsed() <= command.movetime.value()) {
        mcts.do_round();
        ++rounds;
    }
    dbgln("MCTS finished {} rounds.", rounds);
    dbgln("MCTS evaluation {}", mcts.expected_value());
    auto best_move = mcts.best_move();
    dbgln("MCTS best move {}", best_move.to_long_algebraic());
    send_command(BestMoveCommand(best_move));
}
