/*
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Generator.h"

namespace Diff {

Vector<Hunk> from_text(StringView old_text, StringView new_text)
{
    auto old_lines = old_text.lines();
    auto new_lines = new_text.lines();

    /**
     * This is a simple implementation of the Longest Common Subsequence algorithm (over
     * the lines of the text as opposed to the characters). A Dynamic programming approach
     * is used here.
     */

    enum class Direction {
        Down,     // Added a new line
        Right,    // Removed a line
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
        dp(i, new_lines.size()) = { 0, Direction::Right };

    for (size_t j = 0; j <= new_lines.size(); ++j)
        dp(old_lines.size(), 0) = { 0, Direction::Down };

    // Fill in the rest of the DP table
    for (int i = old_lines.size() - 1; i >= 0; --i) {
        for (int j = new_lines.size() - 1; j >= 0; --j) {
            if (old_lines[i] == new_lines[j]) {
                dp(i, j) = { dp(i + 1, j + 1).length + 1, Direction::Diagonal };
            } else {
                auto down = dp(i, j + 1).length;
                auto right = dp(i + 1, j).length;
                if (down > right)
                    dp(i, j) = { down, Direction::Down };
                else
                    dp(i, j) = { right, Direction::Right };
            }
        }
    }

    Vector<Hunk> hunks;
    Hunk cur_hunk;
    bool in_hunk = false;

    auto update_hunk = [&](size_t i, size_t j, Direction direction) {
        if (!in_hunk) {
            in_hunk = true;
            cur_hunk = { i, j, {}, {} };
        }
        if (direction == Direction::Down) {
            cur_hunk.added_lines.append(new_lines[j]);
        } else if (direction == Direction::Right) {
            cur_hunk.removed_lines.append(old_lines[i]);
        }
    };

    auto flush_hunk = [&]() {
        if (in_hunk) {
            if (cur_hunk.added_lines.size() > 0)
                cur_hunk.target_start_line++;
            if (cur_hunk.removed_lines.size() > 0)
                cur_hunk.original_start_line++;
            hunks.append(cur_hunk);
            in_hunk = false;
        }
    };

    size_t i = 0;
    size_t j = 0;

    while (i < old_lines.size() && j < new_lines.size()) {
        auto& cell = dp(i, j);
        if (cell.direction == Direction::Down) {
            update_hunk(i, j, cell.direction);
            ++j;
        } else if (cell.direction == Direction::Right) {
            update_hunk(i, j, cell.direction);
            ++i;
        } else {
            ++i;
            ++j;
            flush_hunk();
        }
    }

    while (i < old_lines.size() && new_lines.size() > 0) {
        update_hunk(i, new_lines.size() - 1, Direction::Right); // Remove a line
        ++i;
    }
    while (j < new_lines.size() && old_lines.size() > 0) {
        update_hunk(old_lines.size() - 1, j, Direction::Down); // Add a line
        ++j;
    }

    flush_hunk();

    return hunks;
}

}
