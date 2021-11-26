/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibSQL/SQLClient.h>

namespace SQL {

SQLClient::~SQLClient()
{
}

void SQLClient::connected(int connection_id, String const& connected_to_database)
{
    if (on_connected)
        on_connected(connection_id, connected_to_database);
}

void SQLClient::disconnected(int connection_id)
{
    if (on_disconnected)
        on_disconnected(connection_id);
}

void SQLClient::connection_error(int connection_id, int code, String const& message)
{
    if (on_connection_error)
        on_connection_error(connection_id, code, message);
    else
        warnln("Connection error for connection_id {}: {} ({})", connection_id, message, code);
}

void SQLClient::execution_error(int statement_id, int code, String const& message)
{
    if (on_execution_error)
        on_execution_error(statement_id, code, message);
    else
        warnln("Execution error for statement_id {}: {} ({})", statement_id, message, code);
}

void SQLClient::execution_success(int statement_id, bool has_results, int created, int updated, int deleted)
{
    if (on_execution_success)
        on_execution_success(statement_id, has_results, created, updated, deleted);
    else
        outln("{} row(s) created, {} updated, {} deleted", created, updated, deleted);
}

void SQLClient::next_result(int statement_id, Vector<String> const& row)
{
    if (on_next_result) {
        on_next_result(statement_id, row);
        return;
    }
    bool first = true;
    for (auto& column : row) {
        if (!first)
            out(", ");
        out("\"{}\"", column);
        first = false;
    }
    outln();
}

void SQLClient::results_exhausted(int statement_id, int total_rows)
{
    if (on_results_exhausted)
        on_results_exhausted(statement_id, total_rows);
    else
        outln("{} total row(s)", total_rows);
}

}
