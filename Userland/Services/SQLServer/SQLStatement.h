/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <AK/String.h>
#include <LibCore/Object.h>
#include <LibSQL/AST/AST.h>
#include <LibSQL/Result.h>
#include <LibSQL/ResultSet.h>
#include <SQLServer/DatabaseConnection.h>
#include <SQLServer/Forward.h>

namespace SQLServer {

class SQLStatement final : public Core::Object {
    C_OBJECT(SQLStatement)

public:
    ~SQLStatement() override = default;

    static RefPtr<SQLStatement> statement_for(int statement_id);
    int statement_id() const { return m_statement_id; }
    String const& sql() const { return m_sql; }
    DatabaseConnection* connection() { return dynamic_cast<DatabaseConnection*>(parent()); }
    void execute();

private:
    SQLStatement(DatabaseConnection&, String sql);
    SQL::ResultOr<void> parse();
    bool should_send_result_rows() const;
    void next();
    void report_error(SQL::Result);

    int m_statement_id;
    String m_sql;
    size_t m_index { 0 };
    RefPtr<SQL::AST::Statement> m_statement { nullptr };
    Optional<SQL::ResultSet> m_result {};
};

}
