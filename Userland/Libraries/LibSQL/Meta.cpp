/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibSQL/Key.h>
#include <LibSQL/Meta.h>
#include <LibSQL/Type.h>

namespace SQL {

SchemaDef::SchemaDef(String name)
    : Relation(move(name))
{
}

SchemaDef::SchemaDef(Key const& key)
    : Relation(key["schema_name"].to_string().value())
{
}

Key SchemaDef::key() const
{
    auto key = Key(index_def()->to_tuple_descriptor());
    key["schema_name"] = name();
    key.set_pointer(pointer());
    return key;
}

Key SchemaDef::make_key()
{
    return Key(index_def());
}

NonnullRefPtr<IndexDef> SchemaDef::index_def()
{
    NonnullRefPtr<IndexDef> s_index_def = IndexDef::construct("$schema", true, 0);
    if (!s_index_def->size()) {
        s_index_def->append_column("schema_name", SQLType::Text, Order::Ascending);
    }
    return s_index_def;
}

ColumnDef::ColumnDef(Relation* parent, size_t column_number, String name, SQLType sql_type)
    : Relation(move(name), parent)
    , m_index(column_number)
    , m_type(sql_type)
{
}

Key ColumnDef::key() const
{
    auto key = Key(index_def());
    key["table_hash"] = parent_relation()->hash();
    key["column_number"] = (int)column_number();
    key["column_name"] = name();
    key["column_type"] = (int)type();
    return key;
}

Key ColumnDef::make_key(TableDef const& table_def)
{
    Key key(index_def());
    key["table_hash"] = table_def.key().hash();
    return key;
}

NonnullRefPtr<IndexDef> ColumnDef::index_def()
{
    NonnullRefPtr<IndexDef> s_index_def = IndexDef::construct("$column", true, 0);
    if (!s_index_def->size()) {
        s_index_def->append_column("table_hash", SQLType::Integer, Order::Ascending);
        s_index_def->append_column("column_number", SQLType::Integer, Order::Ascending);
        s_index_def->append_column("column_name", SQLType::Text, Order::Ascending);
        s_index_def->append_column("column_type", SQLType::Integer, Order::Ascending);
    }
    return s_index_def;
}

KeyPartDef::KeyPartDef(IndexDef* index, String name, SQLType sql_type, Order sort_order)
    : ColumnDef(index, index->size(), move(name), sql_type)
    , m_sort_order(sort_order)
{
}

IndexDef::IndexDef(TableDef* table, String name, bool unique, u32 pointer)
    : Relation(move(name), pointer, table)
    , m_key_definition()
    , m_unique(unique)
{
}

IndexDef::IndexDef(String name, bool unique, u32 pointer)
    : IndexDef(nullptr, move(name), unique, pointer)
{
}

void IndexDef::append_column(String name, SQLType sql_type, Order sort_order)
{
    auto part = KeyPartDef::construct(this, move(name), sql_type, sort_order);
    m_key_definition.append(part);
}

TupleDescriptor IndexDef::to_tuple_descriptor() const
{
    TupleDescriptor ret;
    for (auto& part : m_key_definition) {
        ret.append({ part.name(), part.type(), part.sort_order() });
    }
    return ret;
}

Key IndexDef::key() const
{
    auto key = Key(index_def()->to_tuple_descriptor());
    key["table_hash"] = parent_relation()->key().hash();
    key["index_name"] = name();
    key["unique"] = unique() ? 1 : 0;
    return key;
}

Key IndexDef::make_key(TableDef const& table_def)
{
    Key key(index_def());
    key["table_hash"] = table_def.key().hash();
    return key;
}

NonnullRefPtr<IndexDef> IndexDef::index_def()
{
    NonnullRefPtr<IndexDef> s_index_def = IndexDef::construct("$index", true, 0);
    if (!s_index_def->size()) {
        s_index_def->append_column("table_hash", SQLType::Integer, Order::Ascending);
        s_index_def->append_column("index_name", SQLType::Text, Order::Ascending);
        s_index_def->append_column("unique", SQLType::Integer, Order::Ascending);
    }
    return s_index_def;
}

TableDef::TableDef(SchemaDef* schema, String name)
    : Relation(move(name), schema)
    , m_columns()
    , m_indexes()
{
}

TupleDescriptor TableDef::to_tuple_descriptor() const
{
    TupleDescriptor ret;
    for (auto& part : m_columns) {
        ret.append({ part.name(), part.type(), Order::Ascending });
    }
    return ret;
}

Key TableDef::key() const
{
    auto key = Key(index_def()->to_tuple_descriptor());
    key["schema_hash"] = parent_relation()->key().hash();
    key["table_name"] = name();
    key.set_pointer(pointer());
    return key;
}

void TableDef::append_column(String name, SQLType sql_type)
{
    auto column = ColumnDef::construct(this, num_columns(), move(name), sql_type);
    m_columns.append(column);
}

void TableDef::append_column(Key const& column)
{
    append_column(
        (String)column["column_name"],
        (SQLType)((int)column["column_type"]));
}

Key TableDef::make_key(SchemaDef const& schema_def)
{
    return TableDef::make_key(schema_def.key());
}

Key TableDef::make_key(Key const& schema_key)
{
    Key key(index_def());
    key["schema_hash"] = schema_key.hash();
    return key;
}

NonnullRefPtr<IndexDef> TableDef::index_def()
{
    NonnullRefPtr<IndexDef> s_index_def = IndexDef::construct("$table", true, 0);
    if (!s_index_def->size()) {
        s_index_def->append_column("schema_hash", SQLType::Integer, Order::Ascending);
        s_index_def->append_column("table_name", SQLType::Text, Order::Ascending);
    }
    return s_index_def;
}

}
