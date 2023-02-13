/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Platform.h>
#include <LibIPC/ConnectionToServer.h>
#include <LibSQL/Result.h>
#include <SQLServer/SQLClientEndpoint.h>
#include <SQLServer/SQLServerEndpoint.h>

namespace SQL {

struct ExecutionSuccess {
    u64 statement_id { 0 };
    u64 execution_id { 0 };

    Vector<DeprecatedString> column_names;
    bool has_results { false };
    size_t rows_created { 0 };
    size_t rows_updated { 0 };
    size_t rows_deleted { 0 };
};

struct ExecutionError {
    u64 statement_id { 0 };
    u64 execution_id { 0 };

    SQLErrorCode error_code;
    DeprecatedString error_message;
};

struct ExecutionResult {
    u64 statement_id { 0 };
    u64 execution_id { 0 };

    Vector<Value> values;
};

struct ExecutionComplete {
    u64 statement_id { 0 };
    u64 execution_id { 0 };

    size_t total_rows { 0 };
};

class SQLClient
    : public IPC::ConnectionToServer<SQLClientEndpoint, SQLServerEndpoint>
    , public SQLClientEndpoint {
    IPC_CLIENT_CONNECTION(SQLClient, "/tmp/session/%sid/portal/sql"sv)

public:
#if !defined(AK_OS_SERENITY)
    static ErrorOr<NonnullRefPtr<SQLClient>> launch_server_and_create_client(Vector<String> candidate_server_paths);
#endif

    virtual ~SQLClient() = default;

    Function<void(ExecutionSuccess)> on_execution_success;
    Function<void(ExecutionError)> on_execution_error;
    Function<void(ExecutionResult)> on_next_result;
    Function<void(ExecutionComplete)> on_results_exhausted;

private:
    explicit SQLClient(NonnullOwnPtr<Core::LocalSocket> socket)
        : IPC::ConnectionToServer<SQLClientEndpoint, SQLServerEndpoint>(*this, move(socket))
    {
    }

    virtual void execution_success(u64 statement_id, u64 execution_id, Vector<DeprecatedString> const& column_names, bool has_results, size_t created, size_t updated, size_t deleted) override;
    virtual void execution_error(u64 statement_id, u64 execution_id, SQLErrorCode const& code, DeprecatedString const& message) override;
    virtual void next_result(u64 statement_id, u64 execution_id, Vector<SQL::Value> const&) override;
    virtual void results_exhausted(u64 statement_id, u64 execution_id, size_t total_rows) override;
};

}
