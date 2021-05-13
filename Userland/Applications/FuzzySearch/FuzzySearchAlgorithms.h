/*
 * Copyright (c) 2021, Fabian Blatz <fabianblatz@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <ctype.h>
#include <string.h>

class FuzzySearchAlgorithms {
public:
    struct SearchOptions {
        bool ignore_case { false };
    };

    static double levenshtein_distance(const StringView&, const StringView&, const SearchOptions&);
    static double fzf_match_v1(const StringView&, const StringView&, const SearchOptions&);

    ALWAYS_INLINE static bool char_equals(char c1, char c2, bool ignore_case)
    {
        return c1 == c2 || (ignore_case && isalpha(c1) && isalpha(c2) && abs(c1 - c2) == 32);
    }

private:
    enum class CharClass {
        NonWord,
        Lower,
        Upper,
        Number
    };

    ALWAYS_INLINE static CharClass get_char_class(char c)
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

    static i32 get_bonus_for(CharClass, CharClass);
    static i32 ascii_fuzzy_index(const StringView&, const StringView&, bool);
    static i32 calculate_score(const StringView&, const StringView&, size_t, size_t, const SearchOptions&);
    static i32 try_skip(const StringView&, bool, char, i32);
    FuzzySearchAlgorithms()
    {
    }
};
