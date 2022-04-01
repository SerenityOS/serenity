/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/Vector.h>

namespace Diff {

struct HunkLocation {
    size_t original_start_line { 0 };
    size_t original_length { 0 };
    size_t target_start_line { 0 };
    size_t target_length { 0 };

    enum class LocationType {
        Original,
        Target,
        Both
    };
    void apply_offset(size_t offset, LocationType);
};

struct Hunk {
    size_t original_start_line { 0 };
    size_t target_start_line { 0 };
    Vector<String> removed_lines;
    Vector<String> added_lines;
};

Vector<Hunk> parse_hunks(String const& diff);
HunkLocation parse_hunk_location(String const& location_line);
};
