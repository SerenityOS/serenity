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

    bool is_eof() const { return m_type == Type::EndOfFile; }
    bool is_ident() const { return m_type == Type::Ident; }
    bool is_function() const { return m_type == Type::Function; }
    bool is_at() const { return m_type == Type::AtKeyword; }
    bool is_hash() const { return m_type == Type::Hash; }
    bool is_string() const { return m_type == Type::String; }
    bool is_bad_string() const { return m_type == Type::BadString; }
    bool is_url() const { return m_type == Type::Url; }
    bool is_bad_url() const { return m_type == Type::BadUrl; }
    bool is_delim() const { return m_type == Type::Delim; }
    bool is_number() const { return m_type == Type::Number; }
    bool is_percentage() const { return m_type == Type::Percentage; }
    bool is_dimension() const { return m_type == Type::Dimension; }
    bool is_whitespace() const { return m_type == Type::Whitespace; }
    bool is_cdo() const { return m_type == Type::CDO; }
    bool is_cdc() const { return m_type == Type::CDC; }
    bool is_colon() const { return m_type == Type::Colon; }
    bool is_semicolon() const { return m_type == Type::Semicolon; }
    bool is_comma() const { return m_type == Type::Comma; }
    bool is_open_square() const { return m_type == Type::OpenSquare; }
    bool is_close_square() const { return m_type == Type::CloseSquare; }
    bool is_open_paren() const { return m_type == Type::OpenParen; }
    bool is_close_paren() const { return m_type == Type::CloseParen; }
    bool is_open_curly() const { return m_type == Type::OpenCurly; }
    bool is_close_curly() const { return m_type == Type::CloseCurly; }

    bool is(Type type) const { return m_type == type; }

    StringView ident() const
    {
        VERIFY(is_ident());
        return m_value.string_view();
    }

    StringView delim() const
    {
        VERIFY(is_delim());
        return m_value.string_view();
    }

    StringView string() const
    {
        VERIFY(is_string());
        return m_value.string_view();
    }

    Type mirror_variant() const;
    String bracket_string() const;
    String bracket_mirror_string() const;
    String to_string() const;

private:
    Type m_type { Type::Invalid };

    StringBuilder m_value;
    StringBuilder m_unit;
    HashType m_hash_type { HashType::Unrestricted };
    NumberType m_number_type { NumberType::Integer };
};

}
