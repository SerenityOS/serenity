/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullOwnPtr.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/NonnullRefPtr.h>
#include <AK/Result.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibCore/Object.h>
#include <LibSQL/Forward.h>
#include <LibSQL/Type.h>
#include <LibSQL/Value.h>

namespace SQL {

/**
 * This file declares objects describing tables, indexes, and columns.
 * It remains to be seen if this will survive in it's current form.
 */

class Relation : public Core::Object {
    C_OBJECT_ABSTRACT(Relation);

public:
    u32 hash() const;
    u32 pointer() const { return m_pointer; }
    void set_pointer(u32 pointer) { m_pointer = pointer; }
    ~Relation() override = default;
    virtual Key key() const = 0;
    Relation const* parent_relation() const { return dynamic_cast<Relation const*>(parent()); }

protected:
    Relation(String name, u32 pointer, Relation* parent = nullptr)
        : Core::Object(parent)
        , m_pointer(pointer)
    {
        set_name(move(name));
    }

    explicit Relation(String name, Relation* parent = nullptr)
        : Core::Object(parent)
        , m_pointer(0)
    {
        set_name(move(name));
    }

private:
    u32 m_pointer { 0 };
};

class SchemaDef : public Relation {
    C_OBJECT(SchemaDef);

public:
    Key key() const override;
    static NonnullRefPtr<IndexDef> index_def();
    static Key make_key();

private:
    explicit SchemaDef(String);
    explicit SchemaDef(Key const&);
};

class ColumnDef : public Relation {
    C_OBJECT(ColumnDef);

public:
    Key key() const override;
    SQLType type() const { return m_type; }
    size_t column_number() const { return m_index; }
    void set_not_null(bool can_not_be_null) { m_not_null = can_not_be_null; }
    bool not_null() const { return m_not_null; }
    void set_default_value(Value const& default_value);
    Value const& default_value() const { return m_default; }

    static NonnullRefPtr<IndexDef> index_def();
    static Key make_key(TableDef const&);

protected:
    ColumnDef(Relation*, size_t, String, SQLType);

private:
    size_t m_index;
    SQLType m_type { SQLType::Text };
    bool m_not_null { false };
    Value m_default;
};

class KeyPartDef : public ColumnDef {
    C_OBJECT(KeyPartDef);

public:
    Order sort_order() const { return m_sort_order; }

private:
    KeyPartDef(IndexDef*, String, SQLType, Order = Order::Ascending);

    Order m_sort_order { Order::Ascending };
};

class IndexDef : public Relation {
    C_OBJECT(IndexDef);

public:
    ~IndexDef() override = default;

    NonnullRefPtrVector<KeyPartDef> const& key_definition() const { return m_key_definition; }
    bool unique() const { return m_unique; }
    [[nodiscard]] size_t size() const { return m_key_definition.size(); }
    void append_column(String, SQLType, Order = Order::Ascending);
    Key key() const override;
    [[nodiscard]] NonnullRefPtr<TupleDescriptor> to_tuple_descriptor() const;
    static NonnullRefPtr<IndexDef> index_def();
    static Key make_key(TableDef const& table_def);

private:
    IndexDef(TableDef*, String, bool unique = true, u32 pointer = 0);
    explicit IndexDef(String, bool unique = true, u32 pointer = 0);

    NonnullRefPtrVector<KeyPartDef> m_key_definition;
    bool m_unique { false };

    friend TableDef;
};

class TableDef : public Relation {
    C_OBJECT(TableDef);

public:
    Key key() const override;
    void append_column(String, SQLType);
    void append_column(Key const&);
    size_t num_columns() { return m_columns.size(); }
    size_t num_indexes() { return m_indexes.size(); }
    NonnullRefPtrVector<ColumnDef> const& columns() const { return m_columns; }
    NonnullRefPtrVector<IndexDef> const& indexes() const { return m_indexes; }
    [[nodiscard]] NonnullRefPtr<TupleDescriptor> to_tuple_descriptor() const;

    static NonnullRefPtr<IndexDef> index_def();
    static Key make_key(SchemaDef const& schema_def);
    static Key make_key(Key const& schema_key);

private:
    explicit TableDef(SchemaDef*, String);

    NonnullRefPtrVector<ColumnDef> m_columns;
    NonnullRefPtrVector<IndexDef> m_indexes;
};

}
