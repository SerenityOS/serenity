/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Game.h"
#include <AK/Array.h>
#include <AK/NumericLimits.h>
#include <AK/String.h>
#include <stdlib.h>

Game::Game(size_t grid_size, size_t target_tile, bool evil_ai)
    : m_grid_size(grid_size)
    , m_evil_ai(evil_ai)
{
    if (target_tile == 0)
        m_target_tile = 2048;
    else if ((target_tile & (target_tile - 1)) != 0)
        m_target_tile = 1 << max_power_for_board(grid_size);
    else
        m_target_tile = target_tile;

    m_board.resize(grid_size);
    for (auto& row : m_board) {
        row.ensure_capacity(grid_size);
        for (size_t i = 0; i < grid_size; i++)
            row.append(0);
    }

    add_tile();
    add_tile();
}

void Game::add_random_tile()
{
    int row;
    int column;
    do {
        row = rand() % m_grid_size;
        column = rand() % m_grid_size;
    } while (m_board[row][column] != 0);

    size_t value = rand() < RAND_MAX * 0.9 ? 2 : 4;
    m_board[row][column] = value;
}

static Game::Board transpose(const Game::Board& board)
{
    Vector<Vector<u32>> new_board;
    auto result_row_count = board[0].size();
    auto result_column_count = board.size();

    new_board.resize(result_row_count);

    for (size_t i = 0; i < board.size(); ++i) {
        auto& row = new_board[i];
        row.clear_with_capacity();
        row.ensure_capacity(result_column_count);
        for (auto& entry : board) {
            row.append(entry[i]);
        }
    }

    return new_board;
}

static Game::Board reverse(const Game::Board& board)
{
    auto new_board = board;
    for (auto& row : new_board) {
        for (size_t i = 0; i < row.size() / 2; ++i)
            swap(row[i], row[row.size() - i - 1]);
    }

    return new_board;
}

static Vector<u32> slide_row(const Vector<u32>& row, size_t& successful_merge_score)
{
    if (row.size() < 2)
        return row;

    auto x = row[0];
    auto y = row[1];

    auto result = row;
    result.take_first();

    if (x == 0) {
        result = slide_row(result, successful_merge_score);
        result.append(0);
        return result;
    }

    if (y == 0) {
        result[0] = x;
        result = slide_row(result, successful_merge_score);
        result.append(0);
        return result;
    }

    if (x == y) {
        result.take_first();
        result = slide_row(result, successful_merge_score);
        result.append(0);
        result.prepend(x + x);
        successful_merge_score += x * 2;
        return result;
    }

    result = slide_row(result, successful_merge_score);
    result.prepend(x);
    return result;
}

static Game::Board slide_left(const Game::Board& board, size_t& successful_merge_score)
{
    Vector<Vector<u32>> new_board;
    for (auto& row : board)
        new_board.append(slide_row(row, successful_merge_score));

    return new_board;
}

static bool is_complete(const Game::Board& board, size_t target)
{
    for (auto& row : board) {
        if (row.contains_slow(target))
            return true;
    }

    return false;
}

static bool has_no_neighbors(const Span<const u32>& row)
{
    if (row.size() < 2)
        return true;

    auto x = row[0];
    auto y = row[1];

    if (x == y)
        return false;

    return has_no_neighbors(row.slice(1, row.size() - 1));
};

static bool is_stalled(const Game::Board& board)
{
    static auto stalled = [](auto& row) {
        return !row.contains_slow(0) && has_no_neighbors(row.span());
    };

    for (auto& row : board)
        if (!stalled(row))
            return false;

    for (auto& row : transpose(board))
        if (!stalled(row))
            return false;

    return true;
}

static size_t get_number_of_free_cells(const Game::Board& board)
{
    size_t accumulator = 0;
    for (auto& row : board) {
        for (auto& cell : row)
            accumulator += cell == 0;
    }
    return accumulator;
}

bool Game::slide_tiles(Direction direction)
{
    size_t successful_merge_score = 0;
    Board new_board;

    switch (direction) {
    case Direction::Left:
        new_board = slide_left(m_board, successful_merge_score);
        break;
    case Direction::Right:
        new_board = reverse(slide_left(reverse(m_board), successful_merge_score));
        break;
    case Direction::Up:
        new_board = transpose(slide_left(transpose(m_board), successful_merge_score));
        break;
    case Direction::Down:
        new_board = transpose(reverse(slide_left(reverse(transpose(m_board)), successful_merge_score)));
        break;
    }

    bool moved = new_board != m_board;
    if (moved) {
        m_board = new_board;
        m_score += successful_merge_score;
    }

    return moved;
}

Game::MoveOutcome Game::attempt_move(Direction direction)
{
    bool moved = slide_tiles(direction);
    if (moved) {
        m_turns++;
        add_tile();
    }

    if (is_complete(m_board, m_target_tile))
        return MoveOutcome::Won;
    if (is_stalled(m_board))
        return MoveOutcome::GameOver;
    if (moved)
        return MoveOutcome::OK;
    return MoveOutcome::InvalidMove;
}

void Game::add_evil_tile()
{
    size_t worst_row = 0;
    size_t worst_column = 0;
    u32 worst_value = 2;

    size_t most_free_cells = NumericLimits<size_t>::max();
    size_t worst_score = NumericLimits<size_t>::max();

    for (size_t row = 0; row < m_grid_size; row++) {
        for (size_t column = 0; column < m_grid_size; column++) {
            if (m_board[row][column] != 0)
                continue;

            for (u32 value : Array { 2, 4 }) {
                Game saved_state = *this;
                saved_state.m_board[row][column] = value;

                if (is_stalled(saved_state.m_board)) {
                    // We can stall the board now, instant game over.
                    worst_row = row;
                    worst_column = column;
                    worst_value = value;

                    goto found_worst_tile;
                }

                // These are the best outcome and score the player can achieve in one move.
                // We want this to be as low as possible.
                size_t best_outcome = 0;
                size_t best_score = 0;
                for (auto direction : Array { Direction::Down, Direction::Left, Direction::Right, Direction::Up }) {
                    Game moved_state = saved_state;
                    bool moved = moved_state.slide_tiles(direction);
                    if (!moved) // invalid move
                        continue;
                    best_outcome = max(best_outcome, get_number_of_free_cells(moved_state.board()));
                    best_score = max(best_score, moved_state.score());
                }

                // We already know a worse cell placement; discard.
                if (best_outcome > most_free_cells)
                    continue;

                // This tile is the same as the worst we know in terms of board population,
                // but the player can achieve the same or better score; discard.
                if (best_outcome == most_free_cells && best_score >= worst_score)
                    continue;

                worst_row = row;
                worst_column = column;
                worst_value = value;

                most_free_cells = best_outcome;
                worst_score = best_score;
            }
        }
    }
found_worst_tile:
    m_board[worst_row][worst_column] = worst_value;
}

u32 Game::largest_tile() const
{
    u32 tile = 0;
    for (auto& row : board()) {
        for (auto& cell : row)
            tile = max(tile, cell);
    }
    return tile;
}
