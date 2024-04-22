/*
 * Copyright (c) 2022-2024, Tim Flynn <trflynn89@serenityos.org>
 * Copyright (c) 2023, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/NonnullRefPtr.h>
#include <AK/Optional.h>
#include <AK/RefCounted.h>
#include <AK/StringView.h>
#include <AK/Vector.h>
#include <LibCore/Promise.h>
#include <LibSQL/SQLClient.h>
#include <LibSQL/Type.h>
#include <LibSQL/Value.h>

namespace WebView {

class Database : public RefCounted<Database> {
    using OnResult = Function<void(ReadonlySpan<SQL::Value>)>;
    using OnComplete = Function<void()>;
    using OnError = Function<void(StringView)>;

public:
    static ErrorOr<NonnullRefPtr<Database>> create();
    static ErrorOr<NonnullRefPtr<Database>> create(NonnullRefPtr<SQL::SQLClient>);

    ErrorOr<SQL::StatementID> prepare_statement(StringView statement);

    template<typename... PlaceholderValues>
    void execute_statement(SQL::StatementID statement_id, OnResult on_result, OnComplete on_complete, OnError on_error, PlaceholderValues&&... placeholder_values)
    {
        auto sync_promise = Core::Promise<Empty>::construct();

        PendingExecution pending_execution {
            .on_result = move(on_result),
            .on_complete = [sync_promise, on_complete = move(on_complete)] {
                if (on_complete)
                    on_complete();
                sync_promise->resolve({}); },
            .on_error = [sync_promise, on_error = move(on_error)](auto message) {
                if (on_error)
                    on_error(message);
                sync_promise->resolve({}); },
        };

        Vector<SQL::Value> values { SQL::Value(forward<PlaceholderValues>(placeholder_values))... };
        execute_statement(statement_id, move(values), move(pending_execution));

        MUST(sync_promise->await());
    }

private:
    struct ExecutionKey {
        constexpr bool operator==(ExecutionKey const&) const = default;

        SQL::StatementID statement_id { 0 };
        SQL::ExecutionID execution_id { 0 };
    };

    struct PendingExecution {
        OnResult on_result { nullptr };
        OnComplete on_complete { nullptr };
        OnError on_error { nullptr };
    };

    struct ExecutionKeyTraits : public Traits<ExecutionKey> {
        static constexpr unsigned hash(ExecutionKey const& key)
        {
            return pair_int_hash(u64_hash(key.statement_id), u64_hash(key.execution_id));
        }
    };

    Database(NonnullRefPtr<SQL::SQLClient> sql_client, SQL::ConnectionID connection_id);
    void execute_statement(SQL::StatementID statement_id, Vector<SQL::Value> placeholder_values, PendingExecution pending_execution);

    template<typename ResultData>
    auto take_pending_execution(ResultData const& result_data)
    {
        return m_pending_executions.take({ result_data.statement_id, result_data.execution_id });
    }

    NonnullRefPtr<SQL::SQLClient> m_sql_client;
    SQL::ConnectionID m_connection_id { 0 };

    HashMap<ExecutionKey, PendingExecution, ExecutionKeyTraits> m_pending_executions;
};

}
