/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
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
    double uct(Chess::Color color) const;
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
