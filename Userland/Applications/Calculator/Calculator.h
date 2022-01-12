/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCrypto/BigFraction/BigFraction.h>

// This type implements the regular calculator
// behavior, such as performing arithmetic
// operations and providing a memory cell.
// It does not deal with number input; you
// have to pass in already parsed double
// values.

class Calculator final {
public:
    Calculator() = default;
    ~Calculator() = default;

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

    Crypto::BigFraction begin_operation(Operation, Crypto::BigFraction);
    Crypto::BigFraction finish_operation(Crypto::BigFraction);

    bool has_error() const { return m_has_error; }

    void clear_operation();
    void clear_error() { m_has_error = false; }

private:
    Operation m_operation_in_progress { Operation::None };
    Crypto::BigFraction m_saved_argument {};
    Crypto::BigFraction m_mem {};
    bool m_has_error { false };
};
