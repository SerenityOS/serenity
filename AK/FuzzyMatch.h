/*
 * Copyright (c) 2021, Spencer Dixon <spencercdixon@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>

namespace AK {

struct FuzzyMatchResult {
    bool matched { false };
    int score { 0 };
};

FuzzyMatchResult fuzzy_match(StringView needle, StringView haystack);

}

#if USING_AK_GLOBALLY
using AK::fuzzy_match;
using AK::FuzzyMatchResult;
#endif
