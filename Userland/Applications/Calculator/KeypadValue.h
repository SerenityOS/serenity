/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Math.h>
#include <AK/String.h>
#include <AK/Types.h>
#include <LibCrypto/BigInt/SignedBigInteger.h>

class KeypadValue {
    friend class Keypad;
    friend class Calculator;

public:
    KeypadValue() = default;
    KeypadValue(Crypto::SignedBigInteger);
    KeypadValue(Crypto::SignedBigInteger value, Crypto::UnsignedBigInteger decimal_places);

    explicit KeypadValue(StringView);

    KeypadValue operator+(KeypadValue const&) const;
    KeypadValue operator-(KeypadValue const&) const;
    KeypadValue operator*(KeypadValue const&) const;
    KeypadValue operator-(void) const;
    bool operator<(KeypadValue const&) const;
    bool operator==(KeypadValue const&) const;

    KeypadValue sqrt() const;
    KeypadValue invert() const;
    KeypadValue operator/(KeypadValue const&) const;

    void round(unsigned);

    void set_to_0();

private:
    explicit KeypadValue(double);
    explicit operator double() const;

    template<typename T, typename F>
    T operator_helper(KeypadValue const& lhs, KeypadValue const& rhs, F callback) const;

    // This class represents a pair of a value together with the amount of decimal places that value is offset by.
    // For example, if we were to represent the value -123.55 in this format, m_value would be -12355 and
    // m_decimal_places would be 2, because when you shift -12355 2 digits to the right, you get -123.55.
    // This way, most operations don't have to be performed on doubles, but can be performed without loss of
    // precision on this class.
    Crypto::SignedBigInteger m_value { 0 };
    Crypto::UnsignedBigInteger m_decimal_places { 0 };
};
