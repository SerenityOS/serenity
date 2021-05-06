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
#include <LibSQL/StorageForward.h>
#include <LibSQL/Type.h>

namespace SQL {

enum SortOrder {
    Ascending,
    Descending
};

class Heap;
class ColumnDef;
class KeyPartDef;
class IndexDef;
class TableDef;

class Relation : public Core::Object {
    C_OBJECT_ABSTRACT(Relation);
public:
    int hash() const { return m_hash; }
    u32 pointer() const { return m_pointer; }
    void set_pointer(u32 pointer) { m_pointer = pointer; }
    ~Relation() override = default;

protected:
    Relation(String name, u32 pointer, Relation *parent = nullptr)
        : Core::Object(parent)
        , m_pointer(pointer)
    {
        set_hash(name.hash());
        set_name(move(name));
    }

    explicit Relation(String name, Relation *parent = nullptr)
        : Core::Object(parent)
        , m_pointer(0)
    {
        set_hash(name.hash());
        set_name(move(name));
    }

    void set_hash(u32 hash) {
        m_hash = abs((int) hash);
    }

private:
    int m_hash { 0 };  // FIXME Have to use int so comparisons work; no unsigned Values yet.
    u32 m_pointer { 0 };
};

class ColumnDef : public Relation {
    C_OBJECT(ColumnDef);
public:
    Key key() const;
    SQLType type() const { return m_type; }
    size_t column_number() const { return m_index; }
    static NonnullRefPtr<IndexDef> index_def();
    static Key get_column_key(String);

protected:
    ColumnDef(Relation *, size_t, String, SQLType);

private:
    size_t m_index;
    SQLType m_type { SQLType::Text };
    static NonnullRefPtr<IndexDef> s_index_def;
};

class KeyPartDef : public ColumnDef {
    C_OBJECT(KeyPartDef);

public:
    KeyPartDef(IndexDef *, String, SQLType, SortOrder = Ascending);
    SortOrder sort_order() const { return m_sort_order; }

private:
    SortOrder m_sort_order { SortOrder::Ascending };
};


class IndexDef : public Relation {
    C_OBJECT(IndexDef);
public:
    ~IndexDef() override = default;

    NonnullRefPtrVector<KeyPartDef> key() const { return m_key; }
    bool unique() const { return m_unique; }
    size_t size() { return m_key.size(); }
    void append_column(String, SQLType, SortOrder = SortOrder::Ascending);
private:
    IndexDef(TableDef *, String, bool unique= true, u32 pointer = 0);
    explicit IndexDef(String, bool unique = true, u32 pointer = 0);

    NonnullRefPtrVector<KeyPartDef> m_key;
    bool m_unique { false };

    friend TableDef;
};

class TableDef : public Relation {
    C_OBJECT(TableDef);
public:
    Key key() const;
    void append_column(String, SQLType);
    size_t num_columns() { return m_columns.size(); }
    size_t num_indexes() { return m_indexes.size(); }
    NonnullRefPtrVector<ColumnDef> columns() const { return m_columns; }
    NonnullRefPtrVector<IndexDef> indexes() const { return m_indexes; }

    static NonnullRefPtr<IndexDef> index_def();
    static Key make_table_key(String);
private:
    explicit TableDef(String);
    explicit TableDef(Key const&);

    NonnullRefPtrVector<ColumnDef> m_columns;
    NonnullRefPtrVector<IndexDef> m_indexes;
    static NonnullRefPtr<IndexDef> s_index_def;
};

}
