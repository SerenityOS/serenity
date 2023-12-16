/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <SQLServer/DatabaseConnection.h>
#include <SQLServer/SQLStatement.h>

namespace SQLServer {

static HashMap<SQL::ConnectionID, NonnullRefPtr<DatabaseConnection>> s_connections;
static SQL::ConnectionID s_next_connection_id = 0;

static ErrorOr<NonnullRefPtr<SQL::Database>> find_or_create_database(StringView database_path, StringView database_name)
{
    for (auto const& connection : s_connections) {
        if (connection.value->database_name() == database_name)
            return connection.value->database();
    }

    auto database_file = ByteString::formatted("{}/{}.db", database_path, database_name);
    return SQL::Database::create(move(database_file));
}

RefPtr<DatabaseConnection> DatabaseConnection::connection_for(SQL::ConnectionID connection_id)
{
    if (s_connections.contains(connection_id))
        return *s_connections.get(connection_id).value();
    dbgln_if(SQLSERVER_DEBUG, "Invalid connection_id {}", connection_id);
    return nullptr;
}

ErrorOr<NonnullRefPtr<DatabaseConnection>> DatabaseConnection::create(StringView database_path, ByteString database_name, int client_id)
{
    if (LexicalPath path(database_name); (path.title() != database_name) || (path.dirname() != "."))
        return Error::from_string_view("Invalid database name"sv);

    auto database = TRY(find_or_create_database(database_path, database_name));
    if (!database->is_open()) {
        if (auto result = database->open(); result.is_error()) {
            warnln("Could not open database: {}", result.error().error_string());
            return Error::from_string_view("Could not open database"sv);
        }
    }

    return adopt_nonnull_ref_or_enomem(new (nothrow) DatabaseConnection(move(database), move(database_name), client_id));
}

DatabaseConnection::DatabaseConnection(NonnullRefPtr<SQL::Database> database, ByteString database_name, int client_id)
    : m_database(move(database))
    , m_database_name(move(database_name))
    , m_connection_id(s_next_connection_id++)
    , m_client_id(client_id)
{
    dbgln_if(SQLSERVER_DEBUG, "DatabaseConnection {} initiatedconnection with database '{}'", connection_id(), m_database_name);
    s_connections.set(m_connection_id, *this);
}

void DatabaseConnection::disconnect()
{
    dbgln_if(SQLSERVER_DEBUG, "DatabaseConnection::disconnect(connection_id {}, database '{}'", connection_id(), m_database_name);
    s_connections.remove(connection_id());
}

SQL::ResultOr<SQL::StatementID> DatabaseConnection::prepare_statement(StringView sql)
{
    dbgln_if(SQLSERVER_DEBUG, "DatabaseConnection::prepare_statement(connection_id {}, database '{}', sql '{}'", connection_id(), m_database_name, sql);

    auto statement = TRY(SQLStatement::create(*this, sql));
    return statement->statement_id();
}

}
