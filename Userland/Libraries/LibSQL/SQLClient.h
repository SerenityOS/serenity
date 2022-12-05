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

public:
    explicit SQLClient(NonnullOwnPtr<Core::Stream::LocalSocket> socket)
        : IPC::ConnectionToServer<SQLClientEndpoint, SQLServerEndpoint>(*this, move(socket))
    {
    }

    virtual ~SQLClient() = default;

    Function<void(u64, u64, SQLErrorCode, DeprecatedString const&)> on_execution_error;
    Function<void(u64, u64, bool, size_t, size_t, size_t)> on_execution_success;
    Function<void(u64, u64, Span<SQL::Value const>)> on_next_result;
    Function<void(u64, u64, size_t)> on_results_exhausted;

private:
    virtual void execution_success(u64 statement_id, u64 execution_id, bool has_results, size_t created, size_t updated, size_t deleted) override;
    virtual void next_result(u64 statement_id, u64 execution_id, Vector<SQL::Value> const&) override;
    virtual void results_exhausted(u64 statement_id, u64 execution_id, size_t total_rows) override;
    virtual void execution_error(u64 statement_id, u64 execution_id, SQLErrorCode const& code, DeprecatedString const& message) override;
};

}
