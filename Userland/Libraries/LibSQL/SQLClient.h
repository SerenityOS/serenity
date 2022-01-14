/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibIPC/ServerConnection.h>
#include <SQLServer/SQLClientEndpoint.h>
#include <SQLServer/SQLServerEndpoint.h>

namespace SQL {

class SQLClient
    : public IPC::ServerConnection<SQLClientEndpoint, SQLServerEndpoint>
    , public SQLClientEndpoint {
    IPC_CLIENT_CONNECTION(SQLClient, "/tmp/portal/sql")
    virtual ~SQLClient();

    Function<void(int, String const&)> on_connected;
    Function<void(int)> on_disconnected;
    Function<void(int, int, String const&)> on_connection_error;
    Function<void(int, int, String const&)> on_execution_error;
    Function<void(int, bool, int, int, int)> on_execution_success;
    Function<void(int, Vector<String> const&)> on_next_result;
    Function<void(int, int)> on_results_exhausted;

private:
    SQLClient(NonnullOwnPtr<Core::Stream::LocalSocket> socket)
        : IPC::ServerConnection<SQLClientEndpoint, SQLServerEndpoint>(*this, move(socket))
    {
    }

    virtual void connected(int connection_id, String const& connected_to_database) override;
    virtual void connection_error(int connection_id, int code, String const& message) override;
    virtual void execution_success(int statement_id, bool has_results, int created, int updated, int deleted) override;
    virtual void next_result(int statement_id, Vector<String> const&) override;
    virtual void results_exhausted(int statement_id, int total_rows) override;
    virtual void execution_error(int statement_id, int code, String const& message) override;
    virtual void disconnected(int connection_id) override;
};

}
