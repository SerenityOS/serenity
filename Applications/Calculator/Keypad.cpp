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
