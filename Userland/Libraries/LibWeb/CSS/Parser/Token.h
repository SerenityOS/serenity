/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
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
    enum class TokenType {
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

    bool is_eof() const { return m_type == TokenType::EndOfFile; }
    bool is_whitespace() const { return m_type == TokenType::Whitespace; }
    bool is_cdo() const { return m_type == TokenType::CDO; }
    bool is_cdc() const { return m_type == TokenType::CDC; }
    bool is_at() const { return m_type == TokenType::AtKeyword; }
    bool is_semicolon() const { return m_type == TokenType::Semicolon; }
    bool is_open_curly() const { return m_type == TokenType::OpenCurly; }
    bool is_open_square() const { return m_type == TokenType::OpenSquare; }
    bool is_open_paren() const { return m_type == TokenType::OpenParen; }
    bool is_close_paren() const { return m_type == TokenType::CloseParen; }
    bool is_close_square() const { return m_type == TokenType::CloseSquare; }
    bool is_close_curly() const { return m_type == TokenType::CloseCurly; }
    bool is_function() const { return m_type == TokenType::Function; }
    bool is_colon() const { return m_type == TokenType::Colon; }
    bool is_ident() const { return m_type == TokenType::Ident; }
    bool is_delim() const { return m_type == TokenType::Delim; }
    bool is_comma() const { return m_type == TokenType::Comma; }

    TokenType mirror_variant() const;
    String bracket_string() const;
    String bracket_mirror_string() const;
    String to_string() const;

private:
    TokenType m_type { TokenType::Invalid };

    StringBuilder m_value;
    StringBuilder m_unit;
    HashType m_hash_type { HashType::Unrestricted };
    NumberType m_number_type { NumberType::Integer };
};

}
