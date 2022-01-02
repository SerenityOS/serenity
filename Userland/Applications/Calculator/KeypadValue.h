/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Math.h>
#include <AK/String.h>
#include <AK/Types.h>

// This function is just a temporary workaround until try_value actually return an ErrorOr
template<typename T>
ErrorOr<T> WIP_HELPER(T a)
{
    if (a == NumericLimits<T>::max())
        return Error::from_string_literal("Overflow Occurred");

    return a;
}

class KeypadValue {
    friend class Keypad;
    friend class Calculator;

public:
    KeypadValue(i64, u8);
    KeypadValue(Checked<i64>, Checked<u8>);
    KeypadValue(i64);

    explicit KeypadValue(StringView);

    KeypadValue operator+(KeypadValue const&);
    KeypadValue operator-(KeypadValue const&);
    KeypadValue operator*(KeypadValue const&);
    KeypadValue operator-(void) const;
    bool operator<(KeypadValue const&);
    bool operator==(KeypadValue const&);

    KeypadValue sqrt() const;
    KeypadValue invert() const;
    KeypadValue operator/(KeypadValue const&);

private:
    explicit KeypadValue(double);
    explicit operator double() const;

    template<typename T, typename F>
    T operator_helper(KeypadValue const& lhs, KeypadValue const& rhs, F callback);

    // This class represents a pair of a value together with the amount of decimal places that value is offset by.
    // For example, if we were to represent the value -123.55 in this format, m_value would be -12355 and
    // m_decimal_places would be 2, because when you shift -12355 2 digits to the right, you get -123.55.
    // This way, most operations don't have to be performed on doubles, but can be performed without loss of
    // precision on this class.
    Checked<i64> m_value { 0 };
    Checked<u8> m_decimal_places { 0 };
};
