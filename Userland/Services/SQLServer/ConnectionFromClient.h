/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <LibIPC/ConnectionFromClient.h>
#include <SQLServer/SQLClientEndpoint.h>
#include <SQLServer/SQLServerEndpoint.h>

namespace SQLServer {

class ConnectionFromClient final
    : public IPC::ConnectionFromClient<SQLClientEndpoint, SQLServerEndpoint> {
    C_OBJECT(ConnectionFromClient);

public:
    virtual ~ConnectionFromClient() override;

    virtual void die() override;

    static RefPtr<ConnectionFromClient> client_connection_for(int client_id);

private:
    explicit ConnectionFromClient(NonnullOwnPtr<Core::Stream::LocalSocket>, int client_id);

    virtual Messages::SQLServer::ConnectResponse connect(String const&) override;
    virtual Messages::SQLServer::SqlStatementResponse sql_statement(int, String const&) override;
    virtual void statement_execute(int) override;
    virtual void disconnect(int) override;
};

}
