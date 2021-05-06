/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibSQL/Key.h>
#include <LibSQL/Meta.h>
#include <LibSQL/Serialize.h>
#include <LibSQL/Tuple.h>

namespace SQL {

Tuple::Tuple()
    : m_table()
    , m_values()
{
}

Tuple::Tuple(TableDef const& table)
    : m_table(table)
    , m_values()
{
    for (auto& column : table.columns()) {
        m_values.append(Value(column.type()));
    }
}

Tuple::Tuple(TableDef const& table, u32 pointer, ByteBuffer& buffer)
    : m_table(table)
    , m_values()
    , m_pointer(pointer)
{
    size_t offset = 0;
    for (auto& column : table.columns()) {
        m_values.append(Value(column.type(), buffer, offset));
    }
    deserialize_from<u32>(buffer, offset, m_next_pointer);
}

void Tuple::serialize(ByteBuffer& buffer) const
{
    for (auto &value : m_values) {
        value.serialize(buffer);
    }
    serialize_to<u32>(buffer, m_next_pointer);
}

Tuple::Tuple(Tuple const& other)
    : m_table()
    , m_values()
{
    copy_from(other);
}

Tuple& Tuple::operator = (Tuple const& other)
{
    if (this != &other) {
        copy_from(other);
    }
    return *this;
}

int Tuple::index_of(String name) const
{
    auto n = move(name);
    for (auto ix = 0u; ix < m_table->columns().size(); ix++) {
        auto & column = m_table->columns()[ix];
        if (column.name() == n) {
            return (int) ix;
        }
    }
    return -1;
}

bool Tuple::has(String name) const
{
    return index_of(move(name)) >= 0;
}

Value const& Tuple::operator [](size_t ix) const
{
    VERIFY(ix < m_values.size());
    return m_values[ix];
}

Value& Tuple::operator [](size_t ix)
{
    VERIFY(ix < m_values.size());
    return m_values[ix];
}

Value const& Tuple::operator [](String name) const
{
    return (*this)[(size_t) index_of(move(name))];
}

Value& Tuple::operator [](String name)
{
    return (*this)[(size_t) index_of(move(name))];
}

Tuple::operator String() const
{
    StringBuilder builder;
    builder.appendff("{} tuple [{}]: ", m_table->name(), m_pointer);
    for (auto & value : m_values) {
        if (!builder.is_empty()) {
            builder.append('|');
        }
        builder.append((String)value);
    }
    builder.appendff(" -> [{}]", m_next_pointer);
    return builder.build();
}

bool Tuple::operator==(const Tuple& other) const
{
    return pointer() == other.pointer();
}

bool Tuple::operator!=(const Tuple& other) const
{
    return pointer() != other.pointer();
}

size_t Tuple::size() const
{
    size_t sz = sizeof(u32);
    for (auto& value : m_values) {
        sz += value.size();
    }
    return sz;
}

void Tuple::copy_from(Tuple const& other)
{
    m_table = other.m_table;
    m_values.clear();
    for (auto &part : other.m_values) {
        m_values.append(part);
    }
    m_pointer = other.pointer();
    m_next_pointer = other.next_pointer();
}

bool Tuple::match(Key const& key) const
{
    auto key_index = 0u;
    for (auto& part : key.parts()) {
        auto my_index = index_of(part.name());
        if (my_index < 0) {
            return false;
        }
        auto key_value = key[key_index];
        if (key_value.is_null()) {
            return true;
        }
        if (m_values[my_index] != key_value) {
            return false;
        }
        key_index++;
    }
    return true;
}

}
