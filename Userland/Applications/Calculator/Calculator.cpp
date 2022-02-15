/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Calculator.h"
#include "KeypadValue.h"
#include <AK/Assertions.h>
#include <AK/Math.h>

KeypadValue Calculator::begin_operation(Operation operation, KeypadValue argument)
{
    KeypadValue res = 0;

    switch (operation) {
    case Operation::None:
        VERIFY_NOT_REACHED();

    case Operation::Add:
    case Operation::Subtract:
    case Operation::Multiply:
    case Operation::Divide:
        m_saved_argument = argument;
        m_operation_in_progress = operation;
        return argument;

    case Operation::Sqrt:
        if (argument < 0) {
            m_has_error = true;
            return argument;
        }
        res = argument.sqrt();
        clear_operation();
        break;
    case Operation::Inverse:
        if (argument == 0) {
            m_has_error = true;
            return argument;
        }
        res = argument.invert();
        clear_operation();
        break;
    case Operation::Percent:
        res = argument * KeypadValue { 1, 2 }; // also known as `KeypadValue{0.01}`
        break;
    case Operation::ToggleSign:
        res = -argument;
        break;

    case Operation::MemClear:
        m_mem = 0;
        res = argument;
        break;
    case Operation::MemRecall:
        res = m_mem;
        break;
    case Operation::MemSave:
        m_mem = argument;
        res = argument;
        break;
    case Operation::MemAdd:
        m_mem = m_mem + argument; // avoids the need for operator+=()
        res = m_mem;
        break;
    }

    if (should_be_rounded(res))
        round(res);

    return res;
}

KeypadValue Calculator::finish_operation(KeypadValue argument)
{
    KeypadValue res = 0;

    switch (m_operation_in_progress) {
    case Operation::None:
        return argument;

    case Operation::Add:
        res = m_saved_argument + argument;
        break;
    case Operation::Subtract:
        res = m_saved_argument - argument;
        break;
    case Operation::Multiply:
        res = m_saved_argument * argument;
        break;
    case Operation::Divide:
        if (argument == 0) {
            m_has_error = true;
            return argument;
        }
        res = m_saved_argument / argument;
        break;

    case Operation::Sqrt:
    case Operation::Inverse:
    case Operation::Percent:
    case Operation::ToggleSign:
    case Operation::MemClear:
    case Operation::MemRecall:
    case Operation::MemSave:
    case Operation::MemAdd:
        VERIFY_NOT_REACHED();
    }

    if (should_be_rounded(res))
        round(res);

    clear_operation();
    return res;
}

void Calculator::clear_operation()
{
    m_operation_in_progress = Operation::None;
    m_saved_argument = 0;
    clear_error();
}

bool Calculator::should_be_rounded(KeypadValue value)
{
    // We check if pow(10, value.m_decimal_places) overflow.
    // If it does, the value can't be displayed (and provoke a division by zero), see Keypad::set_value()
    // For u64, the threshold is 19
    return value.m_decimal_places > rounding_threshold;
}

void Calculator::round(KeypadValue& value)
{
    while (value.m_decimal_places > rounding_threshold) {
        bool const need_increment = value.m_value % 10 > 4;

        value.m_value /= 10;
        if (need_increment)
            value.m_value++;

        value.m_decimal_places--;

        if (value.m_value == 0) {
            value = 0;
            return;
        }
    }
}
