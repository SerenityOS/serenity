/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

// This type implements the regular calculator
// behavior, such as performing arithmetic
// operations and providing a memory cell.
// It does not deal with number input; you
// have to pass in already parsed double
// values.

class Calculator final {
public:
    Calculator();
    ~Calculator();

    enum class Operation {
        None,
        Add,
        Subtract,
        Multiply,
        Divide,

        Sqrt,
        Inverse,
        Percent,
        ToggleSign,

        MemClear,
        MemRecall,
        MemSave,
        MemAdd
    };

    double begin_operation(Operation, double);
    double finish_operation(double);

    bool has_error() const { return m_has_error; }

    void clear_operation();
    void clear_error() { m_has_error = false; }

private:
    Operation m_operation_in_progress { Operation::None };
    double m_saved_argument { 0.0 };
    double m_mem { 0.0 };
    bool m_has_error { false };
};
