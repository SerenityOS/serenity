/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/OwnPtr.h>
#include <LibChess/Chess.h>
#include <math.h>

class MCTSTree {
public:
    enum EvalMethod {
        Simulation,
        Heuristic,
    };

    MCTSTree(Chess::Board const& board, MCTSTree* parent = nullptr);
    MCTSTree(MCTSTree&&);

    MCTSTree& select_leaf();
    MCTSTree& expand();
    int simulate_game() const;
    int heuristic() const;
    void apply_result(int game_score);
    void do_round();

    Optional<MCTSTree&> child_with_move(Chess::Move);

    MCTSTree& best_node();

    Chess::Move last_move() const;
    double expected_value() const;
    double uct(Chess::Color color) const;
    bool expanded() const;

private:
    // While static parameters are less configurable, they don't take up any
    // memory in the tree, which I believe to be a worthy tradeoff.
    static constexpr double s_exploration_parameter { M_SQRT2 };
    static constexpr int s_number_of_visit_parameter { 1 };
    // FIXME: Optimize simulations enough for use.
    static constexpr EvalMethod s_eval_method { EvalMethod::Heuristic };

    Vector<NonnullOwnPtr<MCTSTree>> m_children;
    MCTSTree* m_parent { nullptr };
    int m_white_points { 0 };
    int m_simulations { 0 };
    OwnPtr<Chess::Board> m_board;
    Optional<Chess::Move> m_last_move;
    Chess::Color m_turn : 2;
    bool m_moves_generated : 1 { false };
};
