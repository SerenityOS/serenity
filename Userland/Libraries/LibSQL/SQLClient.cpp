/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibSQL/SQLClient.h>

namespace SQL {

void SQLClient::execution_error(u64 statement_id, u64 execution_id, SQLErrorCode const& code, DeprecatedString const& message)
{
    if (on_execution_error)
        on_execution_error(statement_id, execution_id, code, message);
    else
        warnln("Execution error for statement_id {}: {} ({})", statement_id, message, to_underlying(code));
}

void SQLClient::execution_success(u64 statement_id, u64 execution_id, bool has_results, size_t created, size_t updated, size_t deleted)
{
    if (on_execution_success)
        on_execution_success(statement_id, execution_id, has_results, created, updated, deleted);
    else
        outln("{} row(s) created, {} updated, {} deleted", created, updated, deleted);
}

void SQLClient::next_result(u64 statement_id, u64 execution_id, Vector<SQL::Value> const& row)
{
    if (on_next_result) {
        on_next_result(statement_id, execution_id, row);
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

void SQLClient::results_exhausted(u64 statement_id, u64 execution_id, size_t total_rows)
{
    if (on_results_exhausted)
        on_results_exhausted(statement_id, execution_id, total_rows);
    else
        outln("{} total row(s)", total_rows);
}

}
