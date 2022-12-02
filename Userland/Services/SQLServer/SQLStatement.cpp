/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/Object.h>
#include <LibSQL/AST/Parser.h>
#include <SQLServer/ConnectionFromClient.h>
#include <SQLServer/DatabaseConnection.h>
#include <SQLServer/SQLStatement.h>

namespace SQLServer {

static HashMap<u64, NonnullRefPtr<SQLStatement>> s_statements;
static u64 s_next_statement_id = 0;

RefPtr<SQLStatement> SQLStatement::statement_for(u64 statement_id)
{
    if (s_statements.contains(statement_id))
        return *s_statements.get(statement_id).value();
    dbgln_if(SQLSERVER_DEBUG, "Invalid statement_id {}", statement_id);
    return nullptr;
}

SQL::ResultOr<NonnullRefPtr<SQLStatement>> SQLStatement::create(DatabaseConnection& connection, StringView sql)
{
    auto parser = SQL::AST::Parser(SQL::AST::Lexer(sql));
    auto statement = parser.next_statement();

    if (parser.has_errors())
        return SQL::Result { SQL::SQLCommand::Unknown, SQL::SQLErrorCode::SyntaxError, parser.errors()[0].to_deprecated_string() };

    return TRY(adopt_nonnull_ref_or_enomem(new (nothrow) SQLStatement(connection, move(statement))));
}

SQLStatement::SQLStatement(DatabaseConnection& connection, NonnullRefPtr<SQL::AST::Statement> statement)
    : Core::Object(&connection)
    , m_statement_id(s_next_statement_id++)
    , m_statement(move(statement))
{
    dbgln_if(SQLSERVER_DEBUG, "SQLStatement({})", connection.connection_id());
    s_statements.set(m_statement_id, *this);
}

void SQLStatement::report_error(SQL::Result result)
{
    dbgln_if(SQLSERVER_DEBUG, "SQLStatement::report_error(statement_id {}, error {}", statement_id(), result.error_string());

    auto client_connection = ConnectionFromClient::client_connection_for(connection()->client_id());

    s_statements.remove(statement_id());
    remove_from_parent();

    if (client_connection)
        client_connection->async_execution_error(statement_id(), result.error(), result.error_string());
    else
        warnln("Cannot return execution error. Client disconnected");

    m_result = {};
}

void SQLStatement::execute(Vector<SQL::Value> placeholder_values)
{
    dbgln_if(SQLSERVER_DEBUG, "SQLStatement::execute(statement_id {}", statement_id());

    auto client_connection = ConnectionFromClient::client_connection_for(connection()->client_id());
    if (!client_connection) {
        warnln("Cannot yield next result. Client disconnected");
        return;
    }

    deferred_invoke([this, placeholder_values = move(placeholder_values)] {
        VERIFY(!connection()->database().is_null());

        auto execution_result = m_statement->execute(connection()->database().release_nonnull(), placeholder_values);
        if (execution_result.is_error()) {
            report_error(execution_result.release_error());
            return;
        }

        auto client_connection = ConnectionFromClient::client_connection_for(connection()->client_id());
        if (!client_connection) {
            warnln("Cannot return statement execution results. Client disconnected");
            return;
        }

        m_result = execution_result.release_value();

        if (should_send_result_rows()) {
            client_connection->async_execution_success(statement_id(), true, 0, 0, 0);
            m_index = 0;
            next();
        } else {
            client_connection->async_execution_success(statement_id(), false, 0, m_result->size(), 0);
        }
    });
}

bool SQLStatement::should_send_result_rows() const
{
    VERIFY(m_result.has_value());

    if (m_result->is_empty())
        return false;

    switch (m_result->command()) {
    case SQL::SQLCommand::Describe:
    case SQL::SQLCommand::Select:
        return true;
    default:
        return false;
    }
}

void SQLStatement::next()
{
    VERIFY(!m_result->is_empty());
    auto client_connection = ConnectionFromClient::client_connection_for(connection()->client_id());
    if (!client_connection) {
        warnln("Cannot yield next result. Client disconnected");
        return;
    }
    if (m_index < m_result->size()) {
        auto& tuple = m_result->at(m_index++).row;
        client_connection->async_next_result(statement_id(), tuple.to_deprecated_string_vector());
        deferred_invoke([this]() {
            next();
        });
    } else {
        client_connection->async_results_exhausted(statement_id(), m_index);
    }
}

}
