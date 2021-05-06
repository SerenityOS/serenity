/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibSQL/Key.h>
#include <LibSQL/Meta.h>
#include <LibSQL/Type.h>

namespace SQL {

ColumnDef::ColumnDef(Relation *parent, size_t column_number, String name, SQLType sql_type)
    : Relation(move(name), parent)
    , m_index(column_number)
    , m_type(sql_type)
{
}

Key ColumnDef::key() const {
    auto key = Key(index_def());

    key["table_name"] = parent()->name();
    key["column_number"] = (int) column_number();
    key["column_name"] = name();
    key["column_type"] = (int) type();
    key["column_hash"] = (int) hash();
    VERIFY((int) key["column_hash"] == (int) hash());
    return key;
}

Key ColumnDef::get_column_key(String table_name)
{
    Key key(*index_def());
    key["table_name"] = move(table_name);
    return key;
}

NonnullRefPtr<IndexDef> ColumnDef::s_index_def = IndexDef::construct("$columns", true, 0);

NonnullRefPtr<IndexDef> ColumnDef::index_def()
{
  if (!s_index_def->size()) {
    s_index_def->append_column("table_name", SQLType::Text, SortOrder::Ascending);
    s_index_def->append_column("column_number", SQLType::Integer, SortOrder::Ascending);
    s_index_def->append_column("column_name", SQLType::Text, SortOrder::Ascending);
    s_index_def->append_column("column_type", SQLType::Integer, SortOrder::Ascending);
    s_index_def->append_column("column_hash", SQLType::Integer, SortOrder::Ascending);
  }
  return s_index_def;
}


KeyPartDef::KeyPartDef(IndexDef *index, String name, SQLType sql_type, SortOrder sort_order)
    : ColumnDef(index, index->size(), move(name), sql_type)
    , m_sort_order(sort_order)
{
}


IndexDef::IndexDef(TableDef *table, String name, bool unique, u32 pointer)
    : Relation(move(name), pointer, table)
    , m_key()
    , m_unique(unique)
{
}

IndexDef::IndexDef(String name, bool unique, u32 pointer)
    : IndexDef(nullptr, move(name), unique, pointer)
{
}

void IndexDef::append_column(String name, SQLType sql_type, SortOrder sort_order)
{
    auto part = KeyPartDef::construct(this, move(name), sql_type, sort_order);
    m_key.append(part);
}


TableDef::TableDef(String name)
    : Relation(move(name))
    , m_columns()
    , m_indexes()
{
}

TableDef::TableDef(Key const& key)
    : Relation((String) key[0], key.pointer())
    , m_columns()
    , m_indexes()
{
    set_hash((int) key[1]);
}

Key TableDef::key() const {
    auto key = Key(index_def());
    key["table_hash"] = (int) hash();
    key["table_name"] = name();
    VERIFY((int) key["table_hash"] == (int) hash());
    key.pointer(pointer());
    return key;
}

void TableDef::append_column(String name, SQLType sql_type)
{
    auto column = ColumnDef::construct(this, num_columns(), move(name), sql_type);
    m_columns.append(column);
}

Key TableDef::make_table_key(String name)
{
    Key key(*index_def());
    key["table_name"] = move(name);
    return key;
}

NonnullRefPtr<IndexDef> TableDef::s_index_def = IndexDef::construct("$tables", true, 0);

NonnullRefPtr<IndexDef> TableDef::index_def()
{
    if (!s_index_def->size()) {
        s_index_def->append_column("table_name", SQLType::Text, SortOrder::Ascending);
        s_index_def->append_column("table_hash", SQLType::Integer, SortOrder::Ascending);
    }
    return s_index_def;
}

}
