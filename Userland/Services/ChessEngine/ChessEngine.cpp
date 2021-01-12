/*
 * Copyright (c) 2020, the SerenityOS developers.
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

#include "ChessEngine.h"
#include "MCTSTree.h"
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
    ASSERT(!command.fen().has_value());
    m_board = Chess::Board();
    for (auto& move : command.moves()) {
        ASSERT(m_board.apply_move(move));
    }
}

void ChessEngine::handle_go(const GoCommand& command)
{
    // FIXME: A better algorithm than naive mcts.
    // FIXME: Add different ways to terminate search.
    ASSERT(command.movetime.has_value());

    srand(arc4random());

    Core::ElapsedTimer elapsed_time;
    elapsed_time.start();

    MCTSTree mcts(m_board);

    // FIXME: optimize simulations enough for use.
    mcts.set_eval_method(MCTSTree::EvalMethod::Heuristic);

    int rounds = 0;
    while (elapsed_time.elapsed() <= command.movetime.value()) {
        mcts.do_round();
        ++rounds;
    }
    dbg() << "MCTS finished " << rounds << " rounds.";
    dbg() << "MCTS evaluation " << mcts.expected_value();
    auto best_move = mcts.best_move();
    dbg() << "MCTS best move " << best_move.to_long_algebraic();
    send_command(BestMoveCommand(best_move));
}
