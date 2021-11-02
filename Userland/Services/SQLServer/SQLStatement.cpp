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

void SQLStatement::report_error(SQL::SQLError error)
{
    dbgln_if(SQLSERVER_DEBUG, "SQLStatement::report_error(statement_id {}, error {}", statement_id(), error.to_string());
    auto client_connection = ClientConnection::client_connection_for(connection()->client_id());
    m_statement = nullptr;
    m_result = nullptr;
    remove_from_parent();
    s_statements.remove(statement_id());
    if (!client_connection) {
        warnln("Cannot return execution error. Client disconnected");
        warnln("SQLStatement::report_error(statement_id {}, error {}", statement_id(), error.to_string());
        m_result = nullptr;
        return;
    }
    client_connection->async_execution_error(statement_id(), (int)error.code, error.to_string());
    m_result = nullptr;
}

void SQLStatement::execute()
{
    dbgln_if(SQLSERVER_DEBUG, "SQLStatement::execute(statement_id {}", statement_id());
    auto client_connection = ClientConnection::client_connection_for(connection()->client_id());
    if (!client_connection) {
        warnln("Cannot yield next result. Client disconnected");
        return;
    }

    deferred_invoke([this]() {
        auto maybe_error = parse();
        if (maybe_error.has_value()) {
            report_error(maybe_error.value());
            return;
        }
        VERIFY(!connection()->database().is_null());
        m_result = m_statement->execute(connection()->database().release_nonnull());
        if (m_result->error().code != SQL::SQLErrorCode::NoError) {
            report_error(m_result->error());
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

Optional<SQL::SQLError> SQLStatement::parse()
{
    auto parser = SQL::AST::Parser(SQL::AST::Lexer(m_sql));
    m_statement = parser.next_statement();
    if (parser.has_errors()) {
        return SQL::SQLError { SQL::SQLErrorCode::SyntaxError, parser.errors()[0].to_string() };
    }
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
        auto& tuple = m_result->results()[m_index++];
        client_connection->async_next_result(statement_id(), tuple.to_string_vector());
        deferred_invoke([this]() {
            next();
        });
    } else {
        client_connection->async_results_exhausted(statement_id(), (int)m_index);
    }
}

}
