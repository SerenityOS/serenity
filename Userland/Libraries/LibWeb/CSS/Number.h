/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <math.h>

namespace Web::CSS {

class Number {
public:
    enum class Type {
        Number,
        IntegerWithExplicitSign, // This only exists for the nightmarish An+B parsing algorithm
        Integer
    };

    Number()
        : m_value(0)
        , m_type(Type::Number)
    {
    }
    Number(Type type, float value)
        : m_value(value)
        , m_type(type)
    {
    }

    float value() const { return m_value; }
    i64 integer_value() const
    {
        // https://www.w3.org/TR/css-values-4/#numeric-types
        // When a value cannot be explicitly supported due to range/precision limitations, it must be converted
        // to the closest value supported by the implementation, but how the implementation defines "closest"
        // is explicitly undefined as well.
        return llroundf(m_value);
    }
    bool is_integer() const { return m_type == Type::Integer || m_type == Type::IntegerWithExplicitSign; }
    bool is_integer_with_explicit_sign() const { return m_type == Type::IntegerWithExplicitSign; }

    Number operator+(Number const& other) const
    {
        if (is_integer() && other.is_integer())
            return { Type::Integer, m_value + other.m_value };
        return { Type::Number, m_value + other.m_value };
    }

    Number operator-(Number const& other) const
    {
        if (is_integer() && other.is_integer())
            return { Type::Integer, m_value - other.m_value };
        return { Type::Number, m_value - other.m_value };
    }

    Number operator*(Number const& other) const
    {
        if (is_integer() && other.is_integer())
            return { Type::Integer, m_value * other.m_value };
        return { Type::Number, m_value * other.m_value };
    }

    Number operator/(Number const& other) const
    {
        return { Type::Number, m_value / other.m_value };
    }

private:
    float m_value { 0 };
    Type m_type;
};
}
