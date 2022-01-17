/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <LibIPC/ClientConnection.h>
#include <SQLServer/SQLClientEndpoint.h>
#include <SQLServer/SQLServerEndpoint.h>

namespace SQLServer {

class ClientConnection final
    : public IPC::ClientConnection<SQLClientEndpoint, SQLServerEndpoint> {
    C_OBJECT(ClientConnection);

public:
    virtual ~ClientConnection() override;

    virtual void die() override;

    static RefPtr<ClientConnection> client_connection_for(int client_id);

private:
    explicit ClientConnection(NonnullOwnPtr<Core::Stream::LocalSocket>, int client_id);

    virtual Messages::SQLServer::ConnectResponse connect(String const&) override;
    virtual Messages::SQLServer::SqlStatementResponse sql_statement(int, String const&) override;
    virtual void statement_execute(int) override;
    virtual void disconnect(int) override;
};

}
