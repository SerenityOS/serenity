/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 * Copyright (c) 2021, Mahmoud Mandour <ma.mandourr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteString.h>
#include <LibSQL/BTree.h>
#include <LibSQL/Database.h>
#include <LibSQL/Heap.h>
#include <LibSQL/Meta.h>
#include <LibSQL/Row.h>
#include <LibSQL/Tuple.h>

namespace SQL {

ErrorOr<NonnullRefPtr<Database>> Database::create(ByteString name)
{
    auto heap = TRY(Heap::create(move(name)));
    return adopt_nonnull_ref_or_enomem(new (nothrow) Database(move(heap)));
}

Database::Database(NonnullRefPtr<Heap> heap)
    : m_heap(move(heap))
    , m_serializer(m_heap)
{
}

ResultOr<void> Database::open()
{
    VERIFY(!m_open);
    TRY(m_heap->open());

    m_schemas = TRY(BTree::create(m_serializer, SchemaDef::index_def()->to_tuple_descriptor(), m_heap->schemas_root()));
    m_schemas->on_new_root = [&]() {
        m_heap->set_schemas_root(m_schemas->root());
    };

    m_tables = TRY(BTree::create(m_serializer, TableDef::index_def()->to_tuple_descriptor(), m_heap->tables_root()));
    m_tables->on_new_root = [&]() {
        m_heap->set_tables_root(m_tables->root());
    };

    m_table_columns = TRY(BTree::create(m_serializer, ColumnDef::index_def()->to_tuple_descriptor(), m_heap->table_columns_root()));
    m_table_columns->on_new_root = [&]() {
        m_heap->set_table_columns_root(m_table_columns->root());
    };

    m_open = true;

    auto ensure_schema_exists = [&](auto schema_name) -> ResultOr<NonnullRefPtr<SchemaDef>> {
        if (auto result = get_schema(schema_name); result.is_error()) {
            if (result.error().error() != SQLErrorCode::SchemaDoesNotExist)
                return result.release_error();

            auto schema_def = TRY(SchemaDef::create(schema_name));
            TRY(add_schema(*schema_def));
            return schema_def;
        } else {
            return result.release_value();
        }
    };

    (void)TRY(ensure_schema_exists("default"sv));
    auto master_schema = TRY(ensure_schema_exists("master"sv));

    if (auto result = get_table("master"sv, "internal_describe_table"sv); result.is_error()) {
        if (result.error().error() != SQLErrorCode::TableDoesNotExist)
            return result.release_error();

        auto internal_describe_table = TRY(TableDef::create(master_schema, "internal_describe_table"));
        internal_describe_table->append_column("Name", SQLType::Text);
        internal_describe_table->append_column("Type", SQLType::Text);
        TRY(add_table(*internal_describe_table));
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

Key Database::get_schema_key(ByteString const& schema_name)
{
    auto key = SchemaDef::make_key();
    key["schema_name"] = schema_name;
    return key;
}

ResultOr<NonnullRefPtr<SchemaDef>> Database::get_schema(ByteString const& schema)
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

    auto schema_def = TRY(SchemaDef::create(*schema_iterator));
    m_schema_cache.set(key.hash(), schema_def);
    return schema_def;
}

ResultOr<void> Database::add_table(TableDef& table)
{
    VERIFY(is_open());

    if (!m_tables->insert(table.key()))
        return Result { SQLCommand::Unknown, SQLErrorCode::TableExists, table.name() };

    for (auto& column : table.columns()) {
        if (!m_table_columns->insert(column->key()))
            VERIFY_NOT_REACHED();
    }

    return {};
}

Key Database::get_table_key(ByteString const& schema_name, ByteString const& table_name)
{
    auto key = TableDef::make_key(get_schema_key(schema_name));
    key["table_name"] = table_name;
    return key;
}

ResultOr<NonnullRefPtr<TableDef>> Database::get_table(ByteString const& schema, ByteString const& name)
{
    VERIFY(is_open());

    auto schema_name = schema;
    if (schema.is_empty())
        schema_name = "default"sv;

    Key key = get_table_key(schema_name, name);
    if (auto it = m_table_cache.find(key.hash()); it != m_table_cache.end())
        return it->value;

    auto table_iterator = m_tables->find(key);
    if (table_iterator.is_end() || (*table_iterator != key))
        return Result { SQLCommand::Unknown, SQLErrorCode::TableDoesNotExist, ByteString::formatted("{}.{}", schema_name, name) };

    auto schema_def = TRY(get_schema(schema));
    auto table_def = TRY(TableDef::create(schema_def, name));
    table_def->set_block_index((*table_iterator).block_index());
    m_table_cache.set(key.hash(), table_def);

    auto table_hash = table_def->hash();
    auto column_key = ColumnDef::make_key(table_def);
    for (auto it = m_table_columns->find(column_key); !it.is_end() && ((*it)["table_hash"].to_int<u32>() == table_hash); ++it)
        table_def->append_column(*it);

    return table_def;
}

ErrorOr<Vector<Row>> Database::select_all(TableDef& table)
{
    VERIFY(m_table_cache.get(table.key().hash()).has_value());
    Vector<Row> ret;
    for (auto block_index = table.block_index(); block_index; block_index = ret.last().next_block_index())
        ret.append(m_serializer.deserialize_block<Row>(block_index, table, block_index));
    return ret;
}

ErrorOr<Vector<Row>> Database::match(TableDef& table, Key const& key)
{
    VERIFY(m_table_cache.get(table.key().hash()).has_value());
    Vector<Row> ret;

    // TODO Match key against indexes defined on table. If found,
    // use the index instead of scanning the table.
    for (auto block_index = table.block_index(); block_index;) {
        auto row = m_serializer.deserialize_block<Row>(block_index, table, block_index);
        if (row.match(key))
            ret.append(row);
        block_index = ret.last().next_block_index();
    }
    return ret;
}

ErrorOr<void> Database::insert(Row& row)
{
    VERIFY(m_table_cache.get(row.table().key().hash()).has_value());
    // TODO: implement table constraints such as unique, foreign key, etc.

    row.set_block_index(m_heap->request_new_block_index());
    row.set_next_block_index(row.table().block_index());
    TRY(update(row));

    // TODO update indexes defined on table.

    auto table_key = row.table().key();
    table_key.set_block_index(row.block_index());
    VERIFY(m_tables->update_key_pointer(table_key));
    row.table().set_block_index(row.block_index());
    return {};
}

ErrorOr<void> Database::remove(Row& row)
{
    auto& table = row.table();
    VERIFY(m_table_cache.get(table.key().hash()).has_value());

    TRY(m_heap->free_storage(row.block_index()));

    if (table.block_index() == row.block_index()) {
        auto table_key = table.key();
        table_key.set_block_index(row.next_block_index());
        m_tables->update_key_pointer(table_key);

        table.set_block_index(row.next_block_index());
        return {};
    }

    for (auto block_index = table.block_index(); block_index;) {
        auto current = m_serializer.deserialize_block<Row>(block_index, table, block_index);

        if (current.next_block_index() == row.block_index()) {
            current.set_next_block_index(row.next_block_index());
            TRY(update(current));
            break;
        }

        block_index = current.next_block_index();
    }

    return {};
}

ErrorOr<void> Database::update(Row& tuple)
{
    VERIFY(m_table_cache.get(tuple.table().key().hash()).has_value());
    // TODO: implement table constraints such as unique, foreign key, etc.

    m_serializer.reset();
    m_serializer.serialize_and_write<Tuple>(tuple);

    // TODO update indexes defined on table.
    return {};
}

}
