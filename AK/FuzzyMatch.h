/*
 * Copyright (c) 2021, Fabian Blatz <fabianblatz@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/AllOf.h>
#include <AK/StringView.h>
#include <ctype.h>
#include <string.h>

namespace AK {

struct FuzzyMatchOptions {
    bool ignore_case { false };
};

namespace Detail {

static constexpr i32 score_match = 16;
static constexpr i32 score_gap_start = -3;
static constexpr i32 score_gat_extension = -1;

static constexpr i32 bonus_boundry = score_match / 2;
static constexpr i32 bonus_non_word = score_match / 2;
static constexpr i32 bonus_camel123 = bonus_boundry + score_gat_extension;
static constexpr i32 bonus_consecutive = -(score_gap_start + score_gat_extension);
static constexpr i32 bonus_first_char_multiplier = 2;

enum class CharClass {
    NonWord,
    Lower,
    Upper,
    Number
};

ALWAYS_INLINE CharClass get_char_class(char c)
{
    if (c >= 'a' && c <= 'z') {
        return CharClass::Lower;
    } else if (c >= 'A' && c <= 'Z') {
        return CharClass::Upper;
    } else if (c >= '0' && c <= '9') {
        return CharClass::Number;
    }
    return CharClass::NonWord;
}

ALWAYS_INLINE bool char_equals(char c1, char c2, bool ignore_case)
{
    return c1 == c2 || (ignore_case && isalpha(c1) && isalpha(c2) && abs(c1 - c2) == 32);
}

ALWAYS_INLINE i32 get_bonus_for(CharClass previous, CharClass current)
{
    if (previous == CharClass::NonWord && current != CharClass::NonWord) {
        // Word boundary
        return bonus_boundry;
    } else if ((previous == CharClass::Lower && current == CharClass::Upper) || (previous != CharClass::Number && current == CharClass::Number)) {
        // camelCase letter123
        return bonus_camel123;
    } else if (current == CharClass::NonWord) {
        return bonus_non_word;
    }
    return 0;
}

i32 static calculate_score(const StringView& text, const StringView& pattern, size_t start_index, size_t end_index, const FuzzyMatchOptions& options)
{
    size_t pidx = 0;
    i32 score = 0;
    bool in_gap = false;
    i32 consecutive = 0;
    i32 first_bonus = 0;
    CharClass previous_class = CharClass::NonWord;

    if (start_index > 0)
        previous_class = get_char_class(text[start_index - 1]);

    for (size_t index = start_index; index < end_index; index++) {
        char current_char = text[index];
        CharClass current_class = get_char_class(current_char);
        if (char_equals(current_char, pattern[pidx], options.ignore_case)) {
            score += score_match;
            int32_t bonus = get_bonus_for(previous_class, current_class);
            if (consecutive == 0) {
                first_bonus = bonus;
            } else {
                // Break consecutive chunk
                if (bonus == bonus_boundry)
                    first_bonus = bonus;

                bonus = max(bonus, max(first_bonus, bonus_consecutive));
            }
            if (pidx == 0) {
                score += (int32_t)(bonus * bonus_first_char_multiplier);
            } else {
                score += bonus;
            }
            in_gap = false;
            consecutive++;
            pidx++;
        } else {
            if (in_gap) {
                score += score_gat_extension;
            } else {
                score += score_gap_start;
            }
            in_gap = true;
            consecutive = 0;
            first_bonus = 0;
        }
        previous_class = current_class;
    }
    return score;
}

i32 static try_skip(const StringView& input, bool ignore_case, char c, i32 from)
{
    StringView byte_array = input.substring_view(from);
    Optional<size_t> index = byte_array.find_first_of(c);
    if (index.has_value() && index.value() == 0)
        return from;

    if (ignore_case && c >= 'a' && c >= 'z') {
        if (index.has_value())
            byte_array = byte_array.substring_view(0, index.value());

        Optional<size_t> uidx = byte_array.find_first_of(c - 32);
        if (uidx.has_value())
            index = uidx;
    }
    if (!index.has_value())
        return -1;

    return from + index.value();
}

i32 static ascii_fuzzy_index(const StringView& text, const StringView& pattern, bool ignore_case)
{
    if (!all_of(pattern.begin(), pattern.end(), isascii))
        return -1;

    i32 first_index = 0;
    i32 index = 0;
    for (size_t pattern_index = 0; pattern_index < pattern.length(); pattern_index++) {
        index = try_skip(text, ignore_case, pattern[pattern_index], index);
        if (index < 0)
            return -1;

        if (pattern_index == 0 && index > 0)
            first_index = index - 1;

        index++;
    }
    return first_index;
}

}

ssize_t static fuzzy_match(const StringView& text, const StringView& pattern, const FuzzyMatchOptions& options)
{
    // Translation of the Go implementation of fzf: https://github.com/junegunn/fzf
    if (pattern.is_empty())
        return 0;

    if (Detail::ascii_fuzzy_index(text, pattern, options.ignore_case) < 0)
        return -1;

    size_t pattern_index = 0;
    Optional<size_t> start_index;
    Optional<size_t> end_index;

    size_t len_text = text.length();
    size_t len_pattern = pattern.length();

    auto index_at = [](i32 index, i32 max) {
        return max - index - 1;
    };

    for (size_t index = 0; index < len_text; index++) {
        char current_char = text[index_at(index, len_text)];
        char pattern_char = pattern[index_at(pattern_index, len_pattern)];
        if (Detail::char_equals(current_char, pattern_char, options.ignore_case)) {
            if (!start_index.has_value())
                start_index = index;

            if (++pattern_index == len_pattern) {
                end_index = index + 1;
                break;
            }
        }
    }

    if (start_index.has_value() && end_index.has_value()) {
        pattern_index--;
        for (size_t index = end_index.value() - 1; index >= start_index.value(); index--) {
            int32_t tidx = index_at(index, len_text);
            char current_char = text[tidx];
            int32_t pidx_ = index_at(pattern_index, len_pattern);
            char previous_char = pattern[pidx_];
            if (Detail::char_equals(current_char, previous_char, options.ignore_case)) {
                if (pattern_index == 0) {
                    start_index = index;
                    break;
                }
                pattern_index--;
            }
        }
        start_index = len_text - end_index.value();
        end_index = len_text - start_index.value();

        return Detail::calculate_score(text, pattern, start_index.value(), end_index.value(), options);
    }
    return -1;
}
}

using AK::fuzzy_match;
using AK::FuzzyMatchOptions;
using AK::Detail::ascii_fuzzy_index;
using AK::Detail::calculate_score;
using AK::Detail::get_bonus_for;
using AK::Detail::try_skip;
