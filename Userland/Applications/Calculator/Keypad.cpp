/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2021, Max Wipfli <mail@maxwipfli.ch>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Keypad.h"
#include "KeypadValue.h"
#include <AK/Math.h>
#include <AK/StringBuilder.h>

Keypad::Keypad()
{
}

Keypad::~Keypad()
{
}

void Keypad::type_digit(int digit)
{
    u64 previous_value = 0;
    switch (m_state) {
    case State::External:
        m_state = State::TypingInteger;
        m_negative = false;
        m_int_value = digit;
        m_frac_value = 0;
        m_frac_length = 0;
        break;
    case State::TypingInteger:
        VERIFY(m_frac_value.value() == 0);
        VERIFY(m_frac_length == 0);
        previous_value = m_int_value.value();
        m_int_value *= 10;
        m_int_value += digit;
        if (m_int_value.has_overflow())
            m_int_value = previous_value;
        break;
    case State::TypingDecimal:
        previous_value = m_frac_value.value();
        m_frac_value *= 10;
        m_frac_value += digit;
        if (m_frac_value.has_overflow())
            m_frac_value = previous_value;
        else
            m_frac_length++;
        break;
    }
}

void Keypad::type_decimal_point()
{
    switch (m_state) {
    case State::External:
        m_negative = false;
        m_int_value = 0;
        m_frac_value = 0;
        m_frac_length = 0;
        m_state = State::TypingDecimal;
        break;
    case State::TypingInteger:
        VERIFY(m_frac_value.value() == 0);
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
        m_int_value = 0;
        m_frac_value = 0;
        m_frac_length = 0;
        break;
    case State::TypingDecimal:
        if (m_frac_length > 0) {
            m_frac_value /= 10;
            m_frac_length--;
            break;
        }
        VERIFY(m_frac_value.value() == 0);
        m_state = State::TypingInteger;
        [[fallthrough]];
    case State::TypingInteger:
        VERIFY(m_frac_value.value() == 0);
        VERIFY(m_frac_length == 0);
        m_int_value /= 10;
        if (m_int_value.value() == 0)
            m_negative = false;
        break;
    }
}

KeypadValue Keypad::value() const
{
    KeypadValue frac_part = { (i64)m_frac_value.value(), m_frac_length };
    KeypadValue int_part = { (i64)m_int_value.value() };
    KeypadValue res = int_part + frac_part;
    if (m_negative)
        res = -res;
    return res;
}

void Keypad::set_value(KeypadValue value)
{
    m_state = State::External;

    if (value.m_value < 0) {
        m_negative = true;
        value = -value;
    } else
        m_negative = false;

    m_int_value = value.m_value / (u64)AK::pow(10.0, (double)value.m_decimal_places);
    m_frac_value = value.m_value % (u64)AK::pow(10.0, (double)value.m_decimal_places);
    m_frac_length = value.m_decimal_places;
}

String Keypad::to_string() const
{
    StringBuilder builder;
    if (m_negative)
        builder.append("-");
    builder.appendff("{}", m_int_value.value());

    // NOTE: This is so the decimal point appears on screen as soon as you type it.
    if (m_frac_length > 0 || m_state == State::TypingDecimal)
        builder.append('.');

    if (m_frac_length > 0) {
        // FIXME: This disables the compiletime format string check since we can't parse '}}' here correctly.
        //        remove the 'StringView { }' when that's fixed.
        builder.appendff("{:0{}}"sv, m_frac_value.value(), m_frac_length);
    }

    return builder.to_string();
}
