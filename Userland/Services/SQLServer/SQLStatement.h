/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/NonnullRefPtr.h>
#include <AK/Vector.h>
#include <LibCore/Object.h>
#include <LibSQL/AST/AST.h>
#include <LibSQL/Result.h>
#include <LibSQL/ResultSet.h>
#include <SQLServer/DatabaseConnection.h>
#include <SQLServer/Forward.h>

namespace SQLServer {

class SQLStatement final : public Core::Object {
    C_OBJECT_ABSTRACT(SQLStatement)

public:
    static SQL::ResultOr<NonnullRefPtr<SQLStatement>> create(DatabaseConnection&, StringView sql);
    ~SQLStatement() override = default;

    static RefPtr<SQLStatement> statement_for(u64 statement_id);
    u64 statement_id() const { return m_statement_id; }
    DatabaseConnection* connection() { return dynamic_cast<DatabaseConnection*>(parent()); }
    Optional<u64> execute(Vector<SQL::Value> placeholder_values);

private:
    SQLStatement(DatabaseConnection&, NonnullRefPtr<SQL::AST::Statement> statement);

    bool should_send_result_rows(SQL::ResultSet const& result) const;
    void next(u64 execution_id, SQL::ResultSet result, size_t result_size);
    void report_error(SQL::Result, u64 execution_id);

    u64 m_statement_id { 0 };

    HashTable<u64> m_ongoing_executions;
    u64 m_next_execution_id { 0 };

    NonnullRefPtr<SQL::AST::Statement> m_statement;
};

}
