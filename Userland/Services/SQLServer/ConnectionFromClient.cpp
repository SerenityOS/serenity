/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
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

ConnectionFromClient::~ConnectionFromClient()
{
}

void ConnectionFromClient::die()
{
    s_connections.remove(client_id());
}

Messages::SQLServer::ConnectResponse ConnectionFromClient::connect(String const& database_name)
{
    dbgln_if(SQLSERVER_DEBUG, "ConnectionFromClient::connect(database_name: {})", database_name);
    auto database_connection = DatabaseConnection::construct(database_name, client_id());
    return { database_connection->connection_id() };
}

void ConnectionFromClient::disconnect(int connection_id)
{
    dbgln_if(SQLSERVER_DEBUG, "ConnectionFromClient::disconnect(connection_id: {})", connection_id);
    auto database_connection = DatabaseConnection::connection_for(connection_id);
    if (database_connection)
        database_connection->disconnect();
    else
        dbgln("Database connection has disappeared");
}

Messages::SQLServer::SqlStatementResponse ConnectionFromClient::sql_statement(int connection_id, String const& sql)
{
    dbgln_if(SQLSERVER_DEBUG, "ConnectionFromClient::sql_statement(connection_id: {}, sql: '{}')", connection_id, sql);
    auto database_connection = DatabaseConnection::connection_for(connection_id);
    if (database_connection) {
        auto statement_id = database_connection->sql_statement(sql);
        dbgln_if(SQLSERVER_DEBUG, "ConnectionFromClient::sql_statement -> statement_id = {}", statement_id);
        return { statement_id };
    } else {
        dbgln("Database connection has disappeared");
        return { -1 };
    }
}

void ConnectionFromClient::statement_execute(int statement_id)
{
    dbgln_if(SQLSERVER_DEBUG, "ConnectionFromClient::statement_execute_query(statement_id: {})", statement_id);
    auto statement = SQLStatement::statement_for(statement_id);
    if (statement && statement->connection()->client_id() == client_id()) {
        statement->execute();
    } else {
        dbgln_if(SQLSERVER_DEBUG, "Statement has disappeared");
        async_execution_error(statement_id, (int)SQL::SQLErrorCode::StatementUnavailable, String::formatted("{}", statement_id));
    }
}

}
