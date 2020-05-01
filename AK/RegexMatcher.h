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

#include "RegexOptions.h"
#include "RegexParser.h"

#include <AK/HashMap.h>
#include <AK/OwnPtr.h>
#include <AK/Types.h>
#include <AK/Vector.h>

namespace AK {
namespace regex {

static const constexpr size_t c_max_recursion = 5000;
static const constexpr size_t c_match_preallocation_count = 20;

class Match final {
private:
    Optional<String> string;

public:
    Match() = default;
    ~Match() = default;

    Match(const StringView view_, const size_t line_, const size_t column_)
        : view(view_)
        , line(line_)
        , column(column_)
    {
    }

    Match(const String string_, const size_t line_, const size_t column_)
        : string(string_)
        , view(string.value().view())
        , line(line_)
        , column(column_)
    {
    }

    StringView view { nullptr };
    size_t line { 0 };
    size_t column { 0 };
};

struct RegexResult final {
    bool success { false };
    size_t count { 0 };
    Vector<Match> matches;
    Vector<Vector<Match>> capture_group_matches;
    Vector<HashMap<String, Match>> named_capture_group_matches;
    size_t operations { 0 };
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

    RegexResult match(const StringView&, Optional<typename ParserTraits<Parser>::OptionsType> = {});

private:
    struct IpSpTuple {
        size_t ip { 0 };
        size_t sp { 0 };
    };

    struct MatchState {
        StringView view;
        size_t ip;
        size_t sp;
        size_t operations;

        Vector<Match> matches;
        Vector<Vector<Match>> capture_group_matches;
        Vector<HashMap<String, Match>> named_capture_group_matches;

        size_t match_index;
        size_t line { 0 };
        size_t column { 0 };

        typename ParserTraits<Parser>::OptionsType regex_options;
        const Vector<ByteCodeValue>& bytecode;

        MatchState(const Vector<ByteCodeValue>& bytecode)
            : bytecode(bytecode)
        {
        }

        const ByteCodeValue get(size_t offset = 0)
        {
            if (ip + offset < bytecode.size())
                return bytecode.at(ip + offset);
            else
                return ByteCodeValue(OpCode::Exit);
        }

        const ByteCodeValue pop(size_t value = 1)
        {
            auto& current = get();
            ip += value;
            return current;
        }

        void reset(size_t sp, size_t match_offset)
        {
            this->ip = 0;
            this->sp = sp;
            this->column = sp;
            this->match_index = match_offset;
        }
    };

    bool execute(MatchState&, size_t recursion_level = 0) const;

    const Regex<Parser>& m_pattern;
    const typename ParserTraits<Parser>::OptionsType m_regex_options;
};

template<class Parser>
class Regex final {
public:
    String pattern_value;
    ParserResult parser_result;
    OwnPtr<Matcher<Parser>> matcher { nullptr };

    Regex(StringView pattern, typename ParserTraits<Parser>::OptionsType regex_options = {});
    ~Regex() = default;

    void print_bytecode() const;
    String error_string() const;

    RegexResult match(StringView view, Optional<typename ParserTraits<Parser>::OptionsType> regex_options = {})
    {
        if (!matcher || parser_result.error != Error::NoError)
            return {};
        return matcher->match(view, regex_options);
    }

    RegexResult search(StringView view, Optional<typename ParserTraits<Parser>::OptionsType> regex_options = {})
    {
        if (!matcher || parser_result.error != Error::NoError)
            return {};
        return matcher->match(view, regex_options.value_or({}) | AllFlags::Global);
    }

    bool match(StringView view, RegexResult& m, Optional<typename ParserTraits<Parser>::OptionsType> regex_options = {})
    {
        if (!matcher || parser_result.error != Error::NoError)
            return {};
        m = matcher->match(view, regex_options);
        return m.success;
    }

    bool search(StringView view, RegexResult& m, Optional<typename ParserTraits<Parser>::OptionsType> regex_options = {})
    {
        if (!matcher || parser_result.error != Error::NoError)
            return {};
        m = matcher->match(view, regex_options.value_or({}) | AllFlags::Global);
        return m.success;
    }

    bool has_match(const StringView view, Optional<typename ParserTraits<Parser>::OptionsType> regex_options = {})
    {
        if (!matcher || parser_result.error != Error::NoError)
            return false;
        RegexResult result = matcher->match(view, regex_options.value_or({}) | AllFlags::NoSubExpressions);
        return result.success;
    }
};

template<class Parser>
RegexResult match(const StringView view, Regex<Parser>& pattern, Optional<typename ParserTraits<Parser>::OptionsType> regex_options = {})
{
    if (!pattern.matcher || pattern.parser_result.error != Error::NoError)
        return {};
    return pattern.matcher->match(view, regex_options);
}

template<class Parser>
bool match(const StringView view, Regex<Parser>& pattern, RegexResult& res, Optional<typename ParserTraits<Parser>::OptionsType> regex_options = {})
{
    if (!pattern.matcher || pattern.parser_result.error != Error::NoError)
        return {};
    res = pattern.matcher->match(view, regex_options);
    return res.success;
}

template<class Parser>
RegexResult search(const StringView view, Regex<Parser>& pattern, Optional<typename ParserTraits<Parser>::OptionsType> regex_options = {})
{
    if (!pattern.matcher || pattern.parser_result.error != Error::NoError)
        return {};
    return pattern.matcher->search(view, regex_options);
}

template<class Parser>
bool search(const StringView view, Regex<Parser>& pattern, RegexResult& res, Optional<typename ParserTraits<Parser>::OptionsType> regex_options = {})
{
    if (!pattern.matcher || pattern.parser_result.error != Error::NoError)
        return {};
    res = pattern.matcher->search(view, regex_options);
    return res.success;
}

template<class Parser>
bool has_match(const StringView view, Regex<Parser>& pattern, Optional<typename ParserTraits<Parser>::OptionsType> regex_options = {})
{
    if (pattern.matcher == nullptr)
        return {};
    RegexResult result = pattern.matcher->match(view, regex_options.value_or({}) | AllFlags::NoSubExpressions);
    return result.success;
}
}
}

using AK::regex::has_match;
using AK::regex::match;
using AK::regex::Regex;
using AK::regex::RegexResult;
