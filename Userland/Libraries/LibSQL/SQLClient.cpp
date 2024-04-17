/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteString.h>
#include <AK/ScopeGuard.h>
#include <AK/String.h>
#include <LibSQL/SQLClient.h>

namespace SQL {

void SQLClient::execution_success(u64 statement_id, u64 execution_id, Vector<ByteString> const& column_names, bool has_results, size_t created, size_t updated, size_t deleted)
{
    if (!on_execution_success) {
        outln("{} row(s) created, {} updated, {} deleted", created, updated, deleted);
        return;
    }

    ExecutionSuccess success {
        .statement_id = statement_id,
        .execution_id = execution_id,
        .column_names = move(const_cast<Vector<ByteString>&>(column_names)),
        .has_results = has_results,
        .rows_created = created,
        .rows_updated = updated,
        .rows_deleted = deleted,
    };

    on_execution_success(move(success));
}

void SQLClient::execution_error(u64 statement_id, u64 execution_id, SQLErrorCode const& code, ByteString const& message)
{
    if (!on_execution_error) {
        warnln("Execution error for statement_id {}: {} ({})", statement_id, message, to_underlying(code));
        return;
    }

    ExecutionError error {
        .statement_id = statement_id,
        .execution_id = execution_id,
        .error_code = code,
        .error_message = move(const_cast<ByteString&>(message)),
    };

    on_execution_error(move(error));
}

void SQLClient::next_result(u64 statement_id, u64 execution_id, Vector<Value> const& row)
{
    ScopeGuard guard { [&]() { async_ready_for_next_result(statement_id, execution_id); } };

    if (!on_next_result) {
        StringBuilder builder;
        builder.join(", "sv, row, "\"{}\""sv);
        outln("{}", builder.string_view());
        return;
    }

    ExecutionResult result {
        .statement_id = statement_id,
        .execution_id = execution_id,
        .values = move(const_cast<Vector<Value>&>(row)),
    };

    on_next_result(move(result));
}

void SQLClient::results_exhausted(u64 statement_id, u64 execution_id, size_t total_rows)
{
    if (!on_results_exhausted) {
        outln("{} total row(s)", total_rows);
        return;
    }

    ExecutionComplete success {
        .statement_id = statement_id,
        .execution_id = execution_id,
        .total_rows = total_rows,
    };

    on_results_exhausted(move(success));
}

}
