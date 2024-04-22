/*
 * Copyright (c) 2022-2024, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <LibWebView/Database.h>

namespace WebView {

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
    m_sql_client->on_execution_success = [this](auto result) {
        if (result.has_results)
            return;

        if (auto in_progress_statement = take_pending_execution(result); in_progress_statement.has_value()) {
            if (in_progress_statement->on_complete)
                in_progress_statement->on_complete();
        }
    };

    m_sql_client->on_next_result = [this](auto result) {
        if (auto in_progress_statement = take_pending_execution(result); in_progress_statement.has_value()) {
            if (in_progress_statement->on_result)
                in_progress_statement->on_result(result.values);

            m_pending_executions.set({ result.statement_id, result.execution_id }, in_progress_statement.release_value());
        }
    };

    m_sql_client->on_results_exhausted = [this](auto result) {
        if (auto in_progress_statement = take_pending_execution(result); in_progress_statement.has_value()) {
            if (in_progress_statement->on_complete)
                in_progress_statement->on_complete();
        }
    };

    m_sql_client->on_execution_error = [this](auto result) {
        if (auto in_progress_statement = take_pending_execution(result); in_progress_statement.has_value()) {
            if (in_progress_statement->on_error)
                in_progress_statement->on_error(result.error_message);
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
