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
    Game(Game const&) = default;
    Game& operator=(Game const&) = default;

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
    void set_want_to_continue() { m_want_to_continue = true; }
    class Board {
    public:
        using Row = Vector<u32>;
        using Tiles = Vector<Row>;

        Tiles const& tiles() const { return m_tiles; }

        bool is_stalled();

        struct Position {
            size_t row;
            size_t column;

            bool operator==(Position const& other) const
            {
                return row == other.row && column == other.column;
            }
        };

        void add_tile(size_t row, size_t column, u32 value)
        {
            m_tiles[row][column] = value;
            m_last_added_position = Position { row, column };
        }
        Position const& last_added_position() const { return m_last_added_position; }

        struct SlideResult {
            bool has_moved;
            size_t score_delta;
        };
        SlideResult slide_tiles(Direction);

        struct SlidingTile {
            size_t row_from;
            size_t column_from;
            u32 value_from;

            size_t row_to;
            size_t column_to;
            u32 value_to;
        };
        Vector<SlidingTile> const& sliding_tiles() const { return m_sliding_tiles; }

    private:
        void reverse();
        void transpose();

        size_t slide_row(size_t row_index);
        size_t slide_left();

        friend Game;

        Tiles m_tiles;

        Position m_last_added_position { 0, 0 };
        Vector<SlidingTile> m_sliding_tiles;
    };

    Board const& board() const { return m_board; }

    static size_t max_power_for_board(size_t size)
    {
        if (size >= 6)
            return 31;

        return size * size + 1;
    }

private:
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
    bool m_want_to_continue { false };

    Board m_board;
    size_t m_score { 0 };
    size_t m_turns { 0 };
};
