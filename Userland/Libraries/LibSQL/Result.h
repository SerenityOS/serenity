/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 * Copyright (c) 2021, Mahmoud Mandour <ma.mandourr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/Error.h>
#include <AK/Noncopyable.h>
#include <LibSQL/Type.h>

namespace SQL {

#define ENUMERATE_SQL_COMMANDS(S) \
    S(Unknown)                    \
    S(Create)                     \
    S(Delete)                     \
    S(Describe)                   \
    S(Insert)                     \
    S(Select)                     \
    S(Update)

enum class SQLCommand {
#undef __ENUMERATE_SQL_COMMAND
#define __ENUMERATE_SQL_COMMAND(command) command,
    ENUMERATE_SQL_COMMANDS(__ENUMERATE_SQL_COMMAND)
#undef __ENUMERATE_SQL_COMMAND
};

constexpr char const* command_tag(SQLCommand command)
{
    switch (command) {
#undef __ENUMERATE_SQL_COMMAND
#define __ENUMERATE_SQL_COMMAND(command) \
    case SQLCommand::command:            \
        return #command;
        ENUMERATE_SQL_COMMANDS(__ENUMERATE_SQL_COMMAND)
#undef __ENUMERATE_SQL_COMMAND
    }
}

#define ENUMERATE_SQL_ERRORS(S)                                                                   \
    S(AmbiguousColumnName, "Column name '{}' is ambiguous")                                       \
    S(BooleanOperatorTypeMismatch, "Cannot apply '{}' operator to non-boolean operands")          \
    S(ColumnDoesNotExist, "Column '{}' does not exist")                                           \
    S(DatabaseDoesNotExist, "Database '{}' does not exist")                                       \
    S(DatabaseUnavailable, "Database Unavailable")                                                \
    S(IntegerOperatorTypeMismatch, "Cannot apply '{}' operator to non-numeric operands")          \
    S(IntegerOverflow, "Operation would cause integer overflow")                                  \
    S(InternalError, "{}")                                                                        \
    S(InvalidDatabaseName, "Invalid database name '{}'")                                          \
    S(InvalidNumberOfPlaceholderValues, "Number of values does not match number of placeholders") \
    S(InvalidNumberOfValues, "Number of values does not match number of columns")                 \
    S(InvalidOperator, "Invalid operator '{}'")                                                   \
    S(InvalidType, "Invalid type '{}'")                                                           \
    S(InvalidValueType, "Invalid type for attribute '{}'")                                        \
    S(NoError, "No error")                                                                        \
    S(NotYetImplemented, "{}")                                                                    \
    S(NumericOperatorTypeMismatch, "Cannot apply '{}' operator to non-numeric operands")          \
    S(SchemaDoesNotExist, "Schema '{}' does not exist")                                           \
    S(SchemaExists, "Schema '{}' already exist")                                                  \
    S(StatementUnavailable, "Statement with id '{}' Unavailable")                                 \
    S(SyntaxError, "Syntax Error")                                                                \
    S(TableDoesNotExist, "Table '{}' does not exist")                                             \
    S(TableExists, "Table '{}' already exist")

enum class SQLErrorCode {
#undef __ENUMERATE_SQL_ERROR
#define __ENUMERATE_SQL_ERROR(error, description) error,
    ENUMERATE_SQL_ERRORS(__ENUMERATE_SQL_ERROR)
#undef __ENUMERATE_SQL_ERROR
};

class [[nodiscard]] Result {
    AK_MAKE_NONCOPYABLE(Result);
    AK_MAKE_DEFAULT_MOVABLE(Result);

public:
    ALWAYS_INLINE Result(SQLCommand command)
        : m_command(command)
    {
    }

    ALWAYS_INLINE Result(SQLCommand command, SQLErrorCode error)
        : m_command(command)
        , m_error(error)
    {
    }

    ALWAYS_INLINE Result(SQLCommand command, SQLErrorCode error, ByteString error_message)
        : m_command(command)
        , m_error(error)
        , m_error_message(move(error_message))
    {
    }

    ALWAYS_INLINE Result(Error error)
        : m_error(SQLErrorCode::InternalError)
        , m_error_message(error.string_literal())
    {
    }

    SQLCommand command() const { return m_command; }
    SQLErrorCode error() const { return m_error; }
    ByteString error_string() const;

    // These are for compatibility with the TRY() macro in AK.
    [[nodiscard]] bool is_error() const { return m_error != SQLErrorCode::NoError; }
    [[nodiscard]] Result release_value() { return move(*this); }
    Result release_error()
    {
        VERIFY(is_error());

        if (m_error_message.has_value())
            return { m_command, m_error, m_error_message.release_value() };
        return { m_command, m_error };
    }

private:
    SQLCommand m_command { SQLCommand::Unknown };

    SQLErrorCode m_error { SQLErrorCode::NoError };
    Optional<ByteString> m_error_message {};
};

template<typename ValueType>
using ResultOr = ErrorOr<ValueType, Result>;

}
