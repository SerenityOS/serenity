/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Math.h>
#include <AK/String.h>
#include <AK/Types.h>

class KeypadValue {
    friend class Keypad;

public:
    KeypadValue(i64, u8);
    KeypadValue(i64);

    KeypadValue operator+(KeypadValue const&);
    KeypadValue operator-(KeypadValue const&);
    KeypadValue operator*(KeypadValue const&);
    KeypadValue operator-(void) const;
    bool operator<(KeypadValue const&);
    bool operator>(KeypadValue const&);
    bool operator==(KeypadValue const&);

    explicit KeypadValue(double);
    explicit operator double();

private:
    template<typename T, typename F>
    T operator_helper(KeypadValue const& lhs, KeypadValue const& rhs, F callback);

    // This class represents a pair of a value together with the amount of decimal places that value is offset by.
    // For example, if we were to represent the value -123.55 in this format, m_value would be -12355 and
    // m_decimal_places would be 2, because when you shift -12355 2 digits to the right, you get -123.55.
    // This way, most operations don't have to be performed on doubles, but can be performed without loss of
    // precision on this class.
    i64 m_value { 0 };
    u8 m_decimal_places { 0 };
};
