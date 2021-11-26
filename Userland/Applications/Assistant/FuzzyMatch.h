/*
 * Copyright (c) 2021, Spencer Dixon <spencercdixon@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/Tuple.h>

namespace Assistant {

struct FuzzyMatchResult {
    bool matched { false };
    int score { 0 };
};

// This fuzzy_match algorithm is based off a similar algorithm used by Sublime Text. The key insight is that instead
// of doing a total in the distance between characters (I.E. Levenshtein Distance), we apply some meaningful heuristics
// related to our dataset that we're trying to match to build up a score. Scores can then be sorted and displayed
// with the highest at the top.
//
// Scores are not normalized between any values and have no particular meaning. The starting value is 100 and when we
// detect good indicators of a match we add to the score. When we detect bad indicators, we penalize the match and subtract
// from its score. Therefore, the longer the needle/haystack the greater the range of scores could be.
FuzzyMatchResult fuzzy_match(String const& needle, String const& haystack);

}
