/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/NonnullRefPtr.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/String.h>
#include <LibSQL/StorageForward.h>
#include <LibSQL/Value.h>

namespace SQL {

/**
 * A Tuple is an element of a sequential-access persistence data structure
 * like a flat table. Like a key it has a definition for all its parts,
 * but unlike a key this definition is not optional.
 *
 * FIXME Tuples should logically belong to a TupleStore object, but right now
 * they stand by themselves; they contain a row's worth of data and a pointer
 * to the next Tuple.
 */
class Tuple {
public:
    Tuple();
    explicit Tuple(TableDef const&);
    Tuple(TableDef const&, u32, ByteBuffer&);
    Tuple(Tuple const&);
    ~Tuple() = default;

    Tuple& operator=(Tuple const&);
    String to_string() const;
    explicit operator String() const { return to_string(); }
    bool operator==(Tuple const&) const;
    bool operator!=(Tuple const& other) const { return !(*this == other); }

    [[nodiscard]] bool has(String name) const { return index_of(move(name)) >= 0; }

    Value const& operator[](size_t ix) const
    {
        VERIFY(ix < m_values.size());
        return m_values[ix];
    }

    Value& operator[](size_t ix)
    {
        VERIFY(ix < m_values.size());
        return m_values[ix];
    }

    Value const& operator[](String name) const { return (*this)[(size_t)index_of(move(name))]; }
    Value& operator[](String name) { return (*this)[(size_t)index_of(move(name))]; }

    [[nodiscard]] RefPtr<TableDef> table() const { return m_table; }
    [[nodiscard]] u32 pointer() const { return m_pointer; }
    void pointer(u32 ptr) { m_pointer = ptr; }
    [[nodiscard]] u32 next_pointer() const { return m_next_pointer; }
    void next_pointer(u32 ptr) { m_next_pointer = ptr; }

    [[nodiscard]] size_t size() const;
    [[nodiscard]] size_t length() const { return m_values.size(); }
    void serialize(ByteBuffer&) const;
    bool match(Key const&) const;

private:
    [[nodiscard]] int index_of(String) const;
    void copy_from(Tuple const&);

    RefPtr<TableDef> m_table;
    Vector<Value> m_values;
    u32 m_pointer { 0 };
    u32 m_next_pointer { 0 };
};

}
