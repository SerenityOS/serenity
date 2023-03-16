/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>

namespace Snake {

enum class Direction {
    Up,
    Right,
    Down,
    Left,
};

struct Coordinate {
    int row { 0 };
    int column { 0 };

    bool operator==(Coordinate const& other) const
    {
        return row == other.row && column == other.column;
    }
};

struct Velocity {
    int vertical { 0 };
    int horizontal { 0 };

    Direction as_direction() const
    {
        if (vertical > 0)
            return Direction::Down;
        if (vertical < 0)
            return Direction::Up;
        if (horizontal > 0)
            return Direction::Right;
        if (horizontal < 0)
            return Direction::Left;

        return Direction::Up;
    }
};

}
