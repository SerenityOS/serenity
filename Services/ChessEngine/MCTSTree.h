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

#pragma once

#include <AK/Function.h>
#include <AK/NonnullOwnPtrVector.h>
#include <LibChess/Chess.h>
#include <math.h>

class MCTSTree {
public:
    enum EvalMethod {
        Simulation,
        Heuristic,
    };

    MCTSTree(const Chess::Board& board, double exploration_parameter = sqrt(2), MCTSTree* parent = nullptr);

    MCTSTree& select_leaf();
    MCTSTree& expand();
    int simulate_game() const;
    int heuristic() const;
    void apply_result(int game_score);
    void do_round();

    Chess::Move best_move() const;
    double expected_value() const;
    double uct(Chess::Colour colour) const;
    bool expanded() const;

    EvalMethod eval_method() const { return m_eval_method; }
    void set_eval_method(EvalMethod method) { m_eval_method = method; }

private:
    NonnullOwnPtrVector<MCTSTree> m_children;
    MCTSTree* m_parent { nullptr };
    int m_white_points { 0 };
    int m_simulations { 0 };
    bool m_moves_generated { false };
    double m_exploration_parameter;
    EvalMethod m_eval_method { EvalMethod::Simulation };
    Chess::Board m_board;
};
