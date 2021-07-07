/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 * Copyright (c) 2021, Sam Atkins <atkinssj@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/StringBuilder.h>

namespace Web::CSS {

class Token {
    friend class Parser;
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

    enum class NumberType {
        Integer,
        Number,
    };

    bool is(Type type) const { return m_type == type; }

    StringView ident() const
    {
        VERIFY(m_type == Type::Ident);
        return m_value.string_view();
    }

    StringView delim() const
    {
        VERIFY(m_type == Type::Delim);
        return m_value.string_view();
    }

    StringView string() const
    {
        VERIFY(m_type == Type::String);
        return m_value.string_view();
    }

    int integer() const
    {
        VERIFY(m_type == Type::Number && m_number_type == NumberType::Integer);
        return m_value.string_view().to_int().value();
    }

    Type mirror_variant() const;
    String bracket_string() const;
    String bracket_mirror_string() const;

    String to_debug_string() const;

private:
    Type m_type { Type::Invalid };

    StringBuilder m_value;
    StringBuilder m_unit;
    HashType m_hash_type { HashType::Unrestricted };
    NumberType m_number_type { NumberType::Integer };
};

}
