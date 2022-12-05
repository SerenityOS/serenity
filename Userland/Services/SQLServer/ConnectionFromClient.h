/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/HashMap.h>
#include <AK/Vector.h>
#include <LibIPC/ConnectionFromClient.h>
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

    void set_database_path(DeprecatedString);

private:
    explicit ConnectionFromClient(NonnullOwnPtr<Core::Stream::LocalSocket>, int client_id);

    virtual Messages::SQLServer::ConnectResponse connect(DeprecatedString const&) override;
    virtual Messages::SQLServer::PrepareStatementResponse prepare_statement(u64, DeprecatedString const&) override;
    virtual Messages::SQLServer::ExecuteStatementResponse execute_statement(u64, Vector<SQL::Value> const& placeholder_values) override;
    virtual void disconnect(u64) override;

    DeprecatedString m_database_path;
};

}
