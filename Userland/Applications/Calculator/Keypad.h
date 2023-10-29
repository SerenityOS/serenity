/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <LibCrypto/BigFraction/BigFraction.h>
#include <LibCrypto/BigInt/UnsignedBigInteger.h>

// This type implements number typing and
// displaying mechanics. It does not perform
// any arithmetic operations or anything on
// the values it deals with.

class Keypad final {
public:
    Keypad() = default;
    ~Keypad() = default;

    void type_digit(int digit);
    void type_decimal_point();
    void type_backspace();

    Crypto::BigFraction value() const;
    void set_value(Crypto::BigFraction);
    void set_typed_value(Crypto::BigFraction);
    void set_to_0();

    void shrink(unsigned);
    void set_rounding_length(unsigned);
    unsigned rounding_length() const;

    ErrorOr<String> to_string() const;

    bool in_typing_state() const;

private:
    // Internal representation of the current decimal value.
    // Those variables are only used when the user is entering a value.
    // Otherwise, the BigFraction m_internal_value is used.
    Crypto::UnsignedBigInteger m_int_value { 0 };
    Crypto::UnsignedBigInteger m_frac_value { 0 };
    Crypto::UnsignedBigInteger m_frac_length { 0 };
    // E.g. for -35.004200,
    // m_negative = true
    // m_int_value = 35
    // m_frac_value = 4200
    // m_frac_length = 6

    mutable Crypto::BigFraction m_internal_value {};

    unsigned m_displayed_fraction_length { 0 };

    enum class State {
        External,
        TypedExternal,
        TypingInteger,
        TypingDecimal
    };

    State m_state { State::External };
};
