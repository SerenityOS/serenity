/*
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Generator.h"

namespace Diff {

Vector<Hunk> from_text(StringView const& old_text, StringView const& new_text)
{
    auto old_lines = old_text.lines();
    auto new_lines = new_text.lines();

    /**
     * This is a simple implementation of the Longest Common Subsequence algorithm (over
     * the lines of the text as opposed to the characters). A Dynamic programming approach
     * is used here.
     */

    enum class Direction {
        Up,       // Added a new line
        Left,     // Removed a line
        Diagonal, // Line remained the same
    };

    // A single cell in the DP-matrix. Cell (i, j) represents the longest common
    // sub-sequence of lines between old_lines[0 : i] and new_lines[0 : j].
    struct Cell {
        size_t length;
        Direction direction;
    };

    auto dp_matrix = Vector<Cell>();
    dp_matrix.resize((old_lines.size() + 1) * (new_lines.size() + 1));

    auto dp = [&dp_matrix, width = old_lines.size() + 1](size_t i, size_t j) -> Cell& {
        return dp_matrix[i + width * j];
    };

    // Initialize the first row and column
    for (size_t i = 0; i <= old_lines.size(); ++i)
        dp(i, 0) = { 0, Direction::Left };

    for (size_t j = 0; j <= new_lines.size(); ++j)
        dp(0, j) = { 0, Direction::Up };

    // Fill in the rest of the DP table
    for (size_t i = 1; i <= old_lines.size(); ++i) {
        for (size_t j = 1; j <= new_lines.size(); ++j) {
            if (old_lines[i - 1] == new_lines[j - 1]) {
                dp(i, j) = { dp(i - 1, j - 1).length + 1, Direction::Diagonal };
            } else {
                auto up = dp(i, j - 1).length;
                auto left = dp(i - 1, j).length;
                if (up > left)
                    dp(i, j) = { up, Direction::Up };
                else
                    dp(i, j) = { left, Direction::Left };
            }
        }
    }

    Vector<Hunk> hunks;
    size_t i = old_lines.size();
    size_t j = new_lines.size();

    // FIXME: This creates a hunk per line, very inefficient.
    while (i > 0 && j > 0) {
        auto& cell = dp(i, j);
        if (cell.direction == Direction::Up) {
            --j;
            hunks.append({ i, j, {}, { new_lines[j] } });
        } else if (cell.direction == Direction::Left) {
            --i;
            hunks.append({ i, j, { old_lines[i] }, {} });
        } else if (cell.direction == Direction::Diagonal) {
            --i;
            --j;
        }
    }

    hunks.reverse();
    return hunks;
}

}
