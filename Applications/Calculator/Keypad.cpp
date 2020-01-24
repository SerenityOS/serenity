/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "Keypad.h"
#include <AK/StringBuilder.h>
#include <math.h>

Keypad::Keypad()
{
}

Keypad::~Keypad()
{
}

void Keypad::type_digit(int digit)
{
    switch (m_state) {
    case State::External:
        m_state = State::TypingInteger;
        m_negative = false;
        m_int_value = digit;
        m_frac_value = 0;
        m_frac_length = 0;
        break;
    case State::TypingInteger:
        ASSERT(m_frac_value == 0);
        ASSERT(m_frac_length == 0);
        m_int_value *= 10;
        m_int_value += digit;
        break;
    case State::TypingDecimal:
        if (m_frac_length > 6)
            break;
        m_frac_value *= 10;
        m_frac_value += digit;
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
        break;
    case State::TypingInteger:
        ASSERT(m_frac_value == 0);
        ASSERT(m_frac_length == 0);
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
        ASSERT(m_frac_value == 0);
        m_state = State::TypingInteger;
        [[fallthrough]];
    case State::TypingInteger:
        ASSERT(m_frac_value == 0);
        ASSERT(m_frac_length == 0);
        m_int_value /= 10;
        if (m_int_value == 0)
            m_negative = false;
        break;
    }
}

double Keypad::value() const
{
    double res = 0.0;

    long frac = m_frac_value;
    for (int i = 0; i < m_frac_length; i++) {
        int digit = frac % 10;
        res += digit;
        res /= 10.0;
        frac /= 10;
    }

    res += m_int_value;
    if (m_negative)
        res = -res;

    return res;
}

void Keypad::set_value(double value)
{
    m_state = State::External;

    if (value < 0.0) {
        m_negative = true;
        value = -value;
    } else
        m_negative = false;

    m_int_value = value;
    value -= m_int_value;

    m_frac_value = 0;
    m_frac_length = 0;
    while (value != 0) {
        value *= 10.0;
        int digit = value;
        m_frac_value *= 10;
        m_frac_value += digit;
        m_frac_length++;
        value -= digit;

        if (m_frac_length > 6)
            break;
    }
}

String Keypad::to_string() const
{
    StringBuilder builder;
    if (m_negative)
        builder.append("-");
    builder.appendf("%ld.", m_int_value);

    if (m_frac_length > 0)
        builder.appendf("%0*ld", m_frac_length, m_frac_value);

    return builder.to_string();
}
