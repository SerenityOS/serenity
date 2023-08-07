/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>
#include <AK/Vector.h>
#include <LibSQL/AST/AST.h>
#include <LibSQL/Result.h>
#include <LibSQL/ResultSet.h>
#include <LibSQL/Type.h>
#include <SQLServer/DatabaseConnection.h>
#include <SQLServer/Forward.h>

namespace SQLServer {

class SQLStatement final : public RefCounted<SQLStatement> {
public:
    static SQL::ResultOr<NonnullRefPtr<SQLStatement>> create(DatabaseConnection&, StringView sql);

    static RefPtr<SQLStatement> statement_for(SQL::StatementID statement_id);
    SQL::StatementID statement_id() const { return m_statement_id; }
    DatabaseConnection& connection() { return m_connection; }
    Optional<SQL::ExecutionID> execute(Vector<SQL::Value> placeholder_values);

private:
    SQLStatement(DatabaseConnection&, NonnullRefPtr<SQL::AST::Statement> statement);

    bool should_send_result_rows(SQL::ResultSet const& result) const;
    void next(SQL::ExecutionID execution_id, SQL::ResultSet result, size_t result_size);
    void report_error(SQL::Result, SQL::ExecutionID execution_id);

    DatabaseConnection& m_connection;
    SQL::StatementID m_statement_id { 0 };

    HashTable<SQL::ExecutionID> m_ongoing_executions;
    SQL::ExecutionID m_next_execution_id { 0 };

    NonnullRefPtr<SQL::AST::Statement> m_statement;
};

}
