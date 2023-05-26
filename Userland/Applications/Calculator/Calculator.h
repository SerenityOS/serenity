/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <LibCrypto/BigFraction/BigFraction.h>

namespace Calculator {

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
        MemAdd,

        Equals
    };

    Optional<Crypto::BigFraction> operation_with_literal_argument(Operation, Crypto::BigFraction);
    Optional<Crypto::BigFraction> operation_without_argument(Operation);

    bool has_error() const { return m_has_error; }

    void clear_operation();
    void clear_error() { m_has_error = false; }

private:
    Crypto::BigFraction m_mem {};

    Crypto::BigFraction m_current_value {};

    Operation m_binary_operation_in_progress { Operation::None };
    Crypto::BigFraction m_binary_operation_saved_left_side {};

    Operation m_previous_operation { Operation::None };
    Crypto::BigFraction m_previous_binary_operation_right_side {};
    bool m_has_error { false };

    Crypto::BigFraction finish_binary_operation(Crypto::BigFraction const& left_side, Operation operation, Crypto::BigFraction const& right_side);
};

}
