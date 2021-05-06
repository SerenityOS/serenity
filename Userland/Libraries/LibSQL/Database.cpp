/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefPtr.h>
#include <AK/String.h>

#include <LibSQL/BTree.h>
#include <LibSQL/Database.h>
#include <LibSQL/Heap.h>
#include <LibSQL/Key.h>
#include <LibSQL/Meta.h>
#include <LibSQL/Tuple.h>

namespace SQL {

Database::Database(String name)
{
    m_heap = Heap::construct(move(name));
    m_tables = BTree::construct(*m_heap, TableDef::index_def(), m_heap->tables_root());
    m_tables->on_new_root = [&]() {
        m_heap->set_tables_root(m_tables->root());
    };
    m_table_columns = BTree::construct(*m_heap, ColumnDef::index_def(), m_heap->table_columns_root());
    m_table_columns->on_new_root = [&]() {
        m_heap->set_table_columns_root(m_table_columns->root());
    };
}

void Database::commit() {
    m_heap->flush();
}

void Database::add_table(TableDef& table)
{
    m_tables->insert(table.key());
    for (auto &column : table.columns()) {
        m_table_columns->insert(column.key());
    }
}

Optional<Key> Database::get_table_key(String table_name)
{
    auto n = move(table_name);
    auto key = TableDef::make_table_key(n);
    auto table_iterator = m_tables->find(key);
    if (table_iterator.is_end() || (String) ((*table_iterator)[0]) != n) {
        warnln("Table {} not found", n);
        return {};
    }
    return *table_iterator;
}

RefPtr<TableDef> Database::get_table(String name)
{
    auto n = move(name);
    auto optional_iter = get_table_key(n);
    if (!optional_iter.has_value()) {
        return nullptr;
    }
    auto ret = TableDef::construct(optional_iter.value());
    auto key = ColumnDef::get_column_key(n);

    for (auto column_iterator = m_table_columns->find(key);
         !column_iterator.is_end() && (((String) (*column_iterator)["table_name"]) == n);
         column_iterator++) {
        ret->append_column(
            (String)(*column_iterator)["column_name"],
            (SQLType)((int)(*column_iterator)["column_type"]));
    }
    return ret;
}

Vector<Tuple> Database::select_all(TableDef const& table)
{
    Vector<Tuple> ret;
    auto optional_iter = get_table_key(table.name());
    if (!optional_iter.has_value()) {
        return ret;
    }
    for (auto pointer = optional_iter.value().pointer(); pointer; ) {
        auto buffer_or_error = m_heap->read_block(pointer);
        if (buffer_or_error.is_error()) {
            VERIFY_NOT_REACHED();
        }
        ret.empend(table, pointer, buffer_or_error.value());
        pointer = ret.last().next_pointer();
    }
    return ret;
}

Vector<Tuple> Database::match(TableDef const& table, Key const& key)
{
    Vector<Tuple> ret;
    auto optional_iter = get_table_key(table.name());
    if (!optional_iter.has_value()) {
        return ret;
    }

    // TODO Match key against indexes defined on table. If found,
    // use the index instead of scanning the table.
    for (auto pointer = optional_iter.value().pointer(); pointer; ) {
        auto buffer_or_error = m_heap->read_block(pointer);
        if (buffer_or_error.is_error()) {
            VERIFY_NOT_REACHED();
        }
        Tuple tuple(table, pointer, buffer_or_error.value());
        if (tuple.match(key)) {
            ret.append(tuple);
        }
        pointer = ret.last().next_pointer();
    }
    return ret;
}

bool Database::insert(Tuple& tuple)
{
    tuple.pointer(m_heap->new_record_pointer());
    tuple.next_pointer(tuple.table()->pointer());
    update(tuple);

    // TODO update indexes defined on table.

    auto table_key = tuple.table()->key();
    auto table_iter = m_tables->find(table_key);
    table_key.pointer(tuple.pointer());
    table_iter.update(table_key);
    tuple.table()->set_pointer(tuple.pointer());
    return true;
}

bool Database::update(Tuple& tuple)
{
    ByteBuffer buffer;
    tuple.serialize(buffer);
    m_heap->add_to_wal(tuple.pointer(), buffer);

    // TODO update indexes defined on table.
    return true;
}

}
