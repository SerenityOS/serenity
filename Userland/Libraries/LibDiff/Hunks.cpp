/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 * Copyright (c) 2023, Shannon Booth <shannon.ml.booth@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Hunks.h"
#include <AK/Debug.h>

namespace Diff {

ErrorOr<Vector<Hunk>> parse_hunks(StringView diff)
{
    Vector<StringView> diff_lines = diff.split_view('\n');
    if (diff_lines.is_empty())
        return Vector<Hunk> {};

    Vector<Hunk> hunks;

    size_t line_index = 0;
    HunkLocation current_location {};

    // Skip to first hunk
    while (diff_lines[line_index][0] != '@') {
        ++line_index;
    }

    while (line_index < diff_lines.size()) {
        if (diff_lines[line_index][0] == '@') {
            current_location = parse_hunk_location(diff_lines[line_index]);
            ++line_index;
            continue;
        }

        Hunk hunk {};
        hunk.location = current_location;

        while (line_index < diff_lines.size()) {
            auto const& line = diff_lines[line_index];

            char const operation = line[0];
            if (operation != ' ' && operation != '+' && operation != '-')
                break;

            auto const content = line.substring_view(1, line.length() - 1);

            TRY(hunk.lines.try_append(Line { Line::operation_from_symbol(operation), TRY(String::from_utf8(content)) }));

            ++line_index;
        }
        TRY(hunks.try_append(hunk));
    }

    if constexpr (HUNKS_DEBUG) {
        for (auto const& hunk : hunks) {
            dbgln("{}", hunk.location);
            for (auto const& line : hunk.lines)
                dbgln("{}", line);
        }
    }

    return hunks;
}

HunkLocation parse_hunk_location(StringView location_line)
{
    size_t char_index = 0;
    auto parse_start_and_length_pair = [](StringView raw) {
        auto maybe_index_of_separator = raw.find(',');

        size_t start = 0;
        size_t length = 0;
        if (maybe_index_of_separator.has_value()) {
            auto index_of_separator = maybe_index_of_separator.value();
            start = raw.substring_view(0, index_of_separator).to_uint().value();
            length = raw.substring_view(index_of_separator + 1, raw.length() - index_of_separator - 1).to_uint().value();
        } else {
            length = 1;
            start = raw.to_uint().value();
        }

        return Range { start, length };
    };
    while (char_index < location_line.length() && location_line[char_index++] != '-') {
    }
    VERIFY(char_index < location_line.length());

    size_t original_location_start_index = char_index;

    while (char_index < location_line.length() && location_line[char_index++] != ' ') {
    }
    VERIFY(char_index < location_line.length() && location_line[char_index] == '+');
    size_t original_location_end_index = char_index - 2;

    size_t target_location_start_index = char_index + 1;

    char_index += 1;
    while (char_index < location_line.length() && location_line[char_index++] != ' ') {
    }
    VERIFY(char_index < location_line.length());

    size_t target_location_end_index = char_index - 2;

    auto old_range = parse_start_and_length_pair(location_line.substring_view(original_location_start_index, original_location_end_index - original_location_start_index + 1));
    auto new_range = parse_start_and_length_pair(location_line.substring_view(target_location_start_index, target_location_end_index - target_location_start_index + 1));
    return { old_range, new_range };
}

};
