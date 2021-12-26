/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Debug.h>
#include <LibSQL/Forward.h>
#include <LibSQL/TupleDescriptor.h>
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
class Tuple {
public:
    Tuple();
    explicit Tuple(TupleDescriptor const&, u32 pointer = 0);
    Tuple(TupleDescriptor const&, ByteBuffer&, size_t&);
    Tuple(TupleDescriptor const&, ByteBuffer&);
    Tuple(Tuple const&);
    virtual ~Tuple() = default;

    Tuple& operator=(Tuple const&);

    [[nodiscard]] String to_string() const;
    explicit operator String() const { return to_string(); }

    bool operator<(Tuple const& other) const { return compare(other) < 0; }
    bool operator<=(Tuple const& other) const { return compare(other) <= 0; }
    bool operator==(Tuple const& other) const { return compare(other) == 0; }
    bool operator!=(Tuple const& other) const { return compare(other) != 0; }
    bool operator>(Tuple const& other) const { return compare(other) > 0; }
    bool operator>=(Tuple const& other) const { return compare(other) >= 0; }

    [[nodiscard]] bool is_null() const { return m_data.is_empty(); }
    [[nodiscard]] bool has(String const& name) const { return index_of(name).has_value(); }

    Value const& operator[](size_t ix) const;
    Value& operator[](size_t ix);
    Value const& operator[](String const& name) const;
    Value& operator[](String const& name);
    void append(Value const&);
    Tuple& operator+=(Value const&);
    [[nodiscard]] bool is_compatible(Tuple const&) const;

    [[nodiscard]] u32 pointer() const { return m_pointer; }
    void set_pointer(u32 ptr) { m_pointer = ptr; }

    [[nodiscard]] size_t size() const;
    [[nodiscard]] size_t length() const { return m_descriptor.size(); }
    [[nodiscard]] TupleDescriptor descriptor() const { return m_descriptor; }
    [[nodiscard]] int compare(Tuple const&) const;
    [[nodiscard]] int match(Tuple const&) const;
    [[nodiscard]] u32 hash() const;
    virtual void serialize(ByteBuffer&) const;
    [[nodiscard]] virtual size_t data_length() const { return descriptor().data_length(); }

protected:
    [[nodiscard]] Optional<size_t> index_of(String) const;
    void copy_from(Tuple const&);
    void deserialize(ByteBuffer&, size_t&);

private:
    TupleDescriptor m_descriptor;
    Vector<Value> m_data;
    u32 m_pointer { 0 };
};

}
