/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "KeypadValue.h"
#include <AK/String.h>

// This type implements number typing and
// displaying mechanics. It does not perform
// any arithmetic operations or anything on
// the values it deals with.

class Keypad final {
public:
    Keypad();
    ~Keypad();

    void type_digit(int digit);
    void type_decimal_point();
    void type_backspace();

    KeypadValue value() const;
    void set_value(KeypadValue);

    String to_string() const;

private:
    // Internal representation of the current decimal value.
    bool m_negative { false };
    Checked<u64> m_int_value { 0 };
    Checked<u64> m_frac_value { 0 };
    u8 m_frac_length { 0 };
    // E.g. for -35.004200,
    // m_negative = true
    // m_int_value = 35
    // m_frac_value = 4200
    // m_frac_length = 6

    enum class State {
        External,
        TypingInteger,
        TypingDecimal
    };

    State m_state { State::External };
};
