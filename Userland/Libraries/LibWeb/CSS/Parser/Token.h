/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <AK/String.h>
#include <math.h>

namespace Web::CSS {

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

    enum class NumberType {
        Integer,
        Number,
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

    StringView delim() const
    {
        VERIFY(m_type == Type::Delim);
        return m_value.view();
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

    bool is(NumberType number_type) const { return is(Token::Type::Number) && m_number_type == number_type; }
    StringView number_string_value() const
    {
        VERIFY(m_type == Type::Number);
        return m_value.view();
    }
    double number_value() const
    {
        VERIFY(m_type == Type::Number);
        return m_number_value;
    }
    i64 to_integer() const
    {
        VERIFY(m_type == Type::Number && m_number_type == NumberType::Integer);
        return to_closest_integer(m_number_value);
    }
    bool is_integer_value_signed() const { return number_string_value().starts_with('-') || number_string_value().starts_with('+'); }

    StringView dimension_unit() const
    {
        VERIFY(m_type == Type::Dimension);
        return m_unit.view();
    }
    double dimension_value() const
    {
        VERIFY(m_type == Type::Dimension);
        return m_number_value;
    }
    i64 dimension_value_int() const { return to_closest_integer(dimension_value()); }

    double percentage() const
    {
        VERIFY(m_type == Type::Percentage);
        return m_number_value;
    }

    NumberType number_type() const
    {
        VERIFY((m_type == Type::Number) || (m_type == Type::Dimension) || (m_type == Type::Percentage));
        return m_number_type;
    }

    Type mirror_variant() const;
    String bracket_string() const;
    String bracket_mirror_string() const;

    String to_string() const;
    String to_debug_string() const;

    Position const& start_position() const { return m_start_position; }
    Position const& end_position() const { return m_end_position; }

private:
    static i64 to_closest_integer(double value)
    {
        // https://www.w3.org/TR/css-values-4/#numeric-types
        // When a value cannot be explicitly supported due to range/precision limitations, it must be converted
        // to the closest value supported by the implementation, but how the implementation defines "closest"
        // is explicitly undefined as well.
        return static_cast<i64>(clamp(round(value), NumericLimits<i64>::min(), NumericLimits<i64>::max()));
    }

    Type m_type { Type::Invalid };

    FlyString m_value;
    FlyString m_unit;
    HashType m_hash_type { HashType::Unrestricted };
    NumberType m_number_type { NumberType::Integer };
    double m_number_value { 0 };

    Position m_start_position;
    Position m_end_position;
};

}
