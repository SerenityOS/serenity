/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Hunks.h"
#include <AK/Debug.h>

namespace Diff {
Vector<Hunk> parse_hunks(const String& diff)
{
    Vector<String> diff_lines = diff.split('\n');
    if (diff_lines.is_empty())
        return {};

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
        if (diff_lines[line_index][0] == ' ') {
            current_location.apply_offset(1, HunkLocation::LocationType::Both);
            ++line_index;
            continue;
        }
        Hunk hunk {};
        hunk.original_start_line = current_location.original_start_line;
        hunk.target_start_line = current_location.target_start_line;

        while (line_index < diff_lines.size() && diff_lines[line_index][0] == '-') {
            hunk.removed_lines.append(diff_lines[line_index].substring(1, diff_lines[line_index].length() - 1));
            current_location.apply_offset(1, HunkLocation::LocationType::Original);
            ++line_index;
        }
        while (line_index < diff_lines.size() && diff_lines[line_index][0] == '+') {
            hunk.added_lines.append(diff_lines[line_index].substring(1, diff_lines[line_index].length() - 1));
            current_location.apply_offset(1, HunkLocation::LocationType::Target);
            ++line_index;
        }

        while (line_index < diff_lines.size() && diff_lines[line_index][0] == ' ') {
            current_location.apply_offset(1, HunkLocation::LocationType::Both);
            ++line_index;
        }
        hunks.append(hunk);
    }

    if constexpr (HUNKS_DEBUG) {
        for (const auto& hunk : hunks) {
            dbgln("Hunk location:");
            dbgln("  orig: {}", hunk.original_start_line);
            dbgln("  target: {}", hunk.target_start_line);
            dbgln("  removed:");
            for (const auto& line : hunk.removed_lines)
                dbgln("- {}", line);
            dbgln("  added:");
            for (const auto& line : hunk.added_lines)
                dbgln("+ {}", line);
        }
    }

    return hunks;
}

HunkLocation parse_hunk_location(const String& location_line)
{
    size_t char_index = 0;
    struct StartAndLength {
        size_t start { 0 };
        size_t length { 0 };
    };
    auto parse_start_and_length_pair = [](const String& raw) {
        auto index_of_separator = raw.find(',').value();
        auto start = raw.substring(0, index_of_separator).to_uint().value();
        auto length = raw.substring(index_of_separator + 1, raw.length() - index_of_separator - 1).to_uint().value();

        if (start != 0)
            start--;

        if (length != 0)
            length--;

        return StartAndLength { start, length };
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

    auto original_pair = parse_start_and_length_pair(location_line.substring(original_location_start_index, original_location_end_index - original_location_start_index + 1));
    auto target_pair = parse_start_and_length_pair(location_line.substring(target_location_start_index, target_location_end_index - target_location_start_index + 1));
    return { original_pair.start, original_pair.length, target_pair.start, target_pair.length };
}

void HunkLocation::apply_offset(size_t offset, HunkLocation::LocationType type)
{
    if (type == LocationType::Original || type == LocationType::Both) {
        original_start_line += offset;
        original_length -= offset;
    }
    if (type == LocationType::Target || type == LocationType::Both) {
        target_start_line += offset;
        target_length -= offset;
    }
}

};
