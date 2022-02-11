/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 * Copyright (c) 2021, Mahmoud Mandour <ma.mandourr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

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

#define ENUMERATE_SQL_ERRORS(S)                                                          \
    S(NoError, "No error")                                                               \
    S(InternalError, "{}")                                                               \
    S(NotYetImplemented, "{}")                                                           \
    S(DatabaseUnavailable, "Database Unavailable")                                       \
    S(StatementUnavailable, "Statement with id '{}' Unavailable")                        \
    S(SyntaxError, "Syntax Error")                                                       \
    S(DatabaseDoesNotExist, "Database '{}' does not exist")                              \
    S(SchemaDoesNotExist, "Schema '{}' does not exist")                                  \
    S(SchemaExists, "Schema '{}' already exist")                                         \
    S(TableDoesNotExist, "Table '{}' does not exist")                                    \
    S(ColumnDoesNotExist, "Column '{}' does not exist")                                  \
    S(AmbiguousColumnName, "Column name '{}' is ambiguous")                              \
    S(TableExists, "Table '{}' already exist")                                           \
    S(InvalidType, "Invalid type '{}'")                                                  \
    S(InvalidDatabaseName, "Invalid database name '{}'")                                 \
    S(InvalidValueType, "Invalid type for attribute '{}'")                               \
    S(InvalidNumberOfValues, "Number of values does not match number of columns")        \
    S(BooleanOperatorTypeMismatch, "Cannot apply '{}' operator to non-boolean operands") \
    S(NumericOperatorTypeMismatch, "Cannot apply '{}' operator to non-numeric operands") \
    S(IntegerOperatorTypeMismatch, "Cannot apply '{}' operator to non-numeric operands") \
    S(InvalidOperator, "Invalid operator '{}'")

enum class SQLErrorCode {
#undef __ENUMERATE_SQL_ERROR
#define __ENUMERATE_SQL_ERROR(error, description) error,
    ENUMERATE_SQL_ERRORS(__ENUMERATE_SQL_ERROR)
#undef __ENUMERATE_SQL_ERROR
};

class [[nodiscard]] Result {
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

    ALWAYS_INLINE Result(SQLCommand command, SQLErrorCode error, String error_message)
        : m_command(command)
        , m_error(error)
        , m_error_message(move(error_message))
    {
    }

    ALWAYS_INLINE Result(Error error)
        : m_error(static_cast<SQLErrorCode>(error.code()))
        , m_error_message(error.string_literal())
    {
    }

    Result(Result&&) = default;
    Result& operator=(Result&&) = default;

    SQLCommand command() const { return m_command; }
    SQLErrorCode error() const { return m_error; }
    String error_string() const;

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
    AK_MAKE_NONCOPYABLE(Result);

    SQLCommand m_command { SQLCommand::Unknown };

    SQLErrorCode m_error { SQLErrorCode::NoError };
    Optional<String> m_error_message {};
};

template<typename ValueType>
using ResultOr = ErrorOr<ValueType, Result>;

}
