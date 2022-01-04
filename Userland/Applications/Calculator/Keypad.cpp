/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2021, Max Wipfli <mail@maxwipfli.ch>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Keypad.h"
#include "KeypadValue.h"
#include <AK/StringBuilder.h>
#include <LibCrypto/NumberTheory/ModularFunctions.h>

void Keypad::type_digit(int digit)
{
    switch (m_state) {
    case State::External:
        m_state = State::TypingInteger;
        m_negative = false;
        m_int_value = digit;
        m_frac_value.set_to_0();
        m_frac_length.set_to_0();
        break;
    case State::TypingInteger:
        VERIFY(m_frac_value == 0);
        VERIFY(m_frac_length == 0);
        m_int_value.set_to(m_int_value.multiplied_by(10));
        m_int_value.set_to(m_int_value.plus(digit));
        break;
    case State::TypingDecimal:
        m_frac_value.set_to(m_frac_value.multiplied_by(10));
        m_frac_value.set_to(m_frac_value.plus(digit));

        m_frac_length.set_to(m_frac_length.plus(1));
        break;
    }
}

void Keypad::type_decimal_point()
{
    switch (m_state) {
    case State::External:
        m_negative = false;
        m_int_value.set_to_0();
        m_frac_value.set_to_0();
        m_frac_length.set_to_0();
        m_state = State::TypingDecimal;
        break;
    case State::TypingInteger:
        VERIFY(m_frac_value == 0);
        VERIFY(m_frac_length == 0);
        m_state = State::TypingDecimal;
        break;
    case State::TypingDecimal:
        // Ignore it.
        break;
    }
}

void Keypad::type_backspace()
{
    switch (m_state) {
    case State::External:
        m_negative = false;
        m_int_value.set_to_0();
        m_frac_value.set_to_0();
        m_frac_length.set_to_0();
        break;
    case State::TypingDecimal:
        if (m_frac_length > 0) {
            m_frac_value.set_to(m_frac_value.divided_by(10).quotient);
            m_frac_length.set_to(m_frac_length.minus(1));
            break;
        }
        VERIFY(m_frac_value == 0);
        m_state = State::TypingInteger;
        [[fallthrough]];
    case State::TypingInteger:
        VERIFY(m_frac_value == 0);
        VERIFY(m_frac_length == 0);
        m_int_value.set_to(m_int_value.divided_by(10).quotient);
        if (m_int_value == 0)
            m_negative = false;
        break;
    }
}

KeypadValue Keypad::value() const
{
    KeypadValue frac_part = { Crypto::SignedBigInteger { m_frac_value }, m_frac_length };
    KeypadValue int_part = { Crypto::SignedBigInteger { m_int_value } };
    KeypadValue res = int_part + frac_part;
    if (m_negative)
        res = -res;
    return res;
}

void Keypad::set_value(KeypadValue value)
{
    m_state = State::External;

    if (value.m_value < Crypto::SignedBigInteger { 0 }) {
        m_negative = true;
        value = -value;
    } else
        m_negative = false;

    auto const res = value.m_value.divided_by(Crypto::NumberTheory::Power(Crypto::UnsignedBigInteger { 10 }, value.m_decimal_places));

    m_int_value = res.quotient.unsigned_value();
    m_frac_value = res.remainder.unsigned_value();
    m_frac_length = value.m_decimal_places;

    // A multiple of 10 will just lead to display useless zero after the comma.
    for (auto division_result = res.remainder.divided_by(Crypto::UnsignedBigInteger { 10 });
         division_result.remainder == Crypto::UnsignedBigInteger { 0 } && division_result.quotient != Crypto::UnsignedBigInteger { 0 };
         division_result = division_result.quotient.divided_by(Crypto::UnsignedBigInteger { 10 })) {
        m_frac_value = division_result.quotient.unsigned_value();
        m_frac_length.set_to(m_frac_length.minus(1));
    }

    if (m_frac_value == 0)
        m_frac_length.set_to_0();
}

void Keypad::set_to_0()
{
    m_int_value.set_to_0();
    m_frac_value.set_to_0();
    m_frac_length.set_to_0();
    m_state = State::External;
}

String Keypad::to_string() const
{
    StringBuilder builder;
    if (m_negative)
        builder.append("-");
    builder.appendff("{}", m_int_value.to_base(10));

    // NOTE: This is so the decimal point appears on screen as soon as you type it.
    if (m_frac_length > 0 || m_state == State::TypingDecimal)
        builder.append('.');

    auto const frac_value_str = m_frac_value.to_base(10);
    if (m_frac_length > 0) {
        builder.append_repeated('0', m_frac_length.to_u64() - frac_value_str.length());
        builder.appendff("{}"sv, frac_value_str);
    }

    return builder.to_string();
}
