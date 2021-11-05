/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <SQLServer/ClientConnection.h>
#include <SQLServer/DatabaseConnection.h>
#include <SQLServer/SQLStatement.h>

namespace SQLServer {

static HashMap<int, NonnullRefPtr<DatabaseConnection>> s_connections;

RefPtr<DatabaseConnection> DatabaseConnection::connection_for(int connection_id)
{
    if (s_connections.contains(connection_id))
        return *s_connections.get(connection_id).value();
    dbgln_if(SQLSERVER_DEBUG, "Invalid connection_id {}", connection_id);
    return nullptr;
}

static int s_next_connection_id = 0;

DatabaseConnection::DatabaseConnection(String database_name, int client_id)
    : Object()
    , m_database_name(move(database_name))
    , m_connection_id(s_next_connection_id++)
    , m_client_id(client_id)
{
    if (LexicalPath path(m_database_name); (path.title() != m_database_name) || (path.dirname() != ".")) {
        auto client_connection = ClientConnection::client_connection_for(m_client_id);
        client_connection->async_connection_error(m_connection_id, (int)SQL::SQLErrorCode::InvalidDatabaseName, m_database_name);
        return;
    }

    dbgln_if(SQLSERVER_DEBUG, "DatabaseConnection {} initiating connection with database '{}'", connection_id(), m_database_name);
    s_connections.set(m_connection_id, *this);
    deferred_invoke([this]() {
        m_database = SQL::Database::construct(String::formatted("/home/anon/sql/{}.db", m_database_name));
        auto client_connection = ClientConnection::client_connection_for(m_client_id);
        if (auto maybe_error = m_database->open(); maybe_error.is_error()) {
            client_connection->async_connection_error(m_connection_id, (int)SQL::SQLErrorCode::InternalError, maybe_error.error().string_literal());
            return;
        }
        m_accept_statements = true;
        if (client_connection)
            client_connection->async_connected(m_connection_id, m_database_name);
        else
            warnln("Cannot notify client of database connection. Client disconnected");
    });
}

void DatabaseConnection::disconnect()
{
    dbgln_if(SQLSERVER_DEBUG, "DatabaseConnection::disconnect(connection_id {}, database '{}'", connection_id(), m_database_name);
    m_accept_statements = false;
    deferred_invoke([this]() {
        m_database = nullptr;
        s_connections.remove(m_connection_id);
        auto client_connection = ClientConnection::client_connection_for(client_id());
        if (client_connection)
            client_connection->async_disconnected(m_connection_id);
        else
            warnln("Cannot notify client of database disconnection. Client disconnected");
    });
}

int DatabaseConnection::sql_statement(String const& sql)
{
    dbgln_if(SQLSERVER_DEBUG, "DatabaseConnection::sql_statement(connection_id {}, database '{}', sql '{}'", connection_id(), m_database_name, sql);
    auto client_connection = ClientConnection::client_connection_for(client_id());
    if (!client_connection) {
        warnln("Cannot notify client of database disconnection. Client disconnected");
        return -1;
    }
    if (!m_accept_statements) {
        client_connection->async_execution_error(-1, (int)SQL::SQLErrorCode::DatabaseUnavailable, m_database_name);
        return -1;
    }
    auto statement = SQLStatement::construct(*this, sql);
    return statement->statement_id();
}

}
