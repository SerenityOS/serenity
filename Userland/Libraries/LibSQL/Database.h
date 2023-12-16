/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 * Copyright (c) 2021, Mahmoud Mandour <ma.mandourr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefPtr.h>
#include <LibSQL/Forward.h>
#include <LibSQL/Heap.h>
#include <LibSQL/Meta.h>
#include <LibSQL/Result.h>
#include <LibSQL/Serializer.h>

namespace SQL {

/**
 * A Database object logically connects a Heap with the SQL data we want
 * to store in it. It has BTree pointers for B-Trees holding the definitions
 * of tables, columns, indexes, and other SQL objects.
 */
class Database : public RefCounted<Database> {
public:
    static ErrorOr<NonnullRefPtr<Database>> create(ByteString);
    ~Database();

    ResultOr<void> open();
    bool is_open() const { return m_open; }
    ErrorOr<void> commit();
    ErrorOr<size_t> file_size_in_bytes() const { return m_heap->file_size_in_bytes(); }

    ResultOr<void> add_schema(SchemaDef const&);
    static Key get_schema_key(ByteString const&);
    ResultOr<NonnullRefPtr<SchemaDef>> get_schema(ByteString const&);

    ResultOr<void> add_table(TableDef& table);
    static Key get_table_key(ByteString const&, ByteString const&);
    ResultOr<NonnullRefPtr<TableDef>> get_table(ByteString const&, ByteString const&);

    ErrorOr<Vector<Row>> select_all(TableDef&);
    ErrorOr<Vector<Row>> match(TableDef&, Key const&);
    ErrorOr<void> insert(Row&);
    ErrorOr<void> remove(Row&);
    ErrorOr<void> update(Row&);

private:
    explicit Database(NonnullRefPtr<Heap>);

    bool m_open { false };
    NonnullRefPtr<Heap> m_heap;
    Serializer m_serializer;
    RefPtr<BTree> m_schemas;
    RefPtr<BTree> m_tables;
    RefPtr<BTree> m_table_columns;

    HashMap<u32, NonnullRefPtr<SchemaDef>> m_schema_cache;
    HashMap<u32, NonnullRefPtr<TableDef>> m_table_cache;
};

}
