/*
 * Copyright (c) 2021, Spencer Dixon <spencercdixon@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/FuzzyMatch.h>
#include <string.h>

namespace AK {

static constexpr int RECURSION_LIMIT = 10;
static constexpr int MAX_MATCHES = 256;

// Bonuses and penalties are used to build up a final score for the match.
static constexpr int SEQUENTIAL_BONUS = 15;                      // bonus for adjacent matches (needle: 'ca', haystack: 'cat')
static constexpr int SEPARATOR_BONUS = 30;                       // bonus if match occurs after a separator ('_' or ' ')
static constexpr int CAMEL_BONUS = 30;                           // bonus if match is uppercase and prev is lower (needle: 'myF' haystack: '/path/to/myFile.txt')
static constexpr int FIRST_LETTER_BONUS = 15;                    // bonus if the first letter is matched (needle: 'c' haystack: 'cat')
static constexpr int LEADING_LETTER_PENALTY = -5;                // penalty applied for every letter in str before the first match
static constexpr int MAX_LEADING_LETTER_PENALTY = -15;           // maximum penalty for leading letters
static constexpr int UNMATCHED_LETTER_PENALTY = -1;              // penalty for every letter that doesn't matter
static constexpr int EQUALITY_SCORE = NumericLimits<int>::max(); // Score set on perfect equality

static int calculate_score(StringView string, u8 const* index_points, size_t index_points_size)
{
    int out_score = 100;

    int penalty = LEADING_LETTER_PENALTY * index_points[0];
    if (penalty < MAX_LEADING_LETTER_PENALTY)
        penalty = MAX_LEADING_LETTER_PENALTY;
    out_score += penalty;

    int unmatched = string.length() - index_points_size;
    out_score += UNMATCHED_LETTER_PENALTY * unmatched;

    if (unmatched == 0)
        return EQUALITY_SCORE;

    for (size_t i = 0; i < index_points_size; i++) {
        u8 current_idx = index_points[i];

        if (i > 0) {
            u8 previous_idx = index_points[i - 1];
            if (current_idx - 1 == previous_idx)
                out_score += SEQUENTIAL_BONUS;
        }

        if (current_idx == 0) {
            out_score += FIRST_LETTER_BONUS;
        } else {
            u32 current_character = string[current_idx];
            u32 neighbor_character = string[current_idx - 1];

            if (is_ascii_lower_alpha(neighbor_character) && is_ascii_upper_alpha(current_character))
                out_score += CAMEL_BONUS;

            if (neighbor_character == '_' || neighbor_character == ' ')
                out_score += SEPARATOR_BONUS;
        }
    }

    return out_score;
}

static FuzzyMatchResult fuzzy_match_recursive(StringView needle, StringView haystack, size_t needle_idx, size_t haystack_idx,
    u8 const* src_matches, u8* matches, int next_match, int& recursion_count)
{
    int out_score = 0;

    ++recursion_count;
    if (recursion_count >= RECURSION_LIMIT)
        return { false, out_score };

    if (needle.length() == needle_idx || haystack.length() == haystack_idx)
        return { false, out_score };

    bool had_recursive_match = false;
    constexpr size_t recursive_match_limit = 256;
    u8 best_recursive_matches[recursive_match_limit];
    int best_recursive_score = 0;

    bool first_match = true;
    while (needle_idx < needle.length() && haystack_idx < haystack.length()) {

        if (to_ascii_lowercase(needle[needle_idx]) == to_ascii_lowercase(haystack[haystack_idx])) {
            if (next_match >= MAX_MATCHES)
                return { false, out_score };

            if (first_match && src_matches) {
                memcpy(matches, src_matches, next_match);
                first_match = false;
            }

            u8 recursive_matches[recursive_match_limit] {};
            auto result = fuzzy_match_recursive(needle, haystack, needle_idx, haystack_idx + 1, matches, recursive_matches, next_match, recursion_count);
            if (result.matched) {
                if (!had_recursive_match || result.score > best_recursive_score) {
                    memcpy(best_recursive_matches, recursive_matches, recursive_match_limit);
                    best_recursive_score = result.score;
                }
                had_recursive_match = true;
            }
            matches[next_match++] = haystack_idx;
            needle_idx++;
        }
        haystack_idx++;
    }

    bool matched = needle_idx == needle.length();
    if (!matched)
        return { false, out_score };

    out_score = calculate_score(haystack, matches, next_match);

    if (had_recursive_match && (best_recursive_score > out_score)) {
        memcpy(matches, best_recursive_matches, MAX_MATCHES);
        out_score = best_recursive_score;
    }

    return { true, out_score };
}

// This fuzzy_match algorithm is based off a similar algorithm used by Sublime Text. The key insight is that instead
// of doing a total in the distance between characters (I.E. Levenshtein Distance), we apply some meaningful heuristics
// related to our dataset that we're trying to match to build up a score. Scores can then be sorted and displayed
// with the highest at the top.
//
// Scores are not normalized between any values and have no particular meaning. The starting value is 100 and when we
// detect good indicators of a match we add to the score. When we detect bad indicators, we penalize the match and subtract
// from its score. Therefore, the longer the needle/haystack the greater the range of scores could be.
FuzzyMatchResult fuzzy_match(StringView needle, StringView haystack)
{
    int recursion_count = 0;
    u8 matches[MAX_MATCHES] {};
    return fuzzy_match_recursive(needle, haystack, 0, 0, nullptr, matches, 0, recursion_count);
}

}
