/*
 * Copyright (c) 2021, Spencer Dixon <spencercdixon@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FuzzyMatch.h"
#include <string.h>

namespace Assistant {

static constexpr const int RECURSION_LIMIT = 10;
static constexpr const int MAX_MATCHES = 256;

// Bonuses and penalties are used to build up a final score for the match.
static constexpr const int SEQUENTIAL_BONUS = 15;            // bonus for adjacent matches (needle: 'ca', haystack: 'cat')
static constexpr const int SEPARATOR_BONUS = 30;             // bonus if match occurs after a separator ('_' or ' ')
static constexpr const int CAMEL_BONUS = 30;                 // bonus if match is uppercase and prev is lower (needle: 'myF' haystack: '/path/to/myFile.txt')
static constexpr const int FIRST_LETTER_BONUS = 20;          // bonus if the first letter is matched (needle: 'c' haystack: 'cat')
static constexpr const int LEADING_LETTER_PENALTY = -5;      // penalty applied for every letter in str before the first match
static constexpr const int MAX_LEADING_LETTER_PENALTY = -15; // maximum penalty for leading letters
static constexpr const int UNMATCHED_LETTER_PENALTY = -1;    // penalty for every letter that doesn't matter

static FuzzyMatchResult fuzzy_match_recursive(String const& needle, String const& haystack, size_t needle_idx, size_t haystack_idx,
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

        if (needle.substring(needle_idx, 1).to_lowercase() == haystack.substring(haystack_idx, 1).to_lowercase()) {
            if (next_match >= MAX_MATCHES)
                return { false, out_score };

            if (first_match && src_matches) {
                memcpy(matches, src_matches, next_match);
                first_match = false;
            }

            u8 recursive_matches[recursive_match_limit];
            auto result = fuzzy_match_recursive(needle, haystack, needle_idx, haystack_idx + 1, matches, recursive_matches, next_match, recursion_count);
            if (result.matched) {
                if (!had_recursive_match || result.score > best_recursive_score) {
                    memcpy(best_recursive_matches, recursive_matches, recursive_match_limit);
                    best_recursive_score = result.score;
                }
                had_recursive_match = true;
                matches[next_match++] = haystack_idx;
            }
            needle_idx++;
        }
        haystack_idx++;
    }

    bool matched = needle_idx == needle.length();
    if (matched) {
        out_score = 100;

        int penalty = LEADING_LETTER_PENALTY * matches[0];
        if (penalty < MAX_LEADING_LETTER_PENALTY)
            penalty = MAX_LEADING_LETTER_PENALTY;
        out_score += penalty;

        int unmatched = haystack.length() - next_match;
        out_score += UNMATCHED_LETTER_PENALTY * unmatched;

        for (int i = 0; i < next_match; i++) {
            u8 current_idx = matches[i];

            if (i > 0) {
                u8 previous_idx = matches[i - 1];
                if (current_idx == previous_idx)
                    out_score += SEQUENTIAL_BONUS;
            }

            if (current_idx > 0) {
                auto current_character = haystack.substring(current_idx, 1);
                auto neighbor_character = haystack.substring(current_idx - 1, 1);

                if (neighbor_character != neighbor_character.to_uppercase() && current_character != current_character.to_lowercase())
                    out_score += CAMEL_BONUS;

                if (neighbor_character == "_" || neighbor_character == " ")
                    out_score += SEPARATOR_BONUS;
            } else {
                out_score += FIRST_LETTER_BONUS;
            }
        }

        if (had_recursive_match && (!matched || best_recursive_score > out_score)) {
            memcpy(matches, best_recursive_matches, MAX_MATCHES);
            out_score = best_recursive_score;
            return { true, out_score };
        } else if (matched) {
            return { true, out_score };
        }
    }

    return { false, out_score };
}

FuzzyMatchResult fuzzy_match(String needle, String haystack)
{
    int recursion_count = 0;
    u8 matches[MAX_MATCHES];
    return fuzzy_match_recursive(needle, haystack, 0, 0, nullptr, matches, 0, recursion_count);
}

}
