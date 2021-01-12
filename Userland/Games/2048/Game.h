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

#pragma once

#include <AK/Vector.h>

class Game final {
public:
    Game(size_t board_size, size_t target_tile = 0);
    Game(const Game&) = default;

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
    void add_random_tile();

    size_t m_grid_size { 0 };
    u32 m_target_tile { 0 };

    Board m_board;
    size_t m_score { 0 };
    size_t m_turns { 0 };
};
