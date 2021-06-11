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

    m_board.tiles.resize(grid_size);
    for (auto& row : m_board.tiles) {
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
    } while (m_board.tiles[row][column] != 0);

    size_t value = rand() < RAND_MAX * 0.9 ? 2 : 4;
    m_board.add_tile(row, column, value);
}

static Game::Board::Tiles transpose(Game::Board::Tiles const& tiles)
{
    Game::Board::Tiles new_tiles;
    auto result_row_count = tiles[0].size();
    auto result_column_count = tiles.size();

    new_tiles.resize(result_row_count);

    for (size_t i = 0; i < tiles.size(); ++i) {
        auto& row = new_tiles[i];
        row.clear_with_capacity();
        row.ensure_capacity(result_column_count);
        for (auto& entry : tiles) {
            row.append(entry[i]);
        }
    }

    return new_tiles;
}

static Game::Board::Tiles reverse(Game::Board::Tiles const& tiles)
{
    auto new_tiles = tiles;
    for (auto& row : new_tiles) {
        for (size_t i = 0; i < row.size() / 2; ++i)
            swap(row[i], row[row.size() - i - 1]);
    }

    return new_tiles;
}

static Game::Board::Row slide_row(Game::Board::Row const& row, size_t& successful_merge_score)
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

static Game::Board::Tiles slide_left(Game::Board::Tiles const& tiles, size_t& successful_merge_score)
{
    Game::Board::Tiles new_tiles;
    for (auto& row : tiles)
        new_tiles.append(slide_row(row, successful_merge_score));

    return new_tiles;
}

static bool is_complete(Game::Board const& board, size_t target)
{
    for (auto& row : board.tiles) {
        if (row.contains_slow(target))
            return true;
    }

    return false;
}

static bool has_no_neighbors(Span<u32 const> const& row)
{
    if (row.size() < 2)
        return true;

    auto x = row[0];
    auto y = row[1];

    if (x == y)
        return false;

    return has_no_neighbors(row.slice(1, row.size() - 1));
};

static bool is_stalled(Game::Board const& board)
{
    static auto stalled = [](auto& row) {
        return !row.contains_slow(0) && has_no_neighbors(row.span());
    };

    for (auto& row : board.tiles)
        if (!stalled(row))
            return false;

    for (auto& row : transpose(board.tiles))
        if (!stalled(row))
            return false;

    return true;
}

static size_t get_number_of_free_cells(Game::Board const& board)
{
    size_t accumulator = 0;
    for (auto& row : board.tiles) {
        for (auto& cell : row)
            accumulator += cell == 0;
    }
    return accumulator;
}

bool Game::slide_tiles(Direction direction)
{
    size_t successful_merge_score = 0;
    Game::Board::Tiles new_tiles;

    switch (direction) {
    case Direction::Left:
        new_tiles = slide_left(m_board.tiles, successful_merge_score);
        break;
    case Direction::Right:
        new_tiles = reverse(slide_left(reverse(m_board.tiles), successful_merge_score));
        break;
    case Direction::Up:
        new_tiles = transpose(slide_left(transpose(m_board.tiles), successful_merge_score));
        break;
    case Direction::Down:
        new_tiles = transpose(reverse(slide_left(reverse(transpose(m_board.tiles)), successful_merge_score)));
        break;
    }

    bool moved = new_tiles != m_board.tiles;
    if (moved) {
        m_board.tiles = new_tiles;
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
            if (m_board.tiles[row][column] != 0)
                continue;

            for (u32 value : Array { 2, 4 }) {
                Game saved_state = *this;
                saved_state.m_board.tiles[row][column] = value;

                if (is_stalled(saved_state.board())) {
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
    m_board.add_tile(worst_row, worst_column, worst_value);
}

u32 Game::largest_tile() const
{
    u32 tile = 0;
    for (auto& row : m_board.tiles) {
        for (auto& cell : row)
            tile = max(tile, cell);
    }
    return tile;
}
