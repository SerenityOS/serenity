/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <cstring>

#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <LibSQL/Serializer.h>
#include <LibSQL/Tuple.h>
#include <LibSQL/TupleDescriptor.h>
#include <LibSQL/Value.h>

namespace SQL {

Tuple::Tuple()
    : m_descriptor(adopt_ref(*new TupleDescriptor))
    , m_data()
{
}

Tuple::Tuple(NonnullRefPtr<TupleDescriptor> const& descriptor, u32 pointer)
    : m_descriptor(descriptor)
    , m_data()
    , m_pointer(pointer)
{
    for (auto& element : *descriptor) {
        m_data.empend(element.type);
    }
}

Tuple::Tuple(NonnullRefPtr<TupleDescriptor> const& descriptor, Serializer& serializer)
    : Tuple(descriptor)
{
    deserialize(serializer);
}

void Tuple::deserialize(Serializer& serializer)
{
    dbgln_if(SQL_DEBUG, "deserialize tuple at offset {}", serializer.offset());
    serializer.deserialize_to<u32>(m_pointer);
    dbgln_if(SQL_DEBUG, "pointer: {}", m_pointer);
    auto sz = serializer.deserialize<u32>();
    m_data.clear();
    m_descriptor->clear();
    for (auto ix = 0u; ix < sz; ++ix) {
        m_descriptor->append(serializer.deserialize<TupleElementDescriptor>());
        m_data.append(serializer.deserialize<Value>());
    }
}

void Tuple::serialize(Serializer& serializer) const
{
    VERIFY(m_descriptor->size() == m_data.size());
    dbgln_if(SQL_DEBUG, "Serializing tuple pointer {}", pointer());
    serializer.serialize<u32>(pointer());
    serializer.serialize<u32>((u32)m_descriptor->size());
    for (auto ix = 0u; ix < m_descriptor->size(); ix++) {
        auto& key_part = m_data[ix];
        serializer.serialize<TupleElementDescriptor>((*m_descriptor)[ix]);
        serializer.serialize<Value>(key_part);
    }
}

Tuple::Tuple(Tuple const& other)
    : m_descriptor(other.m_descriptor)
    , m_data()
{
    copy_from(other);
}

Tuple& Tuple::operator=(Tuple const& other)
{
    if (this != &other) {
        copy_from(other);
    }
    return *this;
}

Optional<size_t> Tuple::index_of(String name) const
{
    auto n = move(name);
    for (auto ix = 0u; ix < m_descriptor->size(); ix++) {
        auto& part = (*m_descriptor)[ix];
        if (part.name == n) {
            return (int)ix;
        }
    }
    return {};
}

Value const& Tuple::operator[](size_t ix) const
{
    VERIFY(ix < m_data.size());
    return m_data[ix];
}

Value& Tuple::operator[](size_t ix)
{
    VERIFY(ix < m_data.size());
    return m_data[ix];
}

Value const& Tuple::operator[](String const& name) const
{
    auto index = index_of(name);
    VERIFY(index.has_value());
    return (*this)[index.value()];
}

Value& Tuple::operator[](String const& name)
{
    auto index = index_of(name);
    VERIFY(index.has_value());
    return (*this)[index.value()];
}

void Tuple::append(const Value& value)
{
    VERIFY(descriptor()->size() >= size());
    if (descriptor()->size() == size()) {
        descriptor()->append(value.descriptor());
    }
    m_data.append(value);
}

Tuple& Tuple::operator+=(Value const& value)
{
    append(value);
    return *this;
}

void Tuple::extend(Tuple const& other)
{
    VERIFY((descriptor()->size() == size()) || (descriptor()->size() >= size() + other.size()));
    if (descriptor()->size() == size()) {
        descriptor()->extend(other.descriptor());
    }
    m_data.extend(other.m_data);
}

bool Tuple::is_compatible(Tuple const& other) const
{
    if ((m_descriptor->size() == 0) && (other.m_descriptor->size() == 0)) {
        return true;
    }
    if (m_descriptor->size() != other.m_descriptor->size()) {
        return false;
    }
    for (auto ix = 0u; ix < m_descriptor->size(); ix++) {
        auto& my_part = (*m_descriptor)[ix];
        auto& other_part = (*other.m_descriptor)[ix];
        if (my_part.type != other_part.type) {
            return false;
        }
        if (my_part.order != other_part.order) {
            return false;
        }
    }
    return true;
}

size_t Tuple::length() const
{
    size_t len = 2 * sizeof(u32);
    for (auto ix = 0u; ix < m_descriptor->size(); ix++) {
        auto& descriptor = (*m_descriptor)[ix];
        auto& value = m_data[ix];
        len += descriptor.length();
        len += value.length();
    }
    return len;
}

String Tuple::to_string() const
{
    StringBuilder builder;
    for (auto& part : m_data) {
        if (!builder.is_empty()) {
            builder.append('|');
        }
        builder.append(part.to_string());
    }
    if (pointer() != 0) {
        builder.appendff(":{}", pointer());
    }
    return builder.build();
}

Vector<String> Tuple::to_string_vector() const
{
    Vector<String> ret;
    for (auto& value : m_data) {
        ret.append(value.to_string());
    }
    return ret;
}

void Tuple::copy_from(const Tuple& other)
{
    if (*m_descriptor != *other.m_descriptor) {
        m_descriptor->clear();
        for (TupleElementDescriptor const& part : *other.m_descriptor) {
            m_descriptor->append(part);
        }
    }
    m_data.clear();
    for (auto& part : other.m_data) {
        m_data.append(part);
    }
    m_pointer = other.pointer();
}

int Tuple::compare(const Tuple& other) const
{
    auto num_values = min(m_data.size(), other.m_data.size());
    VERIFY(num_values > 0);
    for (auto ix = 0u; ix < num_values; ix++) {
        auto ret = m_data[ix].compare(other.m_data[ix]);
        if (ret != 0) {
            if ((ix < m_descriptor->size()) && (*m_descriptor)[ix].order == Order::Descending)
                ret = -ret;
            return ret;
        }
    }
    return 0;
}

int Tuple::match(const Tuple& other) const
{
    auto other_index = 0u;
    for (auto& part : *other.descriptor()) {
        auto other_value = other[other_index];
        if (other_value.is_null())
            return 0;
        auto my_index = index_of(part.name);
        if (!my_index.has_value())
            return -1;
        auto ret = m_data[my_index.value()].compare(other_value);
        if (ret != 0)
            return ((*m_descriptor)[my_index.value()].order == Order::Descending) ? -ret : ret;
        other_index++;
    }
    return 0;
}

u32 Tuple::hash() const
{
    u32 ret = 0u;
    for (auto& value : m_data) {
        // This is an extension of the pair_int_hash function from AK/HashFunctions.h:
        if (!ret)
            ret = value.hash();
        else
            ret = int_hash((ret * 209) ^ (value.hash() * 413));
    }
    return ret;
}

}
