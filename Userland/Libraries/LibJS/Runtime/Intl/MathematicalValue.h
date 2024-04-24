/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Checked.h>
#include <AK/String.h>
#include <AK/Variant.h>
#include <LibCrypto/BigInt/SignedBigInteger.h>
#include <LibJS/Runtime/BigInt.h>
#include <LibJS/Runtime/Value.h>

namespace JS::Intl {

// https://tc39.es/ecma402/#intl-mathematical-value
class MathematicalValue {
public:
    enum class Symbol {
        PositiveInfinity,
        NegativeInfinity,
        NegativeZero,
        NotANumber,
    };

    MathematicalValue() = default;

    explicit MathematicalValue(double value)
        : m_value(value_from_number(value))
    {
    }

    explicit MathematicalValue(Crypto::SignedBigInteger value)
        : m_value(move(value))
    {
    }

    explicit MathematicalValue(Symbol symbol)
        : m_value(symbol)
    {
    }

    MathematicalValue(Value value)
        : m_value(value.is_number()
                  ? value_from_number(value.as_double())
                  : ValueType(value.as_bigint().big_integer()))
    {
    }

    bool is_number() const;
    double as_number() const;

    bool is_bigint() const;
    Crypto::SignedBigInteger const& as_bigint() const;

    bool is_mathematical_value() const;
    bool is_positive_infinity() const;
    bool is_negative_infinity() const;
    bool is_negative_zero() const;
    bool is_nan() const;

    void negate();

    MathematicalValue plus(Checked<i32> addition) const;
    MathematicalValue plus(MathematicalValue const& addition) const;

    MathematicalValue minus(Checked<i32> subtraction) const;
    MathematicalValue minus(MathematicalValue const& subtraction) const;

    MathematicalValue multiplied_by(Checked<i32> multiplier) const;
    MathematicalValue multiplied_by(MathematicalValue const& multiplier) const;

    MathematicalValue divided_by(Checked<i32> divisor) const;
    MathematicalValue divided_by(MathematicalValue const& divisor) const;

    MathematicalValue multiplied_by_power(Checked<i32> exponent) const;
    MathematicalValue divided_by_power(Checked<i32> exponent) const;

    bool modulo_is_zero(Checked<i32> mod) const;

    int logarithmic_floor() const;

    bool is_equal_to(MathematicalValue const&) const;
    bool is_less_than(MathematicalValue const&) const;

    bool is_negative() const;
    bool is_positive() const;
    bool is_zero() const;

    String to_string() const;
    Value to_value(VM&) const;

private:
    using ValueType = Variant<double, Crypto::SignedBigInteger, Symbol>;

    static ValueType value_from_number(double number);

    ValueType m_value { 0.0 };
};

}
