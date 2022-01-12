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

Crypto::BigFraction Calculator::begin_operation(Operation operation, Crypto::BigFraction argument)
{
    Crypto::BigFraction res {};

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
        if (argument < Crypto::BigFraction {}) {
            m_has_error = true;
            return argument;
        }
        res = argument.sqrt();
        clear_operation();
        break;
    case Operation::Inverse:
        if (argument == Crypto::BigFraction {}) {
            m_has_error = true;
            return argument;
        }
        res = argument.invert();
        clear_operation();
        break;
    case Operation::Percent:
        res = argument * Crypto::BigFraction { 1, 100 };
        break;
    case Operation::ToggleSign:
        res = -argument;
        break;

    case Operation::MemClear:
        m_mem.set_to_0();
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

    return res;
}

Crypto::BigFraction Calculator::finish_operation(Crypto::BigFraction argument)
{
    Crypto::BigFraction res {};

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
        if (argument == Crypto::BigFraction {}) {
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

    clear_operation();
    return res;
}

void Calculator::clear_operation()
{
    m_operation_in_progress = Operation::None;
    m_saved_argument.set_to_0();
    clear_error();
}
