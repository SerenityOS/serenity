/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 * Copyright (c) 2021, Mahmoud Mandour <ma.mandourr@gmail.com>
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
    : m_heap(Heap::construct(move(name)))
    , m_serializer(m_heap)
{
}

ResultOr<void> Database::open()
{
    TRY(m_heap->open());

    m_schemas = BTree::construct(m_serializer, SchemaDef::index_def()->to_tuple_descriptor(), m_heap->schemas_root());
    m_schemas->on_new_root = [&]() {
        m_heap->set_schemas_root(m_schemas->root());
    };

    m_tables = BTree::construct(m_serializer, TableDef::index_def()->to_tuple_descriptor(), m_heap->tables_root());
    m_tables->on_new_root = [&]() {
        m_heap->set_tables_root(m_tables->root());
    };

    m_table_columns = BTree::construct(m_serializer, ColumnDef::index_def()->to_tuple_descriptor(), m_heap->table_columns_root());
    m_table_columns->on_new_root = [&]() {
        m_heap->set_table_columns_root(m_table_columns->root());
    };

    m_open = true;

    auto ensure_schema_exists = [&](auto schema_name) -> ResultOr<NonnullRefPtr<SchemaDef>> {
        if (auto result = get_schema(schema_name); result.is_error()) {
            if (result.error().error() != SQLErrorCode::SchemaDoesNotExist)
                return result.release_error();

            auto schema_def = SchemaDef::construct(schema_name);
            TRY(add_schema(*schema_def));
            return schema_def;
        } else {
            return result.release_value();
        }
    };

    (void)TRY(ensure_schema_exists("default"sv));
    auto master_schema = TRY(ensure_schema_exists("master"sv));

    auto table_def = TRY(get_table("master", "internal_describe_table"));
    if (!table_def) {
        auto describe_internal_table = TableDef::construct(master_schema, "internal_describe_table");
        describe_internal_table->append_column("Name", SQLType::Text);
        describe_internal_table->append_column("Type", SQLType::Text);
        TRY(add_table(*describe_internal_table));
    }

    return {};
}

Database::~Database() = default;

ErrorOr<void> Database::commit()
{
    VERIFY(is_open());
    TRY(m_heap->flush());
    return {};
}

ResultOr<void> Database::add_schema(SchemaDef const& schema)
{
    VERIFY(is_open());

    if (!m_schemas->insert(schema.key()))
        return Result { SQLCommand::Unknown, SQLErrorCode::SchemaExists, schema.name() };
    return {};
}

Key Database::get_schema_key(String const& schema_name)
{
    auto key = SchemaDef::make_key();
    key["schema_name"] = schema_name;
    return key;
}

ResultOr<NonnullRefPtr<SchemaDef>> Database::get_schema(String const& schema)
{
    VERIFY(is_open());

    auto schema_name = schema;
    if (schema.is_empty())
        schema_name = "default"sv;

    Key key = get_schema_key(schema_name);
    if (auto it = m_schema_cache.find(key.hash()); it != m_schema_cache.end())
        return it->value;

    auto schema_iterator = m_schemas->find(key);
    if (schema_iterator.is_end() || (*schema_iterator != key))
        return Result { SQLCommand::Unknown, SQLErrorCode::SchemaDoesNotExist, schema_name };

    auto schema_def = SchemaDef::construct(*schema_iterator);
    m_schema_cache.set(key.hash(), schema_def);
    return schema_def;
}

ErrorOr<void> Database::add_table(TableDef& table)
{
    VERIFY(is_open());
    if (!m_tables->insert(table.key())) {
        warnln("Duplicate table name '{}'.'{}'"sv, table.parent()->name(), table.name());
        return Error::from_string_literal("Duplicate table name");
    }
    for (auto& column : table.columns()) {
        VERIFY(m_table_columns->insert(column.key()));
    }
    return {};
}

Key Database::get_table_key(String const& schema_name, String const& table_name)
{
    auto key = TableDef::make_key(get_schema_key(schema_name));
    key["table_name"] = table_name;
    return key;
}

ResultOr<RefPtr<TableDef>> Database::get_table(String const& schema, String const& name)
{
    VERIFY(is_open());
    auto schema_name = schema;
    if (schema.is_null() || schema.is_empty())
        schema_name = "default";
    Key key = get_table_key(schema_name, name);
    auto table_def_opt = m_table_cache.get(key.hash());
    if (table_def_opt.has_value())
        return RefPtr<TableDef>(table_def_opt.value());
    auto table_iterator = m_tables->find(key);
    if (table_iterator.is_end() || (*table_iterator != key)) {
        return RefPtr<TableDef>(nullptr);
    }
    auto schema_def = TRY(get_schema(schema));
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
    return RefPtr<TableDef>(ret);
}

ErrorOr<Vector<Row>> Database::select_all(TableDef const& table)
{
    VERIFY(m_table_cache.get(table.key().hash()).has_value());
    Vector<Row> ret;
    for (auto pointer = table.pointer(); pointer; pointer = ret.last().next_pointer()) {
        ret.append(m_serializer.deserialize_block<Row>(pointer, table, pointer));
    }
    return ret;
}

ErrorOr<Vector<Row>> Database::match(TableDef const& table, Key const& key)
{
    VERIFY(m_table_cache.get(table.key().hash()).has_value());
    Vector<Row> ret;

    // TODO Match key against indexes defined on table. If found,
    // use the index instead of scanning the table.
    for (auto pointer = table.pointer(); pointer;) {
        auto row = m_serializer.deserialize_block<Row>(pointer, table, pointer);
        if (row.match(key))
            ret.append(row);
        pointer = ret.last().next_pointer();
    }
    return ret;
}

ErrorOr<void> Database::insert(Row& row)
{
    VERIFY(m_table_cache.get(row.table()->key().hash()).has_value());
    // TODO Check constraints

    row.set_pointer(m_heap->new_record_pointer());
    row.set_next_pointer(row.table()->pointer());
    TRY(update(row));

    // TODO update indexes defined on table.

    auto table_key = row.table()->key();
    table_key.set_pointer(row.pointer());
    VERIFY(m_tables->update_key_pointer(table_key));
    row.table()->set_pointer(row.pointer());
    return {};
}

ErrorOr<void> Database::update(Row& tuple)
{
    VERIFY(m_table_cache.get(tuple.table()->key().hash()).has_value());
    // TODO Check constraints
    m_serializer.reset();
    m_serializer.serialize_and_write<Tuple>(tuple);

    // TODO update indexes defined on table.
    return {};
}

}
