/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>

class Game final {
public:
    Game(size_t grid_size, size_t target_tile, bool evil_ai);
    Game(const Game&) = default;
    Game& operator=(const Game&) = default;

    enum class MoveOutcome {
        OK,
        InvalidMove,
        GameOver,
        Won,
    };

    enum class Direction {
        Up,
        Down,
        Left,
        Right,
    };

    MoveOutcome attempt_move(Direction);

    size_t score() const { return m_score; }
    size_t turns() const { return m_turns; }
    u32 target_tile() const { return m_target_tile; }
    u32 largest_tile() const;

    using Board = Vector<Vector<u32>>;

    const Board& board() const { return m_board; }

    static size_t max_power_for_board(size_t size)
    {
        if (size >= 6)
            return 31;

        return size * size + 1;
    }

private:
    bool slide_tiles(Direction);

    void add_tile()
    {
        if (m_evil_ai)
            add_evil_tile();
        else
            add_random_tile();
    }

    void add_random_tile();
    void add_evil_tile();

    size_t m_grid_size { 0 };
    u32 m_target_tile { 0 };

    bool m_evil_ai { false };

    Board m_board;
    size_t m_score { 0 };
    size_t m_turns { 0 };
};
