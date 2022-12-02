/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibIPC/ConnectionToServer.h>
#include <LibSQL/Result.h>
#include <SQLServer/SQLClientEndpoint.h>
#include <SQLServer/SQLServerEndpoint.h>

namespace SQL {

class SQLClient
    : public IPC::ConnectionToServer<SQLClientEndpoint, SQLServerEndpoint>
    , public SQLClientEndpoint {
    IPC_CLIENT_CONNECTION(SQLClient, "/tmp/session/%sid/portal/sql"sv)
    virtual ~SQLClient() = default;

    Function<void(u64, DeprecatedString const&)> on_connected;
    Function<void(u64)> on_disconnected;
    Function<void(u64, SQLErrorCode, DeprecatedString const&)> on_connection_error;
    Function<void(u64, SQLErrorCode, DeprecatedString const&)> on_execution_error;
    Function<void(u64, bool, size_t, size_t, size_t)> on_execution_success;
    Function<void(u64, Vector<DeprecatedString> const&)> on_next_result;
    Function<void(u64, size_t)> on_results_exhausted;

private:
    SQLClient(NonnullOwnPtr<Core::Stream::LocalSocket> socket)
        : IPC::ConnectionToServer<SQLClientEndpoint, SQLServerEndpoint>(*this, move(socket))
    {
    }

    virtual void connected(u64 connection_id, DeprecatedString const& connected_to_database) override;
    virtual void connection_error(u64 connection_id, SQLErrorCode const& code, DeprecatedString const& message) override;
    virtual void execution_success(u64 statement_id, bool has_results, size_t created, size_t updated, size_t deleted) override;
    virtual void next_result(u64 statement_id, Vector<DeprecatedString> const&) override;
    virtual void results_exhausted(u64 statement_id, size_t total_rows) override;
    virtual void execution_error(u64 statement_id, SQLErrorCode const& code, DeprecatedString const& message) override;
    virtual void disconnected(u64 connection_id) override;
};

}
