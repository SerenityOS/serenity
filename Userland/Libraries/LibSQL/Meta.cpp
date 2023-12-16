/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibSQL/Key.h>
#include <LibSQL/Meta.h>
#include <LibSQL/Type.h>

namespace SQL {

u32 Relation::hash() const
{
    return key().hash();
}

ErrorOr<NonnullRefPtr<SchemaDef>> SchemaDef::create(ByteString name)
{
    return adopt_nonnull_ref_or_enomem(new (nothrow) SchemaDef(move(name)));
}

ErrorOr<NonnullRefPtr<SchemaDef>> SchemaDef::create(Key const& key)
{
    return create(key["schema_name"].to_byte_string());
}

SchemaDef::SchemaDef(ByteString name)
    : Relation(move(name))
{
}

Key SchemaDef::key() const
{
    auto key = Key(index_def()->to_tuple_descriptor());
    key["schema_name"] = name();
    key.set_block_index(block_index());
    return key;
}

Key SchemaDef::make_key()
{
    return Key(index_def());
}

NonnullRefPtr<IndexDef> SchemaDef::index_def()
{
    NonnullRefPtr<IndexDef> s_index_def = IndexDef::create("$schema", true, 0).release_value_but_fixme_should_propagate_errors();
    if (!s_index_def->size()) {
        s_index_def->append_column("schema_name", SQLType::Text, Order::Ascending);
    }
    return s_index_def;
}

ErrorOr<NonnullRefPtr<ColumnDef>> ColumnDef::create(Relation* parent, size_t column_number, ByteString name, SQLType sql_type)
{
    return adopt_nonnull_ref_or_enomem(new (nothrow) ColumnDef(parent, column_number, move(name), sql_type));
}

ColumnDef::ColumnDef(Relation* parent, size_t column_number, ByteString name, SQLType sql_type)
    : Relation(move(name), parent)
    , m_index(column_number)
    , m_type(sql_type)
    , m_default(Value(sql_type))
{
}

Key ColumnDef::key() const
{
    auto key = Key(index_def());
    key["table_hash"] = parent()->hash();
    key["column_number"] = column_number();
    key["column_name"] = name();
    key["column_type"] = to_underlying(type());
    return key;
}

void ColumnDef::set_default_value(Value const& default_value)
{
    VERIFY(default_value.type() == type());
    m_default = default_value;
}

Key ColumnDef::make_key(TableDef const& table_def)
{
    Key key(index_def());
    key["table_hash"] = table_def.key().hash();
    return key;
}

NonnullRefPtr<IndexDef> ColumnDef::index_def()
{
    NonnullRefPtr<IndexDef> s_index_def = IndexDef::create("$column", true, 0).release_value_but_fixme_should_propagate_errors();
    if (!s_index_def->size()) {
        s_index_def->append_column("table_hash", SQLType::Integer, Order::Ascending);
        s_index_def->append_column("column_number", SQLType::Integer, Order::Ascending);
        s_index_def->append_column("column_name", SQLType::Text, Order::Ascending);
        s_index_def->append_column("column_type", SQLType::Integer, Order::Ascending);
    }
    return s_index_def;
}

ErrorOr<NonnullRefPtr<KeyPartDef>> KeyPartDef::create(IndexDef* index, ByteString name, SQLType sql_type, Order sort_order)
{
    return adopt_nonnull_ref_or_enomem(new (nothrow) KeyPartDef(index, move(name), sql_type, sort_order));
}

KeyPartDef::KeyPartDef(IndexDef* index, ByteString name, SQLType sql_type, Order sort_order)
    : ColumnDef(index, index->size(), move(name), sql_type)
    , m_sort_order(sort_order)
{
}

ErrorOr<NonnullRefPtr<IndexDef>> IndexDef::create(TableDef* table, ByteString name, bool unique, u32 pointer)
{
    return adopt_nonnull_ref_or_enomem(new (nothrow) IndexDef(table, move(name), unique, pointer));
}

ErrorOr<NonnullRefPtr<IndexDef>> IndexDef::create(ByteString name, bool unique, u32 pointer)
{
    return create(nullptr, move(name), unique, pointer);
}

IndexDef::IndexDef(TableDef* table, ByteString name, bool unique, u32 pointer)
    : Relation(move(name), pointer, table)
    , m_key_definition()
    , m_unique(unique)
{
}

void IndexDef::append_column(ByteString name, SQLType sql_type, Order sort_order)
{
    auto part = KeyPartDef::create(this, move(name), sql_type, sort_order).release_value_but_fixme_should_propagate_errors();
    m_key_definition.append(part);
}

NonnullRefPtr<TupleDescriptor> IndexDef::to_tuple_descriptor() const
{
    NonnullRefPtr<TupleDescriptor> ret = adopt_ref(*new TupleDescriptor);
    for (auto& part : m_key_definition) {
        ret->append({ "", "", part->name(), part->type(), part->sort_order() });
    }
    return ret;
}

Key IndexDef::key() const
{
    auto key = Key(index_def()->to_tuple_descriptor());
    key["table_hash"] = parent()->key().hash();
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
    NonnullRefPtr<IndexDef> s_index_def = IndexDef::create("$index", true, 0).release_value_but_fixme_should_propagate_errors();
    if (!s_index_def->size()) {
        s_index_def->append_column("table_hash", SQLType::Integer, Order::Ascending);
        s_index_def->append_column("index_name", SQLType::Text, Order::Ascending);
        s_index_def->append_column("unique", SQLType::Integer, Order::Ascending);
    }
    return s_index_def;
}

ErrorOr<NonnullRefPtr<TableDef>> TableDef::create(SchemaDef* schema, ByteString name)
{
    return adopt_nonnull_ref_or_enomem(new (nothrow) TableDef(schema, move(name)));
}

TableDef::TableDef(SchemaDef* schema, ByteString name)
    : Relation(move(name), schema)
    , m_columns()
    , m_indexes()
{
}

NonnullRefPtr<TupleDescriptor> TableDef::to_tuple_descriptor() const
{
    NonnullRefPtr<TupleDescriptor> ret = adopt_ref(*new TupleDescriptor);
    for (auto& part : m_columns) {
        ret->append({ parent()->name(), name(), part->name(), part->type(), Order::Ascending });
    }
    return ret;
}

Key TableDef::key() const
{
    auto key = Key(index_def()->to_tuple_descriptor());
    key["schema_hash"] = parent()->key().hash();
    key["table_name"] = name();
    key.set_block_index(block_index());
    return key;
}

void TableDef::append_column(ByteString name, SQLType sql_type)
{
    auto column = ColumnDef::create(this, num_columns(), move(name), sql_type).release_value_but_fixme_should_propagate_errors();
    m_columns.append(column);
}

void TableDef::append_column(Key const& column)
{
    auto column_type = column["column_type"].to_int<UnderlyingType<SQLType>>();
    VERIFY(column_type.has_value());

    append_column(column["column_name"].to_byte_string(), static_cast<SQLType>(*column_type));
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
    NonnullRefPtr<IndexDef> s_index_def = IndexDef::create("$table", true, 0).release_value_but_fixme_should_propagate_errors();
    if (!s_index_def->size()) {
        s_index_def->append_column("schema_hash", SQLType::Integer, Order::Ascending);
        s_index_def->append_column("table_name", SQLType::Text, Order::Ascending);
    }
    return s_index_def;
}

}
