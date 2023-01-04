/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Max Wipfli <mail@maxwipfli.ch>
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "JsonObject.h"

namespace AK {

JsonObject::JsonObject() = default;
JsonObject::~JsonObject() = default;

JsonObject::JsonObject(JsonObject const& other)
    : m_members(other.m_members)
{
}

JsonObject::JsonObject(JsonObject&& other)
    : m_members(move(other.m_members))
{
}

JsonObject& JsonObject::operator=(JsonObject const& other)
{
    if (this != &other)
        m_members = other.m_members;
    return *this;
}

JsonObject& JsonObject::operator=(JsonObject&& other)
{
    if (this != &other)
        m_members = move(other.m_members);
    return *this;
}

size_t JsonObject::size() const
{
    return m_members.size();
}

bool JsonObject::is_empty() const
{
    return m_members.is_empty();
}

JsonValue const& JsonObject::get_deprecated(StringView key) const
{
    auto const* value = get_ptr(key);
    static JsonValue* s_null_value { nullptr };
    if (!value) {
        if (!s_null_value)
            s_null_value = new JsonValue;
        return *s_null_value;
    }
    return *value;
}

JsonValue const* JsonObject::get_ptr(StringView key) const
{
    auto it = m_members.find(key);
    if (it == m_members.end())
        return nullptr;
    return &(*it).value;
}

bool JsonObject::has(StringView key) const
{
    return m_members.contains(key);
}

bool JsonObject::has_null(StringView key) const
{
    auto const* value = get_ptr(key);
    return value && value->is_null();
}

bool JsonObject::has_bool(StringView key) const
{
    auto const* value = get_ptr(key);
    return value && value->is_bool();
}

bool JsonObject::has_string(StringView key) const
{
    auto const* value = get_ptr(key);
    return value && value->is_string();
}

bool JsonObject::has_i32(StringView key) const
{
    auto const* value = get_ptr(key);
    return value && value->is_i32();
}

bool JsonObject::has_u32(StringView key) const
{
    auto const* value = get_ptr(key);
    return value && value->is_u32();
}

bool JsonObject::has_i64(StringView key) const
{
    auto const* value = get_ptr(key);
    return value && value->is_i64();
}

bool JsonObject::has_u64(StringView key) const
{
    auto const* value = get_ptr(key);
    return value && value->is_u64();
}

bool JsonObject::has_number(StringView key) const
{
    auto const* value = get_ptr(key);
    return value && value->is_number();
}

bool JsonObject::has_array(StringView key) const
{
    auto const* value = get_ptr(key);
    return value && value->is_array();
}

bool JsonObject::has_object(StringView key) const
{
    auto const* value = get_ptr(key);
    return value && value->is_object();
}

#ifndef KERNEL
bool JsonObject::has_double(StringView key) const
{
    auto const* value = get_ptr(key);
    return value && value->is_double();
}
#endif

void JsonObject::set(DeprecatedString const& key, JsonValue value)
{
    m_members.set(key, move(value));
}

bool JsonObject::remove(StringView key)
{
    return m_members.remove(key);
}

DeprecatedString JsonObject::to_deprecated_string() const
{
    return serialized<StringBuilder>();
}

}
