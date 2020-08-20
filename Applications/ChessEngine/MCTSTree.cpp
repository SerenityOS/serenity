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

#include "MCTSTree.h"
#include <AK/String.h>
#include <stdlib.h>

MCTSTree::MCTSTree(const Chess::Board& board, double exploration_parameter, MCTSTree* parent)
    : m_parent(parent)
    , m_exploration_parameter(exploration_parameter)
    , m_board(board)
{
    if (m_parent)
        m_eval_method = m_parent->eval_method();
}

MCTSTree& MCTSTree::select_leaf()
{
    if (!expanded() || m_children.size() == 0)
        return *this;

    MCTSTree* node = nullptr;
    double max_uct = -double(INFINITY);
    for (auto& child : m_children) {
        double uct = child.uct(m_board.turn());
        if (uct >= max_uct) {
            max_uct = uct;
            node = &child;
        }
    }
    ASSERT(node);
    return node->select_leaf();
}

MCTSTree& MCTSTree::expand()
{
    ASSERT(!expanded() || m_children.size() == 0);

    if (!m_moves_generated) {
        m_board.generate_moves([&](Chess::Move move) {
            Chess::Board clone = m_board;
            clone.apply_move(move);
            m_children.append(make<MCTSTree>(clone, m_exploration_parameter, this));
            return IterationDecision::Continue;
        });
        m_moves_generated = true;
    }

    if (m_children.size() == 0) {
        return *this;
    }

    for (auto& child : m_children) {
        if (child.m_simulations == 0) {
            return child;
        }
    }
    ASSERT_NOT_REACHED();
}

int MCTSTree::simulate_game() const
{
    ASSERT_NOT_REACHED();
    Chess::Board clone = m_board;
    while (!clone.game_finished()) {
        clone.apply_move(clone.random_move());
    }
    return clone.game_score();
}

int MCTSTree::heuristic() const
{
    if (m_board.game_finished())
        return m_board.game_score();

    double winchance = max(min(double(m_board.material_imbalance()) / 6, 1.0), -1.0);

    double random = double(rand()) / RAND_MAX;
    if (winchance >= random)
        return 1;
    if (winchance <= -random)
        return -1;

    return 0;
}

void MCTSTree::apply_result(int game_score)
{
    m_simulations++;
    m_white_points += game_score;

    if (m_parent)
        m_parent->apply_result(game_score);
}

void MCTSTree::do_round()
{
    auto& node = select_leaf().expand();

    int result;
    if (m_eval_method == EvalMethod::Simulation) {
        result = node.simulate_game();
    } else {
        result = node.heuristic();
    }
    node.apply_result(result);
}

Chess::Move MCTSTree::best_move() const
{
    int score_multiplier = (m_board.turn() == Chess::Colour::White) ? 1 : -1;

    Chess::Move best_move = { { 0, 0 }, { 0, 0 } };
    double best_score = -double(INFINITY);
    ASSERT(m_children.size());
    for (auto& node : m_children) {
        double node_score = node.expected_value() * score_multiplier;
        if (node_score >= best_score) {
            // The best move is the last move made in the child.
            best_move = node.m_board.moves()[node.m_board.moves().size() - 1];
            best_score = node_score;
        }
    }

    return best_move;
}

double MCTSTree::expected_value() const
{
    if (m_simulations == 0)
        return 0;

    return double(m_white_points) / m_simulations;
}

double MCTSTree::uct(Chess::Colour colour) const
{
    // UCT: Upper Confidence Bound Applied to Trees.
    //      Kocsis, Levente; Szepesvári, Csaba (2006). "Bandit based Monte-Carlo Planning"

    // Fun fact: Szepesvári was my data structures professor.
    double expected = expected_value() * ((colour == Chess::Colour::White) ? 1 : -1);
    return expected + m_exploration_parameter * sqrt(log(m_parent->m_simulations) / m_simulations);
}

bool MCTSTree::expanded() const
{
    if (!m_moves_generated)
        return false;

    for (auto& child : m_children) {
        if (child.m_simulations == 0)
            return false;
    }

    return true;
}
