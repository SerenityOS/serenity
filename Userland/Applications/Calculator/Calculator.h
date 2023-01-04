/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <AK/String.h>
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
        MemAdd,

        Equals
    };

    class [[nodiscard]] RunningExpression {
    public:
        enum class Type {
            None,
            Unary,
            PartialBinary,
            CompleteBinary,
        };

        static RunningExpression make_binary(Crypto::BigFraction const& left, Operation op);
        static RunningExpression make_binary(Crypto::BigFraction const& left, Operation op, Crypto::BigFraction const& right);
        static RunningExpression make_unary(Crypto::BigFraction const& value, Operation op);

        DeprecatedString to_string() const;
        void clear() { m_type = Type::None; }
        Type expression_type() const { return m_type; }

    private:
        Type m_type;
        double m_left;
        Operation m_op;
        double m_right;
    };

    Optional<Crypto::BigFraction> operation_with_literal_argument(Operation, Crypto::BigFraction);
    Optional<Crypto::BigFraction> operation_without_argument(Operation);

    bool has_error() const { return m_has_error; }

    void clear() { clear_operation_and_running_expression(); }
    void clear_error() { m_has_error = false; }

    [[nodiscard]] RunningExpression const& running_expression() const;

private:
    Crypto::BigFraction m_mem {};

    Crypto::BigFraction m_current_value {};

    Operation m_binary_operation_in_progress { Operation::None };
    Crypto::BigFraction m_binary_operation_saved_left_side {};

    Operation m_previous_operation { Operation::None };
    Crypto::BigFraction m_previous_binary_operation_right_side {};
    bool m_has_error { false };

    RunningExpression m_running_expression;

    Crypto::BigFraction finish_binary_operation(Crypto::BigFraction const& left_side, Operation operation, Crypto::BigFraction const& right_side);
    void clear_operation();
    void clear_operation_and_running_expression();
};
