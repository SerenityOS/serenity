/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
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

#include "Hunks.h"

// #define DEBUG_HUNKS

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

#ifdef DEBUG_HUNKS
    for (const auto& hunk : hunks) {
        dbg() << "Hunk location:";
        dbg() << "orig: " << hunk.original_start_line;
        dbg() << "target: " << hunk.target_start_line;
        dbg() << "removed:";
        for (const auto& line : hunk.removed_lines) {
            dbg() << "- " << line;
        }
        dbg() << "added:";
        for (const auto& line : hunk.added_lines) {
            dbg() << "+ " << line;
        }
    }
#endif

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
        auto index_of_separator = raw.index_of(",").value();
        auto start = raw.substring(0, index_of_separator);
        auto length = raw.substring(index_of_separator + 1, raw.length() - index_of_separator - 1);
        auto res = StartAndLength { start.to_uint().value() - 1, length.to_uint().value() - 1 };
        return res;
    };
    while (char_index < location_line.length() && location_line[char_index++] != '-') {
    }
    ASSERT(char_index < location_line.length());

    size_t original_location_start_index = char_index;

    while (char_index < location_line.length() && location_line[char_index++] != ' ') {
    }
    ASSERT(char_index < location_line.length() && location_line[char_index] == '+');
    size_t original_location_end_index = char_index - 2;

    size_t target_location_start_index = char_index + 1;

    char_index += 1;
    while (char_index < location_line.length() && location_line[char_index++] != ' ') {
    }
    ASSERT(char_index < location_line.length());

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
