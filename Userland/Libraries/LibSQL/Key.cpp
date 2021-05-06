/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <cstring>

#include <AK/String.h>
#include <AK/StringBuilder.h>

#include <LibSQL/BTree.h>
#include <LibSQL/Key.h>
#include <LibSQL/Meta.h>
#include <LibSQL/Serialize.h>
#include <LibSQL/Value.h>

namespace SQL {

Key::Key()
    : m_part_definitions()
    , m_key_data()
{
}

Key::Key(IndexDef const& index)
    : m_part_definitions()
    , m_key_data()
{
    for (auto& part : index.key()) {
        m_part_definitions.append(part);
        m_key_data.append(Value(part.type()));
    }
}

Key::Key(IndexDef const& index, ByteBuffer& buffer, size_t& offset)
    : m_part_definitions()
    , m_key_data()
{
    for (auto& part : index.key()) {
        m_part_definitions.append(part);
    }
    dbgln_if(SERIALIZE_DEBUG, "deserialize key at offset {}", offset);
    deserialize_from<u32>(buffer, offset, m_pointer);
    dbgln_if(SERIALIZE_DEBUG, "pointer: {}", m_pointer);
    for (auto& part : m_part_definitions) {
        m_key_data.append(Value(part.type(), buffer, offset));
        dbgln_if(SERIALIZE_DEBUG, "Deserializing key column {} = {}", part.name(), (String) m_key_data.last());
    }
}

void Key::serialize(ByteBuffer& buffer) const
{
    VERIFY(m_part_definitions.size() == m_key_data.size());
    dbgln_if(SERIALIZE_DEBUG, "Serializing key pointer {}", pointer());
    serialize_to<u32>(buffer, pointer());
    for (auto ix = 0u; ix < m_part_definitions.size(); ix++) {
        auto& key_part = m_key_data[ix];
        if constexpr (SERIALIZE_DEBUG) {
            auto str_opt = key_part.to_string();
            auto& key_part_definition = m_part_definitions[ix];
            dbgln("Serializing key[{}] = {}", key_part_definition.name(), (str_opt.has_value()) ? str_opt.value() : "(null)");
        }
        key_part.serialize(buffer);
    }
}

Key::Key(Key const& other)
    : m_part_definitions()
    , m_key_data()
{
    copy_from(other);
}

Key & Key::operator = (Key const& other)
{
    if (this != &other) {
        copy_from(other);
    }
    return *this;
}

bool Key::operator < (Key const& other) const
{
    return compare(other) < 0;
}

bool Key::operator <= (Key const& other) const
{
    return compare(other) <= 0;
}

bool Key::operator == (Key const& other) const
{
    return compare(other) == 0;
}

bool Key::operator != (Key const& other) const
{
    return compare(other) != 0;
}

bool Key::operator > (Key const& other) const
{
    return compare(other) > 0;
}

bool Key::operator >= (Key const& other) const
{
    return compare(other) >= 0;
}

int Key::index_of(String name) const
{
    auto n = move(name);
    for (auto ix = 0u; ix < m_part_definitions.size(); ix++) {
        auto &part = m_part_definitions[ix];
        if (part.name() == n) {
            return (int) ix;
        }
    }
    return -1;
}

bool Key::is_null() const
{
    return m_part_definitions.is_empty();
}

bool Key::has(String name) const
{
    return index_of(move(name)) >= 0;
}

Value const& Key::operator [](size_t ix) const
{
    VERIFY(ix < m_key_data.size());
    return m_key_data[ix];
}

Value& Key::operator [](size_t ix)
{
    VERIFY(ix < m_key_data.size());
    return m_key_data[ix];
}

Value const& Key::operator [](String name) const
{
    return (*this)[(size_t) index_of(move(name))];
}

Value& Key::operator [](String name)
{
    return (*this)[(size_t) index_of(move(name))];
}

void Key::append(const Value& value)
{
    VERIFY(m_part_definitions.size() == 0);
    m_key_data.append(value);
}

Key& Key::operator +=(Value const& value)
{
    append(value);
    return *this;
}

bool Key::is_compatible(Key const& other) const
{
    if ((m_part_definitions.size() == 0) && (other.m_part_definitions.size() == 0)) {
        return true;
    }
    if (m_part_definitions.size() != other.m_part_definitions.size()) {
        return false;
    }
    for (auto ix = 0u; ix < m_part_definitions.size(); ix++) {
        auto& my_part = m_part_definitions[ix];
        auto& other_part = other.m_part_definitions[ix];
        if (my_part.type() != other_part.type()) {
            return false;
        }
        if (my_part.sort_order() != other_part.sort_order()) {
            return false;
        }
    }
    return true;
}

Key::operator String() const
{
    StringBuilder builder;
    for (auto &part : m_key_data) {
        if (!builder.is_empty()) {
            builder.append('|');
        }
        auto str_opt = part.to_string();
        builder.append((str_opt.has_value()) ? str_opt.value() : "(null)");
    }
    if (pointer() != 0) {
        builder.appendff(":{}", pointer());
    }
    return builder.build();
}

size_t Key::size() const
{
    size_t sz = sizeof(u32);
    for (auto &part : m_key_data) {
        sz += part.size();
    }
    return sz;
}

void Key::copy_from(const Key& other)
{
    m_part_definitions.clear();
    for (auto &part : other.m_part_definitions) {
        m_part_definitions.append(part);
    }
    m_key_data.clear();
    for (auto &part : other.m_key_data) {
        m_key_data.append(part);
    }
    m_pointer = other.pointer();
}

int Key::compare(const Key& other) const
{
    auto ret = 0;
    auto num_values = min(m_key_data.size(), other.m_key_data.size());
    VERIFY(num_values > 0);
    for (auto ix = 0u; ix < num_values; ix++) {
        ret = m_key_data[ix].compare(other.m_key_data[ix]);
        if (ret != 0) {
            return ((ix < m_part_definitions.size()) && m_part_definitions[ix].sort_order() == SortOrder::Descending) ? -ret : ret;
        }
    }
    return 0;
}

int Key::match(const Key& other) const
{
    auto ret = 0;
    for (auto ix = 0u; ix < m_key_data.size(); ix++) {
        if (other[ix].is_null()) {
            return 0;
        }
        ret = m_key_data[ix].compare(other[ix]);
        if (ret != 0) {
            return (m_part_definitions[ix].sort_order() == SortOrder::Descending) ? -ret : ret;
        }
    }
    return 0;
}

}
