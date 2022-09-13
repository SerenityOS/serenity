/*
 * Copyright (c) 2021, Spencer Dixon <spencercdixon@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>

namespace AK {

struct FuzzyMatchResult {
    bool matched { false };
    int score { 0 };
};

FuzzyMatchResult fuzzy_match_recursive(String const& needle, String const& haystack, size_t needle_idx, size_t haystack_idx,
    u8 const* src_matches, u8* matches, int next_match, int& recursion_count);

FuzzyMatchResult fuzzy_match(String const& needle, String const& haystack);

}

using AK::fuzzy_match;
using AK::FuzzyMatchResult;
