/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "KeypadValue.h"
#include <AK/IntegralMath.h>
#include <AK/Math.h>
#include <AK/String.h>
#include <LibCrypto/NumberTheory/ModularFunctions.h>

KeypadValue::KeypadValue(Crypto::SignedBigInteger value, Crypto::UnsignedBigInteger decimal_places)
    : m_value(move(value))
    , m_decimal_places(m_value == Crypto::SignedBigInteger { 0 } ? 0 : move(decimal_places))
{
}

KeypadValue::KeypadValue(Crypto::SignedBigInteger value)
    : m_value(move(value))
{
}

KeypadValue::KeypadValue(StringView sv)
{
    String str = sv.to_string(); // TODO: Once we have a StringView equivalent for this C API, we won't need to create a copy for this anymore.
    size_t first_index = 0;
    char* dot_ptr;
    auto const int_part = Crypto::SignedBigInteger::create_from(strtoll(&str[first_index], &dot_ptr, 10));
    size_t dot_index = dot_ptr - str.characters();
    if ((dot_index < str.length()) && (str[dot_index] == '.')) {
        size_t second_index = dot_index + 1;
        char* end_ptr;
        auto const frac_part = Crypto::SignedBigInteger::create_from(strtoll(&str[second_index], &end_ptr, 10));
        size_t end_index = end_ptr - str.characters();
        auto const frac_length = Crypto::UnsignedBigInteger::create_from(end_index - second_index);
        *this = KeypadValue { int_part } + KeypadValue { frac_part, frac_length };
    } else {
        *this = KeypadValue { int_part };
    }
};

KeypadValue KeypadValue::operator+(KeypadValue const& rhs) const
{
    return operator_helper<KeypadValue>(*this, rhs, [](KeypadValue const&, KeypadValue const& more_decimal_places, Crypto::SignedBigInteger const& less_decimal_places_equalized, Crypto::SignedBigInteger const& more_decimal_places_equalized, bool) -> KeypadValue {
        return {
            more_decimal_places_equalized.plus(less_decimal_places_equalized),
            more_decimal_places.m_decimal_places
        };
    });
}

KeypadValue KeypadValue::operator-(KeypadValue const& rhs) const
{
    return *this + (-rhs);
}

KeypadValue KeypadValue::operator*(KeypadValue const& rhs) const
{
    return operator_helper<KeypadValue>(*this, rhs, [](KeypadValue const& less_decimal_places, KeypadValue const& more_decimal_places, Crypto::SignedBigInteger const&, Crypto::SignedBigInteger const&, bool) -> KeypadValue {
        return {
            less_decimal_places.m_value.multiplied_by(more_decimal_places.m_value),
            less_decimal_places.m_decimal_places.plus(more_decimal_places.m_decimal_places)
        };
    });
}

KeypadValue KeypadValue::operator-(void) const
{
    return { m_value.negated_value(), m_decimal_places };
}

KeypadValue KeypadValue::sqrt(void) const
{
    return KeypadValue { AK::sqrt((double)(*this)) };
}

KeypadValue KeypadValue::invert(void) const
{
    return KeypadValue { 1.0 / (double)(*this) };
}

KeypadValue KeypadValue::operator/(KeypadValue const& rhs) const
{
    return KeypadValue { (double)(*this) / (double)rhs };
}

bool KeypadValue::operator<(KeypadValue const& rhs) const
{
    return operator_helper<bool>(*this, rhs, [](KeypadValue const&, KeypadValue const&, Crypto::SignedBigInteger const& less_decimal_places_equalized, Crypto::SignedBigInteger const& more_decimal_places_equalized, bool lhs_is_less) {
        if (lhs_is_less)
            return (less_decimal_places_equalized < more_decimal_places_equalized);
        else
            return (more_decimal_places_equalized < less_decimal_places_equalized);
    });
}

bool KeypadValue::operator==(KeypadValue const& rhs) const
{
    return operator_helper<bool>(*this, rhs, [](KeypadValue const&, KeypadValue const&, Crypto::SignedBigInteger const& less_decimal_places_equalized, Crypto::SignedBigInteger const& more_decimal_places_equalized, bool) {
        return less_decimal_places_equalized == more_decimal_places_equalized;
    });
}

// This is a helper function for the operators. A lot of them need to do very similar calculations, so this function
// does the calculations for them and calls them on the result. In case they don't need the result of a particular
// calculation, they simply ignore that argument.
// The arguments to this function are the operands on the left- and right-hand sides and the callback to call on the
// values computed by this function.
// The first two KeypadValues it passes to the callback are the two original operands, but sorted by the amount of
// decimal places.
// The next two i64s it passes to the callback are these sorted KeypadValues, but normalized, which means that if
// you have for example 12.1 (represented as {121, 1}) and 54.23 (represented as {5423, 2}), you will get 1210 and
// 5423, so that you can compare these two i64s directly in order to compare the original KeypadValues.
// Unfortunately, not all operators are symmetric, so the last boolean tells the callback whether the left-hand side
// was the KeypadValue with less decimal places (true), or the one with more decimal places (false).
template<typename T, typename F>
ALWAYS_INLINE T KeypadValue::operator_helper(KeypadValue const& lhs, KeypadValue const& rhs, F callback) const
{
    KeypadValue const& less_decimal_places = (lhs.m_decimal_places < rhs.m_decimal_places) ? lhs : rhs;
    KeypadValue const& more_decimal_places = (lhs.m_decimal_places < rhs.m_decimal_places) ? rhs : lhs;

    Crypto::SignedBigInteger more_decimal_places_equalized = more_decimal_places.m_value;
    Crypto::SignedBigInteger less_decimal_places_equalized = less_decimal_places.m_value.multiplied_by(
        Crypto::NumberTheory::Power(Crypto::UnsignedBigInteger { 10 },
            more_decimal_places.m_decimal_places.minus(less_decimal_places.m_decimal_places)));

    bool lhs_is_less = (lhs.m_decimal_places < rhs.m_decimal_places);

    return callback(less_decimal_places, more_decimal_places,
        less_decimal_places_equalized, more_decimal_places_equalized,
        lhs_is_less);
}

KeypadValue::KeypadValue(double d)
{
    bool negative = false;
    if (d < 0) {
        negative = true;
        d = -d;
    }
    i8 current_pow = 0;
    while (AK::pow(10.0, (double)current_pow) <= d)
        current_pow += 1;
    current_pow -= 1;
    double epsilon = 1e-6;
    while (d >= epsilon || current_pow >= 0) {
        m_value.set_to(m_value.multiplied_by(Crypto::SignedBigInteger { 10 }));
        i8 digit = (u64)(d * AK::pow(0.1, (double)current_pow)) % 10;
        m_value.set_to(m_value.plus(Crypto::UnsignedBigInteger::create_from(digit)));
        d -= digit * AK::pow(10.0, (double)current_pow);
        if (current_pow < 0)
            m_decimal_places.set_to(m_decimal_places.plus(Crypto::UnsignedBigInteger { 1 }));
        current_pow -= 1;
        if (m_decimal_places > 6)
            break;
    }
    m_value.set_to(negative ? (m_value.negated_value()) : m_value);
}

KeypadValue::operator double() const
{
    auto const divisor = Crypto::NumberTheory::Power(Crypto::UnsignedBigInteger { 10 }, m_decimal_places);
    auto const res = m_value.divided_by(divisor);
    return res.quotient.to_double() + res.remainder.to_double() / divisor.to_double();
}

void KeypadValue::set_to_0()
{
    m_value.set_to_0();
    m_decimal_places.set_to_0();
}
