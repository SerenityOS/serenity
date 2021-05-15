/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Calculator.h"
#include <AK/Assertions.h>
#include <math.h>

Calculator::Calculator()
{
}

Calculator::~Calculator()
{
}

double Calculator::begin_operation(Operation operation, double argument)
{
    double res = 0.0;

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
        if (argument < 0.0) {
            m_has_error = true;
            return argument;
        }
        res = sqrt(argument);
        clear_operation();
        break;
    case Operation::Inverse:
        if (argument == 0.0) {
            m_has_error = true;
            return argument;
        }
        res = 1 / argument;
        clear_operation();
        break;
    case Operation::Percent:
        res = argument * 0.01;
        break;
    case Operation::ToggleSign:
        res = -argument;
        break;

    case Operation::MemClear:
        m_mem = 0.0;
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
        m_mem += argument;
        res = m_mem;
        break;
    }

    return res;
}

double Calculator::finish_operation(double argument)
{
    double res = 0.0;

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
        if (argument == 0.0) {
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
    m_saved_argument = 0.0;
    clear_error();
}
