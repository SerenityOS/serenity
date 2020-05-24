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

#include "AK/HashMap.h"
#include "AK/String.h"
#include "AK/StringView.h"
#include "AK/Vector.h"

namespace AK {
namespace regex {

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

struct MatchInput {
    StringView view { nullptr };
    AK::regex::AllOptions regex_options {};

    size_t match_index { 0 };
    size_t line { 0 };
    size_t column { 0 };
};

struct MatchState {
    size_t string_position { 0 };
    size_t instruction_position { 0 };
    size_t fork_at_position { 0 };
};

struct MatchOutput {
    size_t operations;
    Vector<Match> matches;
    Vector<Vector<Match>> capture_group_matches;
    Vector<HashMap<String, Match>> named_capture_group_matches;
};

}
}
