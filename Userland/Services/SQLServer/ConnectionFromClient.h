/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/Vector.h>
#include <LibIPC/ConnectionFromClient.h>
#include <LibSQL/Type.h>
#include <SQLServer/SQLClientEndpoint.h>
#include <SQLServer/SQLServerEndpoint.h>

namespace SQLServer {

class ConnectionFromClient final
    : public IPC::ConnectionFromClient<SQLClientEndpoint, SQLServerEndpoint> {
    C_OBJECT(ConnectionFromClient);

public:
    virtual ~ConnectionFromClient() override = default;

    virtual void die() override;

    static RefPtr<ConnectionFromClient> client_connection_for(int client_id);

    void set_database_path(ByteString);
    Function<void()> on_disconnect;

private:
    explicit ConnectionFromClient(NonnullOwnPtr<Core::LocalSocket>, int client_id);

    virtual Messages::SQLServer::ConnectResponse connect(ByteString const&) override;
    virtual Messages::SQLServer::PrepareStatementResponse prepare_statement(SQL::ConnectionID, ByteString const&) override;
    virtual Messages::SQLServer::ExecuteStatementResponse execute_statement(SQL::StatementID, Vector<SQL::Value> const& placeholder_values) override;
    virtual void ready_for_next_result(SQL::StatementID, SQL::ExecutionID) override;
    virtual void disconnect(SQL::ConnectionID) override;

    ByteString m_database_path;
};

}
