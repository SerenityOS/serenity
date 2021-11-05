/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <AK/String.h>
#include <LibCore/Object.h>
#include <LibSQL/Forward.h>
#include <LibSQL/Heap.h>
#include <LibSQL/Meta.h>

namespace SQL {

/**
 * A Database object logically connects a Heap with the SQL data we want
 * to store in it. It has BTree pointers for B-Trees holding the definitions
 * of tables, columns, indexes, and other SQL objects.
 */
class Database : public Core::Object {
    C_OBJECT(Database);

public:
    ~Database() override;

    ErrorOr<void> open();
    bool is_open() const { return m_open; }
    ErrorOr<void> commit();

    ErrorOr<void> add_schema(SchemaDef const&);
    static Key get_schema_key(String const&);
    ErrorOr<RefPtr<SchemaDef>> get_schema(String const&);

    ErrorOr<void> add_table(TableDef& table);
    static Key get_table_key(String const&, String const&);
    ErrorOr<RefPtr<TableDef>> get_table(String const&, String const&);

    ErrorOr<Vector<Row>> select_all(TableDef const&);
    ErrorOr<Vector<Row>> match(TableDef const&, Key const&);
    ErrorOr<void> insert(Row&);
    ErrorOr<void> update(Row&);

private:
    explicit Database(String);

    bool m_open { false };
    NonnullRefPtr<Heap> m_heap;
    Serializer m_serializer;
    RefPtr<BTree> m_schemas;
    RefPtr<BTree> m_tables;
    RefPtr<BTree> m_table_columns;

    HashMap<u32, RefPtr<SchemaDef>> m_schema_cache;
    HashMap<u32, RefPtr<TableDef>> m_table_cache;
};

}
