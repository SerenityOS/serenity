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
    explicit Database(String);
    ~Database() override = default;

    void commit() { m_heap->flush(); }

    void add_schema(SchemaDef const&);
    static Key get_schema_key(String const&);
    RefPtr<SchemaDef> get_schema(String const&);

    void add_table(TableDef& table);
    static Key get_table_key(String const&, String const&);
    RefPtr<TableDef> get_table(String const&, String const&);

    Vector<Row> select_all(TableDef const&);
    Vector<Row> match(TableDef const&, Key const&);
    bool insert(Row&);
    bool update(Row&);

private:
    NonnullRefPtr<Heap> m_heap;
    Serializer m_serializer;
    RefPtr<BTree> m_schemas;
    RefPtr<BTree> m_tables;
    RefPtr<BTree> m_table_columns;

    HashMap<u32, RefPtr<SchemaDef>> m_schema_cache;
    HashMap<u32, RefPtr<TableDef>> m_table_cache;
};

}
