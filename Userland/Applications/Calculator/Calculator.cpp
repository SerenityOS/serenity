/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Calculator.h"
#include <AK/Assertions.h>
#include <AK/Math.h>
#include <LibCrypto/BigFraction/BigFraction.h>

namespace Calculator {

Optional<Crypto::BigFraction> Calculator::operation_with_literal_argument(Operation operation, Crypto::BigFraction argument)
{
    // Support binary operations with percentages, for example "2+3%" == 2.06
    if (m_binary_operation_in_progress != Operation::None && operation == Operation::Percent) {
        argument = m_binary_operation_saved_left_side * Crypto::BigFraction { 1, 100 } * argument;
        operation = Operation::None; // Don't apply the "%" operation twice
    }

    switch (operation) {
    case Operation::None:
        m_current_value = argument;
        break;

    case Operation::Add:
    case Operation::Subtract:
    case Operation::Multiply:
    case Operation::Divide:
        // If a previous operation is still in progress, finish it
        // Makes hitting "1+2+3=" equivalent to hitting "1+2=+3="
        if (m_binary_operation_in_progress != Operation::None) {
            argument = finish_binary_operation(m_binary_operation_saved_left_side, m_binary_operation_in_progress, argument);
        }
        m_binary_operation_saved_left_side = argument;
        m_binary_operation_in_progress = operation;
        m_current_value = argument;
        break;

    case Operation::Sqrt:
        if (argument < Crypto::BigFraction {}) {
            m_has_error = true;
            m_current_value = argument;
            break;
        }
        m_current_value = argument.sqrt();
        break;
    case Operation::Inverse:
        if (argument == Crypto::BigFraction {}) {
            m_has_error = true;
            m_current_value = argument;
            break;
        }
        m_current_value = argument.invert();
        break;
    case Operation::Percent:
        m_current_value = argument * Crypto::BigFraction { 1, 100 };
        break;
    case Operation::ToggleSign:
        m_current_value = -argument;
        break;

    case Operation::MemClear:
        m_mem.set_to_0();
        m_current_value = argument;
        break;
    case Operation::MemRecall:
        m_current_value = m_mem;
        break;
    case Operation::MemSave:
        m_mem = argument;
        m_current_value = argument;
        break;
    case Operation::MemAdd:
        m_mem = m_mem + argument; // avoids the need for operator+=()
        m_current_value = m_mem;
        break;
    case Operation::Equals:
        if (m_binary_operation_in_progress != Operation::None) {
            argument = finish_binary_operation(m_binary_operation_saved_left_side, m_binary_operation_in_progress, argument);
        }
        m_current_value = argument;
        break;
    }

    return m_current_value;
}

static bool operation_is_binary(Calculator::Operation operation)
{
    switch (operation) {

    case Calculator::Operation::Add:
    case Calculator::Operation::Subtract:
    case Calculator::Operation::Multiply:
    case Calculator::Operation::Divide:
        return true;

    default:
        return false;
    }
}

Optional<Crypto::BigFraction> Calculator::operation_without_argument(Operation operation)
{
    bool in_binary_operation = m_binary_operation_in_progress != Operation::None;
    bool entering_new_binary_operation = operation_is_binary(operation);
    bool previous_operation_was_binary = operation_is_binary(m_previous_operation);

    if (in_binary_operation && entering_new_binary_operation) {
        m_binary_operation_in_progress = operation;
        return {};
    }
    if (!in_binary_operation && previous_operation_was_binary && operation == Operation::Equals) {
        m_current_value = finish_binary_operation(m_current_value, m_previous_operation, m_previous_binary_operation_right_side);
        return m_current_value;
    }
    return operation_with_literal_argument(operation, m_current_value);
}

Crypto::BigFraction Calculator::finish_binary_operation(Crypto::BigFraction const& left_side, Operation operation, Crypto::BigFraction const& right_side)
{
    Crypto::BigFraction res {};

    m_previous_binary_operation_right_side = right_side;

    switch (operation) {

    case Operation::Add:
        res = left_side + right_side;
        break;
    case Operation::Subtract:
        res = left_side - right_side;
        break;
    case Operation::Multiply:
        res = left_side * right_side;
        break;
    case Operation::Divide:
        if (right_side == Crypto::BigFraction {}) {
            m_has_error = true;
        } else {
            res = left_side / right_side;
        }
        break;

    case Operation::None:
    case Operation::Sqrt:
    case Operation::Inverse:
    case Operation::Percent:
    case Operation::ToggleSign:
    case Operation::MemClear:
    case Operation::MemRecall:
    case Operation::MemSave:
    case Operation::MemAdd:
    case Operation::Equals:
        VERIFY_NOT_REACHED();
    }

    clear_operation();
    return res;
}

void Calculator::clear_operation()
{
    if (m_binary_operation_in_progress != Operation::None) {
        m_previous_operation = m_binary_operation_in_progress;
        m_binary_operation_in_progress = Operation::None;
    }
    m_binary_operation_saved_left_side.set_to_0();
    clear_error();
}

}
