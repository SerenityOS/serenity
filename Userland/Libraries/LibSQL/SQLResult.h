/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 * Copyright (c) 2021, Mahmoud Mandour <ma.mandourr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullOwnPtrVector.h>
#include <AK/Vector.h>
#include <LibCore/Object.h>
#include <LibSQL/ResultSet.h>
#include <LibSQL/Tuple.h>
#include <LibSQL/Type.h>

namespace SQL {

#define ENUMERATE_SQL_COMMANDS(S) \
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

struct SQLError {
    SQLErrorCode code { SQLErrorCode::NoError };
    String error_argument { "" };

    String to_string() const
    {
        String code_string;
        String message;
        switch (code) {
#undef __ENUMERATE_SQL_ERROR
#define __ENUMERATE_SQL_ERROR(error, description) \
    case SQLErrorCode::error:                     \
        code_string = #error;                     \
        message = description;                    \
        break;
            ENUMERATE_SQL_ERRORS(__ENUMERATE_SQL_ERROR)
#undef __ENUMERATE_SQL_ERROR
        default:
            VERIFY_NOT_REACHED();
        }
        if (!error_argument.is_null() && !error_argument.is_empty()) {
            if (message.find("{}").has_value()) {
                message = String::formatted(message, error_argument);
            } else {
                message = String::formatted("{}: {}", message, error_argument);
            }
        }
        if (message.is_null() || (message.is_empty())) {
            return code_string;
        } else {
            return String::formatted("{}: {}", code_string, message);
        }
    }
};

class SQLResult : public Core::Object {
    C_OBJECT(SQLResult)

public:
    void insert(Tuple const& row, Tuple const& sort_key);
    void limit(size_t offset, size_t limit);
    SQLCommand command() const { return m_command; }
    int updated() const { return m_update_count; }
    int inserted() const { return m_insert_count; }
    int deleted() const { return m_delete_count; }
    void set_error(SQLErrorCode code, String argument = {})
    {
        m_error.code = code;
        m_error.error_argument = argument;
    }

    bool has_error() const { return m_error.code != SQLErrorCode::NoError; }
    SQLError const& error() const { return m_error; }
    bool has_results() const { return m_has_results; }
    ResultSet const& results() const { return m_result_set; }

private:
    SQLResult() = default;

    explicit SQLResult(SQLCommand command, int update_count = 0, int insert_count = 0, int delete_count = 0)
        : m_command(command)
        , m_update_count(update_count)
        , m_insert_count(insert_count)
        , m_delete_count(delete_count)
        , m_has_results(command == SQLCommand::Select)
    {
    }

    SQLResult(SQLCommand command, SQLErrorCode error_code, String error_argument)
        : m_command(command)
        , m_error({ error_code, move(error_argument) })
    {
    }

    SQLResult(SQLCommand command, SQLErrorCode error_code, AK::Error error)
        : m_command(command)
        , m_error({ error_code, error.string_literal() })
    {
    }

    SQLCommand m_command { SQLCommand::Select };
    SQLError m_error { SQLErrorCode::NoError, "" };
    int m_update_count { 0 };
    int m_insert_count { 0 };
    int m_delete_count { 0 };
    bool m_has_results { false };
    ResultSet m_result_set;
};

}
