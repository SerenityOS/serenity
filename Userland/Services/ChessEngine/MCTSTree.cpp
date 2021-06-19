/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MCTSTree.h"
#include <AK/String.h>
#include <stdlib.h>

MCTSTree::MCTSTree(const Chess::Board& board, MCTSTree* parent)
    : m_parent(parent)
    , m_board(make<Chess::Board>(board))
    , m_last_move(board.last_move())
    , m_turn(board.turn())
{
}

MCTSTree& MCTSTree::select_leaf()
{
    if (!expanded() || m_children.size() == 0)
        return *this;

    MCTSTree* node = nullptr;
    double max_uct = -double(INFINITY);
    for (auto& child : m_children) {
        double uct = child.uct(m_turn);
        if (uct >= max_uct) {
            max_uct = uct;
            node = &child;
        }
    }
    VERIFY(node);
    return node->select_leaf();
}

MCTSTree& MCTSTree::expand()
{
    VERIFY(!expanded() || m_children.size() == 0);

    if (!m_moves_generated) {
        m_board->generate_moves([&](Chess::Move move) {
            Chess::Board clone = *m_board;
            clone.apply_move(move);
            m_children.append(make<MCTSTree>(clone, this));
            return IterationDecision::Continue;
        });
        m_moves_generated = true;
        if (m_children.size() != 0)
            m_board = nullptr; // Release the board to save memory.
    }

    if (m_children.size() == 0) {
        return *this;
    }

    for (auto& child : m_children) {
        if (child.m_simulations == 0) {
            return child;
        }
    }
    VERIFY_NOT_REACHED();
}

int MCTSTree::simulate_game() const
{
    Chess::Board clone = *m_board;
    while (!clone.game_finished()) {
        clone.apply_move(clone.random_move());
    }
    return clone.game_score();
}

int MCTSTree::heuristic() const
{
    if (m_board->game_finished())
        return m_board->game_score();

    double winchance = max(min(double(m_board->material_imbalance()) / 6, 1.0), -1.0);

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
    if constexpr (s_eval_method == EvalMethod::Simulation) {
        result = node.simulate_game();
    } else {
        result = node.heuristic();
    }
    node.apply_result(result);
}

Chess::Move MCTSTree::best_move() const
{
    int score_multiplier = (m_turn == Chess::Color::White) ? 1 : -1;

    Chess::Move best_move = { { 0, 0 }, { 0, 0 } };
    double best_score = -double(INFINITY);
    VERIFY(m_children.size());
    for (auto& node : m_children) {
        double node_score = node.expected_value() * score_multiplier;
        if (node_score >= best_score) {
            best_move = node.m_last_move.value();
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

double MCTSTree::uct(Chess::Color color) const
{
    // UCT: Upper Confidence Bound Applied to Trees.
    //      Kocsis, Levente; Szepesvári, Csaba (2006). "Bandit based Monte-Carlo Planning"

    // Fun fact: Szepesvári was my data structures professor.
    double expected = expected_value() * ((color == Chess::Color::White) ? 1 : -1);
    return expected + s_exploration_parameter * sqrt(log(m_parent->m_simulations) / m_simulations);
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
