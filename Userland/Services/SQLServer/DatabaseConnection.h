/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/Object.h>
#include <LibSQL/Database.h>
#include <SQLServer/Forward.h>

namespace SQLServer {

class DatabaseConnection final : public Core::Object {
    C_OBJECT(DatabaseConnection)

public:
    ~DatabaseConnection() override = default;

    static RefPtr<DatabaseConnection> connection_for(int connection_id);
    int connection_id() const { return m_connection_id; }
    int client_id() const { return m_client_id; }
    RefPtr<SQL::Database> database() { return m_database; }
    void disconnect();
    int sql_statement(String const& sql);

private:
    DatabaseConnection(String database_name, int client_id);

    RefPtr<SQL::Database> m_database { nullptr };
    String m_database_name;
    int m_connection_id;
    int m_client_id;
    bool m_accept_statements { false };
};

}
