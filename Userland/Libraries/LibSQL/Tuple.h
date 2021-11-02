/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Debug.h>
#include <AK/Vector.h>
#include <LibSQL/Forward.h>
#include <LibSQL/TupleDescriptor.h>
#include <LibSQL/Value.h>

namespace SQL {

/**
 * A Tuple is an element of a random-access data structure persisted in a Heap.
 * Tuple objects stored in such a structure have a definition controlling the
 * number of parts or columns the tuple has, the types of the parts, and the
 * sort order of these parts. Besides having an optional definition, a Tuple
 * consists of one Value object per part. In addition, tuples have a u32 pointer
 * member which points to a Heap location.
 *
 * Tuple is a base class; concrete subclasses are Key, which implements the
 * elements of an index, and Row, which implements the rows in a table.
 */
class Tuple {
public:
    Tuple();
    explicit Tuple(NonnullRefPtr<TupleDescriptor> const&, u32 pointer = 0);
    Tuple(NonnullRefPtr<TupleDescriptor> const&, Serializer&);
    Tuple(Tuple const&);
    virtual ~Tuple() = default;

    Tuple& operator=(Tuple const&);

    [[nodiscard]] String to_string() const;
    explicit operator String() const { return to_string(); }
    [[nodiscard]] Vector<String> to_string_vector() const;

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
    void extend(Tuple const&);
    [[nodiscard]] bool is_compatible(Tuple const&) const;

    [[nodiscard]] u32 pointer() const { return m_pointer; }
    void set_pointer(u32 ptr) { m_pointer = ptr; }

    [[nodiscard]] size_t size() const { return m_data.size(); }
    [[nodiscard]] virtual size_t length() const;
    void clear() { m_data.clear(); }
    [[nodiscard]] NonnullRefPtr<TupleDescriptor> descriptor() const { return m_descriptor; }
    [[nodiscard]] int compare(Tuple const&) const;
    [[nodiscard]] int match(Tuple const&) const;
    [[nodiscard]] u32 hash() const;

protected:
    [[nodiscard]] Optional<size_t> index_of(String) const;
    void copy_from(Tuple const&);
    virtual void serialize(Serializer&) const;
    virtual void deserialize(Serializer&);

private:
    NonnullRefPtr<TupleDescriptor> m_descriptor;
    Vector<Value> m_data;
    u32 m_pointer { 2 * sizeof(u32) };

    friend Serializer;
};

}
