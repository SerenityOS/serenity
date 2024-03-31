/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Game.h"
#include <AK/Array.h>
#include <AK/NumericLimits.h>
#include <AK/ScopeGuard.h>
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

    m_board.m_tiles.resize(grid_size);
    for (auto& row : m_board.m_tiles) {
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
    } while (m_board.m_tiles[row][column] != 0);

    size_t value = rand() < RAND_MAX * 0.9 ? 2 : 4;
    m_board.add_tile(row, column, value);
}

void Game::Board::transpose()
{
    for (size_t i = 1; i < m_tiles.size(); ++i) {
        for (size_t j = 0; j < i; j++)
            swap(m_tiles[i][j], m_tiles[j][i]);
    }
    for (auto& t : m_sliding_tiles) {
        swap(t.row_from, t.column_from);
        swap(t.row_to, t.column_to);
    }
}

void Game::Board::reverse()
{
    for (auto& row : m_tiles) {
        for (size_t i = 0; i < row.size() / 2; ++i)
            swap(row[i], row[row.size() - i - 1]);
    }

    auto const row_size = m_tiles[0].size();
    for (auto& t : m_sliding_tiles) {
        t.column_from = row_size - t.column_from - 1;
        t.column_to = row_size - t.column_to - 1;
    }
}

size_t Game::Board::slide_row(size_t row_index)
{
    Game::Board::Row& row = m_tiles[row_index];
    size_t successful_merge_score = 0;

    auto next_nonempty = [&](size_t start_index) {
        size_t next = start_index;
        for (; next < row.size(); next++) {
            if (row[next] != 0)
                break;
        }
        return next;
    };

    size_t current_index = 0;

    size_t first = next_nonempty(0);
    if (first == row.size()) // empty row
        return 0;

    while (first < row.size()) {
        auto second = next_nonempty(first + 1);
        if (second == row.size() || row[first] != row[second]) {
            m_sliding_tiles.append({ row_index, first, row[first], row_index, current_index, row[first] });

            row[current_index] = row[first];
            current_index++;
            first = second;
        } else {
            VERIFY(row[first] == row[second]);

            m_sliding_tiles.append({ row_index, first, row[first], row_index, current_index, 2 * row[first] });
            m_sliding_tiles.append({ row_index, second, row[second], row_index, current_index, 2 * row[first] });

            row[current_index] = 2 * row[first];
            current_index++;
            successful_merge_score += 2 * row[first];
            first = next_nonempty(second + 1);
        }
    }

    for (; current_index < row.size(); current_index++)
        row[current_index] = 0;

    return successful_merge_score;
}

size_t Game::Board::slide_left()
{
    m_sliding_tiles.clear();

    size_t successful_merge_score = 0;

    for (size_t row_index = 0; row_index < m_tiles.size(); row_index++)
        successful_merge_score += slide_row(row_index);

    return successful_merge_score;
}

static bool is_complete(Game::Board const& board, size_t target)
{
    for (auto& row : board.tiles()) {
        if (row.contains_slow(target))
            return true;
    }

    return false;
}

static bool has_no_neighbors(ReadonlySpan<u32> const& row)
{
    if (row.size() < 2)
        return true;

    auto x = row[0];
    auto y = row[1];

    if (x == y)
        return false;

    return has_no_neighbors(row.slice(1, row.size() - 1));
}

bool Game::Board::is_stalled()
{
    static auto stalled = [](auto& row) {
        return !row.contains_slow(0) && has_no_neighbors(row.span());
    };

    for (auto& row : m_tiles)
        if (!stalled(row))
            return false;

    transpose();
    auto scope_guard = ScopeGuard([&]() { transpose(); });
    for (auto& row : m_tiles)
        if (!stalled(row))
            return false;

    return true;
}

static size_t get_number_of_free_cells(Game::Board const& board)
{
    size_t accumulator = 0;
    for (auto& row : board.tiles()) {
        for (auto& cell : row)
            accumulator += cell == 0;
    }
    return accumulator;
}

Game::Board::SlideResult Game::Board::slide_tiles(Direction direction)
{
    size_t successful_merge_score = 0;

    switch (direction) {
    case Direction::Left:
        successful_merge_score = slide_left();
        break;

    case Direction::Right:
        reverse();
        successful_merge_score = slide_left();
        reverse();
        break;

    case Direction::Up:
        transpose();
        successful_merge_score = slide_left();
        transpose();
        break;

    case Direction::Down:
        transpose();
        reverse();
        successful_merge_score = slide_left();
        reverse();
        transpose();
        break;
    }

    bool moved = false;
    for (auto& t : m_sliding_tiles) {
        if (t.row_from != t.row_to || t.column_from != t.column_to)
            moved = true;
    }

    return { moved, successful_merge_score };
}

Game::MoveOutcome Game::attempt_move(Direction direction)
{
    auto [moved, successful_merge_score] = m_board.slide_tiles(direction);
    if (moved) {
        m_turns++;
        m_score += successful_merge_score;
        add_tile();
    }

    if (is_complete(m_board, m_target_tile) && !m_want_to_continue)
        return MoveOutcome::Won;
    if (m_board.is_stalled())
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
            if (m_board.m_tiles[row][column] != 0)
                continue;

            for (u32 value : Array { 2, 4 }) {
                Game saved_state = *this;
                saved_state.m_board.m_tiles[row][column] = value;

                if (saved_state.m_board.is_stalled()) {
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
                    auto [moved, score_delta] = moved_state.m_board.slide_tiles(direction);
                    if (!moved) // invalid move
                        continue;
                    best_outcome = max(best_outcome, get_number_of_free_cells(moved_state.board()));
                    best_score = max(best_score, score_delta);
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
    m_board.add_tile(worst_row, worst_column, worst_value);
}

u32 Game::largest_tile() const
{
    u32 tile = 0;
    for (auto& row : m_board.m_tiles) {
        for (auto& cell : row)
            tile = max(tile, cell);
    }
    return tile;
}
