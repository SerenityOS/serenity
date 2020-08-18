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

#include "Game.h"
#include <stdlib.h>

Game::Game(size_t rows, size_t columns)
    : m_rows(rows)
    , m_columns(columns)
{
    m_board.resize(rows);
    for (auto& row : m_board) {
        row.ensure_capacity(columns);
        for (size_t i = 0; i < columns; i++)
            row.append(0);
    }

    add_random_tile();
    add_random_tile();
}

void Game::add_random_tile()
{
    int row;
    int column;
    do {
        row = rand() % m_rows;
        column = rand() % m_columns;
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

static bool is_complete(const Game::Board& board)
{
    for (auto& row : board) {
        if (row.contains_slow(2048))
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

Game::MoveOutcome Game::attempt_move(Direction direction)
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
        m_turns++;
        add_random_tile();
        m_score += successful_merge_score;
    }

    if (is_complete(m_board))
        return MoveOutcome::Won;
    if (is_stalled(m_board))
        return MoveOutcome::GameOver;
    if (moved)
        return MoveOutcome::OK;
    return MoveOutcome::InvalidMove;
}
