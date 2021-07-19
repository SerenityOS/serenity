/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/RefPtr.h>
#include <AK/String.h>

#include <LibSQL/BTree.h>
#include <LibSQL/Database.h>
#include <LibSQL/Heap.h>
#include <LibSQL/Meta.h>
#include <LibSQL/Row.h>
#include <LibSQL/Tuple.h>

namespace SQL {

Database::Database(String name)
    : m_heap(Heap::construct(name))
    , m_schemas(BTree::construct(*m_heap, SchemaDef::index_def()->to_tuple_descriptor(), m_heap->schemas_root()))
    , m_tables(BTree::construct(*m_heap, TableDef::index_def()->to_tuple_descriptor(), m_heap->tables_root()))
    , m_table_columns(BTree::construct(*m_heap, ColumnDef::index_def()->to_tuple_descriptor(), m_heap->table_columns_root()))
{
    m_schemas->on_new_root = [&]() {
        m_heap->set_schemas_root(m_schemas->root());
    };
    m_tables->on_new_root = [&]() {
        m_heap->set_tables_root(m_tables->root());
    };
    m_table_columns->on_new_root = [&]() {
        m_heap->set_table_columns_root(m_table_columns->root());
    };
    auto default_schema = get_schema("default");
    if (!default_schema) {
        default_schema = SchemaDef::construct("default");
        add_schema(*default_schema);
    }
}

void Database::add_schema(SchemaDef const& schema)
{
    m_schemas->insert(schema.key());
}

Key Database::get_schema_key(String const& schema_name)
{
    auto key = SchemaDef::make_key();
    key["schema_name"] = schema_name;
    return key;
}

RefPtr<SchemaDef> Database::get_schema(String const& schema)
{
    auto schema_name = schema;
    if (schema.is_null() || schema.is_empty())
        schema_name = "default";
    Key key = get_schema_key(schema_name);
    auto schema_def_opt = m_schema_cache.get(key.hash());
    if (schema_def_opt.has_value())
        return schema_def_opt.value();
    auto schema_iterator = m_schemas->find(key);
    if (schema_iterator.is_end() || (*schema_iterator != key)) {
        return nullptr;
    }
    auto ret = SchemaDef::construct(*schema_iterator);
    m_schema_cache.set(key.hash(), ret);
    return ret;
}

void Database::add_table(TableDef& table)
{
    m_tables->insert(table.key());
    for (auto& column : table.columns()) {
        m_table_columns->insert(column.key());
    }
}

Key Database::get_table_key(String const& schema_name, String const& table_name)
{
    auto key = TableDef::make_key(get_schema_key(schema_name));
    key["table_name"] = table_name;
    return key;
}

RefPtr<TableDef> Database::get_table(String const& schema, String const& name)
{
    auto schema_name = schema;
    if (schema.is_null() || schema.is_empty())
        schema_name = "default";
    Key key = get_table_key(schema_name, name);
    auto table_def_opt = m_table_cache.get(key.hash());
    if (table_def_opt.has_value())
        return table_def_opt.value();
    auto table_iterator = m_tables->find(key);
    if (table_iterator.is_end() || (*table_iterator != key)) {
        return nullptr;
    }
    auto schema_def = get_schema(schema);
    VERIFY(schema_def);
    auto ret = TableDef::construct(schema_def, name);
    ret->set_pointer((*table_iterator).pointer());
    m_table_cache.set(key.hash(), ret);
    auto hash = ret->hash();
    auto column_key = ColumnDef::make_key(ret);

    for (auto column_iterator = m_table_columns->find(column_key);
         !column_iterator.is_end() && ((*column_iterator)["table_hash"].to_u32().value() == hash);
         column_iterator++) {
        ret->append_column(*column_iterator);
    }
    return ret;
}

Vector<Row> Database::select_all(TableDef const& table)
{
    VERIFY(m_table_cache.get(table.key().hash()).has_value());
    Vector<Row> ret;
    for (auto pointer = table.pointer(); pointer; pointer = ret.last().next_pointer()) {
        auto buffer_or_error = m_heap->read_block(pointer);
        if (buffer_or_error.is_error())
            VERIFY_NOT_REACHED();
        ret.empend(table, pointer, buffer_or_error.value());
    }
    return ret;
}

Vector<Row> Database::match(TableDef const& table, Key const& key)
{
    VERIFY(m_table_cache.get(table.key().hash()).has_value());
    Vector<Row> ret;

    // TODO Match key against indexes defined on table. If found,
    // use the index instead of scanning the table.
    for (auto pointer = table.pointer(); pointer;) {
        auto buffer_or_error = m_heap->read_block(pointer);
        if (buffer_or_error.is_error())
            VERIFY_NOT_REACHED();
        Row row(table, pointer, buffer_or_error.value());
        if (row.match(key))
            ret.append(row);
        pointer = ret.last().next_pointer();
    }
    return ret;
}

bool Database::insert(Row& row)
{
    VERIFY(m_table_cache.get(row.table()->key().hash()).has_value());
    row.set_pointer(m_heap->new_record_pointer());
    row.next_pointer(row.table()->pointer());
    update(row);

    // TODO update indexes defined on table.

    auto table_key = row.table()->key();
    table_key.set_pointer(row.pointer());
    VERIFY(m_tables->update_key_pointer(table_key));
    row.table()->set_pointer(row.pointer());
    return true;
}

bool Database::update(Row& tuple)
{
    VERIFY(m_table_cache.get(tuple.table()->key().hash()).has_value());
    ByteBuffer buffer;
    tuple.serialize(buffer);
    m_heap->add_to_wal(tuple.pointer(), buffer);

    // TODO update indexes defined on table.
    return true;
}

}
