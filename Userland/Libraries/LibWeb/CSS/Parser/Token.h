/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <AK/String.h>
#include <AK/Utf8View.h>
#include <LibWeb/CSS/Number.h>

namespace Web::CSS::Parser {

class Token {
    friend class Tokenizer;

public:
    enum class Type {
        Invalid,
        EndOfFile,
        Ident,
        Function,
        AtKeyword,
        Hash,
        String,
        BadString,
        Url,
        BadUrl,
        Delim,
        Number,
        Percentage,
        Dimension,
        Whitespace,
        CDO,
        CDC,
        Colon,
        Semicolon,
        Comma,
        OpenSquare,
        CloseSquare,
        OpenParen,
        CloseParen,
        OpenCurly,
        CloseCurly
    };

    enum class HashType {
        Id,
        Unrestricted,
    };

    struct Position {
        size_t line { 0 };
        size_t column { 0 };
    };

    Type type() const { return m_type; }
    bool is(Type type) const { return m_type == type; }

    StringView ident() const
    {
        VERIFY(m_type == Type::Ident);
        return m_value.view();
    }

    StringView function() const
    {
        VERIFY(m_type == Type::Function);
        return m_value.view();
    }

    u32 delim() const
    {
        VERIFY(m_type == Type::Delim);
        return *Utf8View(m_value.view()).begin();
    }

    StringView string() const
    {
        VERIFY(m_type == Type::String);
        return m_value.view();
    }

    StringView url() const
    {
        VERIFY(m_type == Type::Url);
        return m_value.view();
    }

    StringView at_keyword() const
    {
        VERIFY(m_type == Type::AtKeyword);
        return m_value.view();
    }

    HashType hash_type() const
    {
        VERIFY(m_type == Type::Hash);
        return m_hash_type;
    }
    StringView hash_value() const
    {
        VERIFY(m_type == Type::Hash);
        return m_value.view();
    }

    Number const& number() const
    {
        VERIFY(m_type == Type::Number || m_type == Type::Dimension || m_type == Type::Percentage);
        return m_number_value;
    }
    float number_value() const
    {
        VERIFY(m_type == Type::Number);
        return m_number_value.value();
    }
    i64 to_integer() const
    {
        VERIFY(m_type == Type::Number && m_number_value.is_integer());
        return m_number_value.integer_value();
    }

    StringView dimension_unit() const
    {
        VERIFY(m_type == Type::Dimension);
        return m_value.view();
    }
    float dimension_value() const
    {
        VERIFY(m_type == Type::Dimension);
        return m_number_value.value();
    }
    i64 dimension_value_int() const { return m_number_value.integer_value(); }

    float percentage() const
    {
        VERIFY(m_type == Type::Percentage);
        return m_number_value.value();
    }

    Type mirror_variant() const;
    String bracket_string() const;
    String bracket_mirror_string() const;

    String to_string() const;
    String to_debug_string() const;

    Position const& start_position() const { return m_start_position; }
    Position const& end_position() const { return m_end_position; }

    static Token of_string(FlyString str)
    {
        Token token;
        token.m_type = Type::String;
        token.m_value = move(str);
        return token;
    }

private:
    Type m_type { Type::Invalid };

    FlyString m_value;
    Number m_number_value;
    HashType m_hash_type { HashType::Unrestricted };

    Position m_start_position;
    Position m_end_position;
};

}
