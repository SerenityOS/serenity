/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/Object.h>
#include <LibSQL/Database.h>
#include <LibSQL/Result.h>
#include <SQLServer/Forward.h>

namespace SQLServer {

class DatabaseConnection final : public Core::Object {
    C_OBJECT(DatabaseConnection)

public:
    ~DatabaseConnection() override = default;

    static RefPtr<DatabaseConnection> connection_for(u64 connection_id);
    u64 connection_id() const { return m_connection_id; }
    int client_id() const { return m_client_id; }
    RefPtr<SQL::Database> database() { return m_database; }
    void disconnect();
    SQL::ResultOr<u64> prepare_statement(StringView sql);

private:
    DatabaseConnection(DeprecatedString database_name, int client_id);

    RefPtr<SQL::Database> m_database { nullptr };
    DeprecatedString m_database_name;
    u64 m_connection_id { 0 };
    int m_client_id { 0 };
    bool m_accept_statements { false };
};

}
