/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/DeprecatedString.h>
#include <AK/Vector.h>
#include <LibSQL/Result.h>
#include <SQLServer/ConnectionFromClient.h>
#include <SQLServer/DatabaseConnection.h>
#include <SQLServer/SQLStatement.h>

namespace SQLServer {

static HashMap<int, RefPtr<ConnectionFromClient>> s_connections;

RefPtr<ConnectionFromClient> ConnectionFromClient::client_connection_for(int client_id)
{
    if (s_connections.contains(client_id))
        return *s_connections.get(client_id).value();
    dbgln_if(SQLSERVER_DEBUG, "Invalid client_id {}", client_id);
    return nullptr;
}

ConnectionFromClient::ConnectionFromClient(NonnullOwnPtr<Core::Stream::LocalSocket> socket, int client_id)
    : IPC::ConnectionFromClient<SQLClientEndpoint, SQLServerEndpoint>(*this, move(socket), client_id)
{
    s_connections.set(client_id, *this);
}

void ConnectionFromClient::die()
{
    s_connections.remove(client_id());
}

Messages::SQLServer::ConnectResponse ConnectionFromClient::connect(DeprecatedString const& database_name)
{
    dbgln_if(SQLSERVER_DEBUG, "ConnectionFromClient::connect(database_name: {})", database_name);

    if (auto database_connection = DatabaseConnection::create(database_name, client_id()); !database_connection.is_error())
        return { database_connection.value()->connection_id() };
    return { {} };
}

void ConnectionFromClient::disconnect(u64 connection_id)
{
    dbgln_if(SQLSERVER_DEBUG, "ConnectionFromClient::disconnect(connection_id: {})", connection_id);
    auto database_connection = DatabaseConnection::connection_for(connection_id);
    if (database_connection)
        database_connection->disconnect();
    else
        dbgln("Database connection has disappeared");
}

Messages::SQLServer::PrepareStatementResponse ConnectionFromClient::prepare_statement(u64 connection_id, DeprecatedString const& sql)
{
    dbgln_if(SQLSERVER_DEBUG, "ConnectionFromClient::prepare_statement(connection_id: {}, sql: '{}')", connection_id, sql);

    auto database_connection = DatabaseConnection::connection_for(connection_id);
    if (!database_connection) {
        dbgln("Database connection has disappeared");
        return { {} };
    }

    auto result = database_connection->prepare_statement(sql);
    if (result.is_error()) {
        dbgln_if(SQLSERVER_DEBUG, "Could not parse SQL statement: {}", result.error().error_string());
        return { {} };
    }

    dbgln_if(SQLSERVER_DEBUG, "ConnectionFromClient::prepare_statement -> statement_id = {}", result.value());
    return { result.value() };
}

Messages::SQLServer::ExecuteStatementResponse ConnectionFromClient::execute_statement(u64 statement_id, Vector<SQL::Value> const& placeholder_values)
{
    dbgln_if(SQLSERVER_DEBUG, "ConnectionFromClient::execute_query_statement(statement_id: {})", statement_id);

    auto statement = SQLStatement::statement_for(statement_id);
    if (statement && statement->connection()->client_id() == client_id()) {
        // FIXME: Support taking parameters from IPC requests.
        return statement->execute(move(const_cast<Vector<SQL::Value>&>(placeholder_values)));
    }

    dbgln_if(SQLSERVER_DEBUG, "Statement has disappeared");
    async_execution_error(statement_id, -1, SQL::SQLErrorCode::StatementUnavailable, DeprecatedString::formatted("{}", statement_id));
    return { {} };
}

}
