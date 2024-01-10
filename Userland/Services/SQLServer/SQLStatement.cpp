/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/EventReceiver.h>
#include <LibSQL/AST/Parser.h>
#include <SQLServer/ConnectionFromClient.h>
#include <SQLServer/DatabaseConnection.h>
#include <SQLServer/SQLStatement.h>

namespace SQLServer {

static HashMap<SQL::StatementID, NonnullRefPtr<SQLStatement>> s_statements;
static SQL::StatementID s_next_statement_id = 0;

RefPtr<SQLStatement> SQLStatement::statement_for(SQL::StatementID statement_id)
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
        return SQL::Result { SQL::SQLCommand::Unknown, SQL::SQLErrorCode::SyntaxError, parser.errors()[0].to_byte_string() };

    return TRY(adopt_nonnull_ref_or_enomem(new (nothrow) SQLStatement(connection, move(statement))));
}

SQLStatement::SQLStatement(DatabaseConnection& connection, NonnullRefPtr<SQL::AST::Statement> statement)
    : m_connection(connection)
    , m_statement_id(s_next_statement_id++)
    , m_statement(move(statement))
{
    dbgln_if(SQLSERVER_DEBUG, "SQLStatement({})", connection.connection_id());
    s_statements.set(m_statement_id, *this);
}

void SQLStatement::report_error(SQL::Result result, SQL::ExecutionID execution_id)
{
    dbgln_if(SQLSERVER_DEBUG, "SQLStatement::report_error(statement_id {}, error {}", statement_id(), result.error_string());

    auto client_connection = ConnectionFromClient::client_connection_for(connection().client_id());

    s_statements.remove(statement_id());

    if (client_connection)
        client_connection->async_execution_error(statement_id(), execution_id, result.error(), result.error_string());
    else
        warnln("Cannot return execution error. Client disconnected");
}

Optional<SQL::ExecutionID> SQLStatement::execute(Vector<SQL::Value> placeholder_values)
{
    dbgln_if(SQLSERVER_DEBUG, "SQLStatement::execute(statement_id {}", statement_id());

    auto client_connection = ConnectionFromClient::client_connection_for(connection().client_id());
    if (!client_connection) {
        warnln("Cannot yield next result. Client disconnected");
        return {};
    }

    auto execution_id = m_next_execution_id++;

    Core::deferred_invoke([this, strong_this = NonnullRefPtr(*this), placeholder_values = move(placeholder_values), execution_id] {
        auto execution_result = m_statement->execute(connection().database(), placeholder_values);

        if (execution_result.is_error()) {
            report_error(execution_result.release_error(), execution_id);
            return;
        }

        auto client_connection = ConnectionFromClient::client_connection_for(connection().client_id());
        if (!client_connection) {
            warnln("Cannot return statement execution results. Client disconnected");
            return;
        }

        auto result = execution_result.release_value();
        auto result_size = result.size();

        if (should_send_result_rows(result)) {
            client_connection->async_execution_success(statement_id(), execution_id, result.column_names(), true, 0, 0, 0);

            m_ongoing_executions.set(execution_id, { move(result), result_size });
            ready_for_next_result(execution_id);
        } else {
            if (result.command() == SQL::SQLCommand::Insert)
                client_connection->async_execution_success(statement_id(), execution_id, result.column_names(), false, result_size, 0, 0);
            else if (result.command() == SQL::SQLCommand::Update)
                client_connection->async_execution_success(statement_id(), execution_id, result.column_names(), false, 0, result_size, 0);
            else if (result.command() == SQL::SQLCommand::Delete)
                client_connection->async_execution_success(statement_id(), execution_id, result.column_names(), false, 0, 0, result_size);
            else
                client_connection->async_execution_success(statement_id(), execution_id, result.column_names(), false, 0, 0, 0);
        }
    });

    return execution_id;
}

void SQLStatement::ready_for_next_result(SQL::ExecutionID execution_id)
{
    auto client_connection = ConnectionFromClient::client_connection_for(connection().client_id());
    if (!client_connection) {
        warnln("Cannot yield next result. Client disconnected");
        return;
    }

    auto execution = m_ongoing_executions.get(execution_id);
    if (!execution.has_value()) {
        return;
    }

    if (execution->result.is_empty()) {
        client_connection->async_results_exhausted(statement_id(), execution_id, execution->result_size);
        m_ongoing_executions.remove(execution_id);
        return;
    }

    auto result_row = execution->result.take_first();
    client_connection->async_next_result(statement_id(), execution_id, result_row.row.take_data());
}

bool SQLStatement::should_send_result_rows(SQL::ResultSet const& result) const
{
    if (result.is_empty())
        return false;

    switch (result.command()) {
    case SQL::SQLCommand::Describe:
    case SQL::SQLCommand::Select:
        return true;
    default:
        return false;
    }
}

}
