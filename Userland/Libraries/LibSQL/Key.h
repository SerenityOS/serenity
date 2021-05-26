/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Meta.h"
#include <LibCore/Object.h>
#include <LibSQL/StorageForward.h>
#include <LibSQL/Value.h>

namespace SQL {

/**
 * A Key is an element of a random-access data structure persisted in a Heap.
 * Key objects stored in such a structure have a definition controlling the
 * number of parts or columns the key has, the types of the parts, and the
 * sort order of these parts. Besides having an optional definition, a Key
 * consists of one Value object per part. In addition, keys have a u32 pointer
 * member which points to a Heap location.
 *
 * Key objects without a definition can be used to locate/find objects in
 * a searchable data collection.
 *
 * FIXME Currently the Key definition is passed as an `IndexDefinition` meta
 * data object, meaning that names are associated with both the definition
 * and the parts of the key. These names are not used, meaning that key
 * definitions should probably be constructed in a different way.
 */
class Key {
public:
    Key();
    explicit Key(IndexDef const&);
    Key(IndexDef const&, ByteBuffer&, size_t&);
    Key(Key const&);
    ~Key() = default;

    Key& operator=(Key const&);

    [[nodiscard]] String to_string() const;
    explicit operator String() const { return to_string(); }

    bool operator<(Key const& other) const { return compare(other) < 0; }
    bool operator<=(Key const& other) const { return compare(other) <= 0; }
    bool operator==(Key const& other) const { return compare(other) == 0; }
    bool operator!=(Key const& other) const { return compare(other) != 0; }
    bool operator>(Key const& other) const { return compare(other) > 0; }
    bool operator>=(Key const& other) const { return compare(other) >= 0; }

    [[nodiscard]] bool is_null() const { return m_part_definitions.is_empty(); }
    [[nodiscard]] bool has(String name) const { return index_of(move(name)) >= 0; }

    Value const& operator[](size_t ix) const
    {
        VERIFY(ix < m_key_data.size());
        return m_key_data[ix];
    }

    Value& operator[](size_t ix)
    {
        VERIFY(ix < m_key_data.size());
        return m_key_data[ix];
    }

    Value const& operator[](String name) const { return (*this)[(size_t)index_of(move(name))]; }
    Value& operator[](String name) { return (*this)[(size_t)index_of(move(name))]; }
    void append(Value const&);
    Key& operator+=(Value const&);
    [[nodiscard]] bool is_compatible(Key const&) const;

    [[nodiscard]] u32 pointer() const { return m_pointer; }
    void pointer(u32 ptr) { m_pointer = ptr; }

    [[nodiscard]] size_t size() const;
    [[nodiscard]] size_t length() const { return m_part_definitions.size(); }
    [[nodiscard]] NonnullRefPtrVector<KeyPartDef> parts() const { return m_part_definitions; }
    void serialize(ByteBuffer&) const;
    [[nodiscard]] int compare(Key const&) const;
    [[nodiscard]] int match(Key const&) const;

private:
    NonnullRefPtrVector<KeyPartDef> m_part_definitions;
    Vector<Value> m_key_data;
    u32 m_pointer { 0 };

    [[nodiscard]] int index_of(String) const;
    void copy_from(Key const&);
};

}
