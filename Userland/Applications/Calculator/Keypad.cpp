/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2021, Max Wipfli <mail@maxwipfli.ch>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Keypad.h"
#include <AK/StringBuilder.h>
#include <LibCrypto/BigFraction/BigFraction.h>
#include <LibCrypto/BigInt/UnsignedBigInteger.h>
#include <LibCrypto/NumberTheory/ModularFunctions.h>

void Keypad::type_digit(int digit)
{
    switch (m_state) {
    case State::External:
    case State::TypedExternal:
        m_state = State::TypingInteger;
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
    case State::TypedExternal:
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
    case State::TypedExternal:
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
        break;
    }
}

Crypto::BigFraction Keypad::value() const
{
    if (m_state != State::External && m_state != State::TypedExternal) {
        Crypto::SignedBigInteger sum { m_int_value.multiplied_by(Crypto::NumberTheory::Power("10"_bigint, m_frac_length)).plus(m_frac_value) };
        Crypto::BigFraction res { move(sum), Crypto::NumberTheory::Power("10"_bigint, m_frac_length) };

        m_internal_value = move(res);
    }

    return m_internal_value;
}

void Keypad::set_value(Crypto::BigFraction value)
{
    m_state = State::External;

    m_internal_value = move(value);
}

void Keypad::set_typed_value(Crypto::BigFraction value)
{
    m_state = State::TypedExternal;

    m_internal_value = move(value);
}

void Keypad::set_to_0()
{
    m_int_value.set_to_0();
    m_frac_value.set_to_0();
    m_frac_length.set_to_0();

    m_internal_value.set_to_0();

    m_state = State::External;
}

ErrorOr<String> Keypad::to_string() const
{
    if (m_state == State::External || m_state == State::TypedExternal)
        return String::from_byte_string(m_internal_value.to_byte_string(m_displayed_fraction_length));

    StringBuilder builder;

    ByteString const integer_value = m_int_value.to_base_deprecated(10);
    ByteString const frac_value = m_frac_value.to_base_deprecated(10);
    unsigned const number_pre_zeros = m_frac_length.to_u64() - (frac_value.length() - 1) - (frac_value == "0" ? 0 : 1);

    builder.append(integer_value);

    // NOTE: We test for the state so the decimal point appears on screen as soon as you type it.
    if (m_state == State::TypingDecimal) {
        builder.append('.');
        builder.append_repeated('0', number_pre_zeros);
        if (frac_value != "0")
            builder.append(frac_value);
    }

    return builder.to_string();
}

bool Keypad::in_typing_state() const
{
    return m_state == State::TypedExternal || m_state == State::TypingDecimal || m_state == State::TypingInteger;
}

void Keypad::set_rounding_length(unsigned rounding_threshold)
{
    m_displayed_fraction_length = rounding_threshold;
}

unsigned Keypad::rounding_length() const
{
    return m_displayed_fraction_length;
}

void Keypad::shrink(unsigned shrink_threshold)
{
    m_internal_value = m_internal_value.rounded(shrink_threshold);
}
