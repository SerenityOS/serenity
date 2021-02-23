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

#include "AK/FlyString.h"
#include "AK/HashMap.h"
#include "AK/String.h"
#include "AK/StringBuilder.h"
#include "AK/StringView.h"
#include "AK/Utf32View.h"
#include "AK/Vector.h"

namespace regex {

class RegexStringView {
public:
    RegexStringView(const char* chars)
        : m_u8view(chars)
    {
    }

    RegexStringView(const String& string)
        : m_u8view(string)
    {
    }

    RegexStringView(const StringView view)
        : m_u8view(view)
    {
    }
    RegexStringView(const Utf32View view)
        : m_u32view(view)
    {
    }

    bool is_u8_view() const { return m_u8view.has_value(); }
    bool is_u32_view() const { return m_u32view.has_value(); }

    const StringView& u8view() const
    {
        VERIFY(m_u8view.has_value());
        return m_u8view.value();
    };

    const Utf32View& u32view() const
    {
        VERIFY(m_u32view.has_value());
        return m_u32view.value();
    };

    bool is_empty() const
    {
        if (is_u8_view())
            return m_u8view.value().is_empty();
        else
            return m_u32view.value().is_empty();
    }

    bool is_null() const
    {
        if (is_u8_view())
            return m_u8view.value().is_null();
        else
            return m_u32view.value().code_points() == nullptr;
    }

    size_t length() const
    {
        if (is_u8_view())
            return m_u8view.value().length();
        else
            return m_u32view.value().length();
    }

    Vector<RegexStringView> lines() const
    {
        if (is_u8_view()) {
            auto views = u8view().lines(false);
            Vector<RegexStringView> new_views;
            for (auto& view : views)
                new_views.append(move(view));
            return new_views;
        }

        // FIXME: line splitting for Utf32View needed
        Vector<RegexStringView> views;
        views.append(m_u32view.value());
        return views;
    }

    RegexStringView substring_view(size_t offset, size_t length) const
    {
        if (is_u8_view()) {
            return u8view().substring_view(offset, length);
        }
        return u32view().substring_view(offset, length);
    }

    String to_string() const
    {
        if (is_u8_view()) {
            return u8view().to_string();
        }

        StringBuilder builder;
        builder.append(u32view());
        return builder.to_string();
    }

    u32 operator[](size_t index) const
    {
        if (is_u8_view()) {
            return u8view()[index];
        }
        return u32view().code_points()[index];
    }

    bool operator==(const char* cstring) const
    {
        if (is_u8_view())
            return u8view() == cstring;

        return to_string() == cstring;
    }

    bool operator!=(const char* cstring) const
    {
        return !(*this == cstring);
    }

    bool operator==(const String& string) const
    {
        if (is_u8_view())
            return u8view() == string;

        return to_string() == string;
    }

    bool operator==(const StringView& other) const
    {
        if (is_u8_view())
            return u8view() == other;

        return false;
    }

    bool operator!=(const StringView& other) const
    {
        return !(*this == other);
    }

    bool operator==(const Utf32View& other) const
    {
        if (is_u32_view()) {
            StringBuilder builder;
            builder.append(other);
            return to_string() == builder.to_string();
        }

        return false;
    }

    bool operator!=(const Utf32View& other) const
    {
        return !(*this == other);
    }

    const char* characters_without_null_termination() const
    {
        if (is_u8_view())
            return u8view().characters_without_null_termination();

        return to_string().characters(); // FIXME: it contains the null termination, does that actually matter?
    }

    bool starts_with(const StringView& str) const
    {
        if (is_u32_view())
            return false;
        return u8view().starts_with(str);
    }

    bool starts_with(const Utf32View& str) const
    {
        if (is_u8_view())
            return false;

        StringBuilder builder;
        builder.append(str);
        return to_string().starts_with(builder.to_string());
    }

private:
    Optional<StringView> m_u8view;
    Optional<Utf32View> m_u32view;
};

class Match final {
private:
    Optional<FlyString> string;

public:
    Match() = default;
    ~Match() = default;

    Match(const RegexStringView view_, const size_t line_, const size_t column_, const size_t global_offset_)
        : view(view_)
        , line(line_)
        , column(column_)
        , global_offset(global_offset_)
        , left_column(column_)
    {
    }

    Match(const String string_, const size_t line_, const size_t column_, const size_t global_offset_)
        : string(string_)
        , view(string.value().view())
        , line(line_)
        , column(column_)
        , global_offset(global_offset_)
        , left_column(column_)
    {
    }

    RegexStringView view { nullptr };
    size_t line { 0 };
    size_t column { 0 };
    size_t global_offset { 0 };

    // ugly, as not usable by user, but needed to prevent to create extra vectors that are
    // able to store the column when the left paren has been found
    size_t left_column { 0 };
};

struct MatchInput {
    RegexStringView view { nullptr };
    AllOptions regex_options {};
    size_t start_offset { 0 }; // For Stateful matches, saved and restored from Regex::start_offset.

    size_t match_index { 0 };
    size_t line { 0 };
    size_t column { 0 };

    size_t global_offset { 0 }; // For multiline matching, knowing the offset from start could be important

    mutable size_t fail_counter { 0 };
    mutable Vector<size_t> saved_positions;
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

using regex::RegexStringView;

template<>
struct AK::Formatter<regex::RegexStringView> : Formatter<StringView> {
    void format(FormatBuilder& builder, const regex::RegexStringView& value)
    {
        return Formatter<StringView>::format(builder, { value.characters_without_null_termination(), value.length() });
    }
};
