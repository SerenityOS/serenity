/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Database.h"
#include <AK/Format.h>
#include <AK/StringView.h>

namespace Browser {

static constexpr auto database_name = "Browser"sv;

ErrorOr<NonnullRefPtr<Database>> Database::create()
{
    auto sql_client = TRY(SQL::SQLClient::try_create());
    return create(move(sql_client));
}

ErrorOr<NonnullRefPtr<Database>> Database::create(NonnullRefPtr<SQL::SQLClient> sql_client)
{
    auto connection_id = sql_client->connect(database_name);
    if (!connection_id.has_value())
        return Error::from_string_view("Could not connect to SQL database"sv);

    return adopt_nonnull_ref_or_enomem(new (nothrow) Database(move(sql_client), *connection_id));
}

Database::Database(NonnullRefPtr<SQL::SQLClient> sql_client, SQL::ConnectionID connection_id)
    : m_sql_client(move(sql_client))
    , m_connection_id(connection_id)
{
    m_sql_client->on_execution_success = [this](auto statement_id, auto execution_id, auto has_results, auto, auto, auto) {
        if (has_results)
            return;

        if (auto it = m_pending_executions.find({ statement_id, execution_id }); it != m_pending_executions.end()) {
            auto in_progress_statement = move(it->value);
            m_pending_executions.remove(it);

            if (in_progress_statement.on_complete)
                in_progress_statement.on_complete();
        }
    };

    m_sql_client->on_next_result = [this](auto statement_id, auto execution_id, auto row) {
        if (auto it = m_pending_executions.find({ statement_id, execution_id }); it != m_pending_executions.end()) {
            if (it->value.on_result)
                it->value.on_result(row);
        }
    };

    m_sql_client->on_results_exhausted = [this](auto statement_id, auto execution_id, auto) {
        if (auto it = m_pending_executions.find({ statement_id, execution_id }); it != m_pending_executions.end()) {
            auto in_progress_statement = move(it->value);
            m_pending_executions.remove(it);

            if (in_progress_statement.on_complete)
                in_progress_statement.on_complete();
        }
    };

    m_sql_client->on_execution_error = [this](auto statement_id, auto execution_id, auto, auto const& message) {
        if (auto it = m_pending_executions.find({ statement_id, execution_id }); it != m_pending_executions.end()) {
            auto in_progress_statement = move(it->value);
            m_pending_executions.remove(it);

            if (in_progress_statement.on_error)
                in_progress_statement.on_error(message);
        }
    };
}

ErrorOr<SQL::StatementID> Database::prepare_statement(StringView statement)
{
    if (auto statement_id = m_sql_client->prepare_statement(m_connection_id, statement); statement_id.has_value())
        return *statement_id;
    return Error::from_string_view(statement);
}

void Database::execute_statement(SQL::StatementID statement_id, Vector<SQL::Value> placeholder_values, PendingExecution pending_execution)
{
    Core::deferred_invoke([this, statement_id, placeholder_values = move(placeholder_values), pending_execution = move(pending_execution)]() mutable {
        auto execution_id = m_sql_client->execute_statement(statement_id, move(placeholder_values));
        if (!execution_id.has_value()) {
            if (pending_execution.on_error)
                pending_execution.on_error("Could not execute statement"sv);
            return;
        }

        m_pending_executions.set({ statement_id, *execution_id }, move(pending_execution));
    });
}

}
