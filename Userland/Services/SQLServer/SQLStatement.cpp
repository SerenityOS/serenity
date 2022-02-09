/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/Object.h>
#include <LibSQL/AST/Parser.h>
#include <SQLServer/ClientConnection.h>
#include <SQLServer/DatabaseConnection.h>
#include <SQLServer/SQLStatement.h>

namespace SQLServer {

static HashMap<int, NonnullRefPtr<SQLStatement>> s_statements;

RefPtr<SQLStatement> SQLStatement::statement_for(int statement_id)
{
    if (s_statements.contains(statement_id))
        return *s_statements.get(statement_id).value();
    dbgln_if(SQLSERVER_DEBUG, "Invalid statement_id {}", statement_id);
    return nullptr;
}

static int s_next_statement_id = 0;

SQLStatement::SQLStatement(DatabaseConnection& connection, String sql)
    : Core::Object(&connection)
    , m_statement_id(s_next_statement_id++)
    , m_sql(move(sql))
{
    dbgln_if(SQLSERVER_DEBUG, "SQLStatement({}, {})", connection.connection_id(), sql);
    s_statements.set(m_statement_id, *this);
}

void SQLStatement::report_error()
{
    dbgln_if(SQLSERVER_DEBUG, "SQLStatement::report_error(statement_id {}, error {}", statement_id(), m_result->error_string());

    auto client_connection = ClientConnection::client_connection_for(connection()->client_id());

    s_statements.remove(statement_id());
    remove_from_parent();

    if (client_connection)
        client_connection->async_execution_error(statement_id(), (int)m_result->error(), m_result->error_string());
    else
        warnln("Cannot return execution error. Client disconnected");

    m_statement = nullptr;
    m_result = {};
}

void SQLStatement::execute()
{
    dbgln_if(SQLSERVER_DEBUG, "SQLStatement::execute(statement_id {}", statement_id());
    auto client_connection = ClientConnection::client_connection_for(connection()->client_id());
    if (!client_connection) {
        warnln("Cannot yield next result. Client disconnected");
        return;
    }

    deferred_invoke([this]() mutable {
        m_result = parse();
        if (m_result.has_value()) {
            report_error();
            return;
        }

        VERIFY(!connection()->database().is_null());

        m_result = m_statement->execute(connection()->database().release_nonnull());
        if (m_result->is_error()) {
            report_error();
            return;
        }

        auto client_connection = ClientConnection::client_connection_for(connection()->client_id());
        if (!client_connection) {
            warnln("Cannot return statement execution results. Client disconnected");
            return;
        }
        client_connection->async_execution_success(statement_id(), m_result->has_results(), m_result->updated(), m_result->inserted(), m_result->deleted());
        if (m_result->has_results()) {
            m_index = 0;
            next();
        }
    });
}

Optional<SQL::Result> SQLStatement::parse()
{
    auto parser = SQL::AST::Parser(SQL::AST::Lexer(m_sql));
    m_statement = parser.next_statement();

    if (parser.has_errors())
        return SQL::Result { SQL::SQLCommand::Unknown, SQL::SQLErrorCode::SyntaxError, parser.errors()[0].to_string() };
    return {};
}

void SQLStatement::next()
{
    VERIFY(m_result->has_results());
    auto client_connection = ClientConnection::client_connection_for(connection()->client_id());
    if (!client_connection) {
        warnln("Cannot yield next result. Client disconnected");
        return;
    }
    if (m_index < m_result->results().size()) {
        auto& tuple = m_result->results()[m_index++].row;
        client_connection->async_next_result(statement_id(), tuple.to_string_vector());
        deferred_invoke([this]() {
            next();
        });
    } else {
        client_connection->async_results_exhausted(statement_id(), (int)m_index);
    }
}

}
