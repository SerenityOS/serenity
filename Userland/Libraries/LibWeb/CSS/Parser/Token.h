/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
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

    StringView url() const
    {
        VERIFY(m_type == Type::Url);
        return m_value.string_view();
    }

    StringView at_keyword() const
    {
        VERIFY(m_type == Type::AtKeyword);
        return m_value.string_view();
    }

    HashType hash_type() const
    {
        VERIFY(m_type == Type::Hash);
        return m_hash_type;
    }
    StringView hash_value() const
    {
        VERIFY(m_type == Type::Hash);
        return m_value.string_view();
    }

    bool is(NumberType number_type) const { return is(Token::Type::Number) && m_number_type == number_type; }
    StringView number_string_value() const
    {
        VERIFY(m_type == Type::Number);
        return m_value.string_view();
    }
    int to_integer() const
    {
        VERIFY(m_type == Type::Number && m_number_type == NumberType::Integer);
        return number_string_value().to_int().value();
    }
    bool is_integer_value_signed() const { return number_string_value().starts_with('-') || number_string_value().starts_with('+'); }

    StringView dimension_unit() const
    {
        VERIFY(m_type == Type::Dimension);
        return m_unit.string_view();
    }
    StringView dimension_value() const
    {
        VERIFY(m_type == Type::Dimension);
        return m_value.string_view();
    }
    int dimension_value_int() const { return dimension_value().to_int().value(); }

    NumberType number_type() const
    {
        VERIFY((m_type == Type::Number) || (m_type == Type::Dimension));
        return m_number_type;
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
