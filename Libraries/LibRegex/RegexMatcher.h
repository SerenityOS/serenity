/*
 * Copyright (c) 2020, Emanuel Sprung <emanuel.sprung@gmail.com>
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

#pragma once

#include "RegexByteCode.h"
#include "RegexMatch.h"
#include "RegexOptions.h"
#include "RegexParser.h"

#include <AK/Forward.h>
#include <AK/HashMap.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/Types.h>
#include <AK/Utf32View.h>
#include <AK/Vector.h>

#include <stdio.h>

namespace regex {

static const constexpr size_t c_max_recursion = 5000;
static const constexpr size_t c_match_preallocation_count = 0;

struct RegexResult final {
    bool success { false };
    size_t count { 0 };
    Vector<Match> matches;
    Vector<Vector<Match>> capture_group_matches;
    Vector<HashMap<String, Match>> named_capture_group_matches;
    size_t n_operations { 0 };
    size_t n_capture_groups { 0 };
    size_t n_named_capture_groups { 0 };
};

template<class Parser>
class Regex;

template<class Parser>
class Matcher final {

public:
    Matcher(const Regex<Parser>& pattern, Optional<typename ParserTraits<Parser>::OptionsType> regex_options = {})
        : m_pattern(pattern)
        , m_regex_options(regex_options.value_or({}))
    {
    }
    ~Matcher() = default;

    RegexResult match(const RegexStringView&, Optional<typename ParserTraits<Parser>::OptionsType> = {}) const;
    RegexResult match(const Vector<RegexStringView>, Optional<typename ParserTraits<Parser>::OptionsType> = {}) const;

    typename ParserTraits<Parser>::OptionsType options() const
    {
        return m_regex_options;
    }

private:
    Optional<bool> execute(const MatchInput& input, MatchState& state, MatchOutput& output, size_t recursion_level) const;
    ALWAYS_INLINE Optional<bool> execute_low_prio_forks(const MatchInput& input, MatchState& original_state, MatchOutput& output, Vector<MatchState> states, size_t recursion_level) const;

    const Regex<Parser>& m_pattern;
    const typename ParserTraits<Parser>::OptionsType m_regex_options;
};

template<class Parser>
class Regex final {
public:
    String pattern_value;
    regex::Parser::Result parser_result;
    OwnPtr<Matcher<Parser>> matcher { nullptr };
    mutable size_t start_offset { 0 };

    explicit Regex(StringView pattern, typename ParserTraits<Parser>::OptionsType regex_options = {});
    ~Regex() = default;

    typename ParserTraits<Parser>::OptionsType options() const;
    void print_bytecode(FILE* f = stdout) const;
    String error_string(Optional<String> message = {}) const;

    RegexResult match(const RegexStringView view, Optional<typename ParserTraits<Parser>::OptionsType> regex_options = {}) const
    {
        if (!matcher || parser_result.error != Error::NoError)
            return {};
        return matcher->match(view, regex_options);
    }

    RegexResult match(const Vector<RegexStringView> views, Optional<typename ParserTraits<Parser>::OptionsType> regex_options = {}) const
    {
        if (!matcher || parser_result.error != Error::NoError)
            return {};
        return matcher->match(views, regex_options);
    }

    RegexResult search(const RegexStringView view, Optional<typename ParserTraits<Parser>::OptionsType> regex_options = {}) const
    {
        if (!matcher || parser_result.error != Error::NoError)
            return {};

        AllOptions options = (AllOptions)regex_options.value_or({});
        if ((options & AllFlags::MatchNotBeginOfLine) && (options & AllFlags::MatchNotEndOfLine)) {
            options.reset_flag(AllFlags::MatchNotEndOfLine);
            options.reset_flag(AllFlags::MatchNotBeginOfLine);
        }
        options |= AllFlags::Global;

        return matcher->match(view, options);
    }

    RegexResult search(const Vector<RegexStringView> views, Optional<typename ParserTraits<Parser>::OptionsType> regex_options = {}) const
    {
        if (!matcher || parser_result.error != Error::NoError)
            return {};

        AllOptions options = (AllOptions)regex_options.value_or({});
        if ((options & AllFlags::MatchNotBeginOfLine) && (options & AllFlags::MatchNotEndOfLine)) {
            options.reset_flag(AllFlags::MatchNotEndOfLine);
            options.reset_flag(AllFlags::MatchNotBeginOfLine);
        }
        options |= AllFlags::Global;

        return matcher->match(views, options);
    }

    bool match(const RegexStringView view, RegexResult& m, Optional<typename ParserTraits<Parser>::OptionsType> regex_options = {}) const
    {
        m = match(view, regex_options);
        return m.success;
    }

    bool match(const Vector<RegexStringView> views, RegexResult& m, Optional<typename ParserTraits<Parser>::OptionsType> regex_options = {}) const
    {
        m = match(views, regex_options);
        return m.success;
    }

    bool search(const RegexStringView view, RegexResult& m, Optional<typename ParserTraits<Parser>::OptionsType> regex_options = {}) const
    {
        m = search(view, regex_options);
        return m.success;
    }

    bool search(const Vector<RegexStringView> views, RegexResult& m, Optional<typename ParserTraits<Parser>::OptionsType> regex_options = {}) const
    {
        m = search(views, regex_options);
        return m.success;
    }

    bool has_match(const RegexStringView view, Optional<typename ParserTraits<Parser>::OptionsType> regex_options = {}) const
    {
        if (!matcher || parser_result.error != Error::NoError)
            return false;
        RegexResult result = matcher->match(view, AllOptions { regex_options.value_or({}) } | AllFlags::SkipSubExprResults);
        return result.success;
    }

    bool has_match(const Vector<RegexStringView> views, Optional<typename ParserTraits<Parser>::OptionsType> regex_options = {}) const
    {
        if (!matcher || parser_result.error != Error::NoError)
            return false;
        RegexResult result = matcher->match(views, AllOptions { regex_options.value_or({}) } | AllFlags::SkipSubExprResults);
        return result.success;
    }
};

// free standing functions for match, search and has_match
template<class Parser>
RegexResult match(const RegexStringView view, Regex<Parser>& pattern, Optional<typename ParserTraits<Parser>::OptionsType> regex_options = {})
{
    return pattern.match(view, regex_options);
}

template<class Parser>
RegexResult match(const Vector<RegexStringView> view, Regex<Parser>& pattern, Optional<typename ParserTraits<Parser>::OptionsType> regex_options = {})
{
    return pattern.match(view, regex_options);
}

template<class Parser>
bool match(const RegexStringView view, Regex<Parser>& pattern, RegexResult&, Optional<typename ParserTraits<Parser>::OptionsType> regex_options = {})
{
    return pattern.match(view, regex_options);
}

template<class Parser>
bool match(const Vector<RegexStringView> view, Regex<Parser>& pattern, RegexResult&, Optional<typename ParserTraits<Parser>::OptionsType> regex_options = {})
{
    return pattern.match(view, regex_options);
}

template<class Parser>
RegexResult search(const RegexStringView view, Regex<Parser>& pattern, Optional<typename ParserTraits<Parser>::OptionsType> regex_options = {})
{
    return pattern.search(view, regex_options);
}

template<class Parser>
RegexResult search(const Vector<RegexStringView> views, Regex<Parser>& pattern, Optional<typename ParserTraits<Parser>::OptionsType> regex_options = {})
{
    return pattern.search(views, regex_options);
}

template<class Parser>
bool search(const RegexStringView view, Regex<Parser>& pattern, RegexResult&, Optional<typename ParserTraits<Parser>::OptionsType> regex_options = {})
{
    return pattern.search(view, regex_options);
}

template<class Parser>
bool search(const Vector<RegexStringView> views, Regex<Parser>& pattern, RegexResult&, Optional<typename ParserTraits<Parser>::OptionsType> regex_options = {})
{
    return pattern.search(views, regex_options);
}

template<class Parser>
bool has_match(const RegexStringView view, Regex<Parser>& pattern, Optional<typename ParserTraits<Parser>::OptionsType> regex_options = {})
{
    return pattern.has_match(view, regex_options);
}

template<class Parser>
bool has_match(const Vector<RegexStringView> views, Regex<Parser>& pattern, Optional<typename ParserTraits<Parser>::OptionsType> regex_options = {})
{
    return pattern.has_match(views, regex_options);
}
}

using regex::has_match;
using regex::match;
using regex::Regex;
using regex::RegexResult;
