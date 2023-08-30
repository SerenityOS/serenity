/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/MathematicalValue.h>
#include <math.h>

namespace JS::Intl {

bool MathematicalValue::is_number() const
{
    return m_value.has<double>();
}

double MathematicalValue::as_number() const
{
    VERIFY(is_number());
    return m_value.get<double>();
}

bool MathematicalValue::is_bigint() const
{
    return m_value.has<Crypto::SignedBigInteger>();
}

Crypto::SignedBigInteger const& MathematicalValue::as_bigint() const
{
    VERIFY(is_bigint());
    return m_value.get<Crypto::SignedBigInteger>();
}

bool MathematicalValue::is_mathematical_value() const
{
    return is_number() || is_bigint();
}

bool MathematicalValue::is_positive_infinity() const
{
    if (is_mathematical_value())
        return false;
    return m_value.get<Symbol>() == Symbol::PositiveInfinity;
}

bool MathematicalValue::is_negative_infinity() const
{
    if (is_mathematical_value())
        return false;
    return m_value.get<Symbol>() == Symbol::NegativeInfinity;
}

bool MathematicalValue::is_negative_zero() const
{
    if (is_mathematical_value())
        return false;
    return m_value.get<Symbol>() == Symbol::NegativeZero;
}

bool MathematicalValue::is_nan() const
{
    if (is_mathematical_value())
        return false;
    return m_value.get<Symbol>() == Symbol::NotANumber;
}

void MathematicalValue::negate()
{
    m_value.visit(
        [](double& value) {
            VERIFY(value != 0.0);
            value *= -1.0;
        },
        [](Crypto::SignedBigInteger& value) { value.negate(); },
        [](auto) { VERIFY_NOT_REACHED(); });
}

MathematicalValue MathematicalValue::plus(Checked<i32> addition) const
{
    return m_value.visit(
        [&](double value) {
            return MathematicalValue { value + addition.value() };
        },
        [&](Crypto::SignedBigInteger const& value) {
            return MathematicalValue { value.plus(Crypto::SignedBigInteger { addition.value() }) };
        },
        [](auto) -> MathematicalValue { VERIFY_NOT_REACHED(); });
}

MathematicalValue MathematicalValue::plus(MathematicalValue const& addition) const
{
    return m_value.visit(
        [&](double value) {
            return MathematicalValue { value + addition.as_number() };
        },
        [&](Crypto::SignedBigInteger const& value) {
            return MathematicalValue { value.plus(addition.as_bigint()) };
        },
        [](auto) -> MathematicalValue { VERIFY_NOT_REACHED(); });
}

MathematicalValue MathematicalValue::minus(Checked<i32> subtraction) const
{
    return m_value.visit(
        [&](double value) {
            return MathematicalValue { value - subtraction.value() };
        },
        [&](Crypto::SignedBigInteger const& value) {
            return MathematicalValue { value.minus(Crypto::SignedBigInteger { subtraction.value() }) };
        },
        [](auto) -> MathematicalValue { VERIFY_NOT_REACHED(); });
}

MathematicalValue MathematicalValue::minus(MathematicalValue const& subtraction) const
{
    return m_value.visit(
        [&](double value) {
            return MathematicalValue { value - subtraction.as_number() };
        },
        [&](Crypto::SignedBigInteger const& value) {
            return MathematicalValue { value.minus(subtraction.as_bigint()) };
        },
        [](auto) -> MathematicalValue { VERIFY_NOT_REACHED(); });
}

MathematicalValue MathematicalValue::multiplied_by(Checked<i32> multiplier) const
{
    return m_value.visit(
        [&](double value) {
            return MathematicalValue { value * multiplier.value() };
        },
        [&](Crypto::SignedBigInteger const& value) {
            return MathematicalValue { value.multiplied_by(Crypto::SignedBigInteger { multiplier.value() }) };
        },
        [](auto) -> MathematicalValue { VERIFY_NOT_REACHED(); });
}

MathematicalValue MathematicalValue::multiplied_by(MathematicalValue const& multiplier) const
{
    return m_value.visit(
        [&](double value) {
            return MathematicalValue { value * multiplier.as_number() };
        },
        [&](Crypto::SignedBigInteger const& value) {
            return MathematicalValue { value.multiplied_by(multiplier.as_bigint()) };
        },
        [](auto) -> MathematicalValue { VERIFY_NOT_REACHED(); });
}

MathematicalValue MathematicalValue::divided_by(Checked<i32> divisor) const
{
    return m_value.visit(
        [&](double value) {
            return MathematicalValue { value / divisor.value() };
        },
        [&](Crypto::SignedBigInteger const& value) {
            return MathematicalValue { value.divided_by(Crypto::SignedBigInteger { divisor.value() }).quotient };
        },
        [](auto) -> MathematicalValue { VERIFY_NOT_REACHED(); });
}

MathematicalValue MathematicalValue::divided_by(MathematicalValue const& divisor) const
{
    return m_value.visit(
        [&](double value) {
            return MathematicalValue { value / divisor.as_number() };
        },
        [&](Crypto::SignedBigInteger const& value) {
            return MathematicalValue { value.divided_by(divisor.as_bigint()).quotient };
        },
        [](auto) -> MathematicalValue { VERIFY_NOT_REACHED(); });
}

static Crypto::SignedBigInteger bigint_power(Checked<i32> exponent)
{
    VERIFY(exponent >= 0);

    static auto base = Crypto::SignedBigInteger { 10 };
    auto result = Crypto::SignedBigInteger { 1 };

    for (i32 i = 0; i < exponent; ++i)
        result = result.multiplied_by(base);

    return result;
}

MathematicalValue MathematicalValue::multiplied_by_power(Checked<i32> exponent) const
{
    return m_value.visit(
        [&](double value) {
            return MathematicalValue { value * pow(10, exponent.value()) };
        },
        [&](Crypto::SignedBigInteger const& value) {
            if (exponent < 0)
                return MathematicalValue { value.divided_by(bigint_power(-exponent.value())).quotient };
            return MathematicalValue { value.multiplied_by(bigint_power(exponent)) };
        },
        [](auto) -> MathematicalValue { VERIFY_NOT_REACHED(); });
}

MathematicalValue MathematicalValue::divided_by_power(Checked<i32> exponent) const
{
    return m_value.visit(
        [&](double value) {
            if (exponent < 0)
                return MathematicalValue { value * pow(10, -exponent.value()) };
            return MathematicalValue { value / pow(10, exponent.value()) };
        },
        [&](Crypto::SignedBigInteger const& value) {
            if (exponent < 0)
                return MathematicalValue { value.multiplied_by(bigint_power(-exponent.value())) };
            return MathematicalValue { value.divided_by(bigint_power(exponent)).quotient };
        },
        [](auto) -> MathematicalValue { VERIFY_NOT_REACHED(); });
}

bool MathematicalValue::modulo_is_zero(Checked<i32> mod) const
{
    return m_value.visit(
        [&](double value) {
            auto result = MathematicalValue { modulo(value, mod.value()) };
            return result.is_equal_to(MathematicalValue { 0.0 });
        },
        [&](Crypto::SignedBigInteger const& value) {
            return modulo(value, Crypto::SignedBigInteger { mod.value() }).is_zero();
        },
        [](auto) -> bool { VERIFY_NOT_REACHED(); });
}

int MathematicalValue::logarithmic_floor() const
{
    return m_value.visit(
        [](double value) {
            return static_cast<int>(floor(log10(value)));
        },
        [&](Crypto::SignedBigInteger const& value) {
            // FIXME: Can we do this without string conversion?
            auto value_as_string = MUST(value.to_base(10));
            return static_cast<int>(value_as_string.bytes_as_string_view().length() - 1);
        },
        [](auto) -> int { VERIFY_NOT_REACHED(); });
}

bool MathematicalValue::is_equal_to(MathematicalValue const& other) const
{
    return m_value.visit(
        [&](double value) {
            static constexpr double epsilon = 5e-14;
            return fabs(value - other.as_number()) < epsilon;
        },
        [&](Crypto::SignedBigInteger const& value) {
            return value == other.as_bigint();
        },
        [](auto) -> bool { VERIFY_NOT_REACHED(); });
}

bool MathematicalValue::is_less_than(MathematicalValue const& other) const
{
    return m_value.visit(
        [&](double value) {
            if (is_equal_to(other))
                return false;
            return value < other.as_number();
        },
        [&](Crypto::SignedBigInteger const& value) {
            return value < other.as_bigint();
        },
        [](auto) -> bool { VERIFY_NOT_REACHED(); });
}

bool MathematicalValue::is_negative() const
{
    return m_value.visit(
        [](double value) { return value < 0.0; },
        [](Crypto::SignedBigInteger const& value) { return value.is_negative(); },
        [](Symbol symbol) { return symbol == Symbol::NegativeInfinity; });
}

bool MathematicalValue::is_positive() const
{
    return m_value.visit(
        [](double value) { return value > 0.0; },
        [](Crypto::SignedBigInteger const& value) { return !value.is_zero() && !value.is_negative(); },
        [](Symbol symbol) { return symbol == Symbol::PositiveInfinity; });
}

bool MathematicalValue::is_zero() const
{
    return m_value.visit(
        [&](double value) { return value == 0.0; },
        [](Crypto::SignedBigInteger const& value) { return value.is_zero(); },
        [](auto) { return false; });
}

String MathematicalValue::to_string() const
{
    return m_value.visit(
        [&](double value) {
            return number_to_string(value, NumberToStringMode::WithoutExponent);
        },
        [&](Crypto::SignedBigInteger const& value) {
            return MUST(value.to_base(10));
        },
        [&](auto) -> String { VERIFY_NOT_REACHED(); });
}

Value MathematicalValue::to_value(VM& vm) const
{
    return m_value.visit(
        [](double value) {
            return Value(value);
        },
        [&](Crypto::SignedBigInteger const& value) {
            return Value(BigInt::create(vm, value));
        },
        [](auto symbol) {
            switch (symbol) {
            case Symbol::PositiveInfinity:
                return js_infinity();
            case Symbol::NegativeInfinity:
                return js_negative_infinity();
            case Symbol::NegativeZero:
                return Value(-0.0);
            case Symbol::NotANumber:
                return js_nan();
            }

            VERIFY_NOT_REACHED();
        });
}

MathematicalValue::ValueType MathematicalValue::value_from_number(double number)
{
    Value value(number);

    if (value.is_positive_infinity())
        return Symbol::PositiveInfinity;
    if (value.is_negative_infinity())
        return Symbol::NegativeInfinity;
    if (value.is_negative_zero())
        return Symbol::NegativeZero;
    if (value.is_nan())
        return Symbol::NotANumber;
    return number;
}

}
