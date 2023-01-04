/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Max Wipfli <mail@maxwipfli.ch>
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Concepts.h>
#include <AK/DeprecatedString.h>
#include <AK/Error.h>
#include <AK/HashMap.h>
#include <AK/JsonArray.h>
#include <AK/JsonObjectSerializer.h>
#include <AK/JsonValue.h>

namespace AK {

class JsonObject {
    template<typename Callback>
    using CallbackErrorType = decltype(declval<Callback>()(declval<DeprecatedString const&>(), declval<JsonValue const&>()).release_error());

public:
    JsonObject();
    ~JsonObject();

    JsonObject(JsonObject const& other);
    JsonObject(JsonObject&& other);

    JsonObject& operator=(JsonObject const& other);
    JsonObject& operator=(JsonObject&& other);

    [[nodiscard]] size_t size() const;
    [[nodiscard]] bool is_empty() const;

    [[nodiscard]] JsonValue const& get_deprecated(StringView key) const;

    [[nodiscard]] JsonValue const* get_ptr(StringView key) const;

    [[nodiscard]] bool has(StringView key) const;

    [[nodiscard]] bool has_null(StringView key) const;
    [[nodiscard]] bool has_bool(StringView key) const;
    [[nodiscard]] bool has_string(StringView key) const;
    [[nodiscard]] bool has_i32(StringView key) const;
    [[nodiscard]] bool has_u32(StringView key) const;
    [[nodiscard]] bool has_i64(StringView key) const;
    [[nodiscard]] bool has_u64(StringView key) const;
    [[nodiscard]] bool has_number(StringView key) const;
    [[nodiscard]] bool has_array(StringView key) const;
    [[nodiscard]] bool has_object(StringView key) const;
#ifndef KERNEL
    [[nodiscard]] bool has_double(StringView key) const;
#endif

    void set(DeprecatedString const& key, JsonValue value);

    template<typename Callback>
    void for_each_member(Callback callback) const
    {
        for (auto const& member : m_members)
            callback(member.key, member.value);
    }

    template<FallibleFunction<DeprecatedString const&, JsonValue const&> Callback>
    ErrorOr<void, CallbackErrorType<Callback>> try_for_each_member(Callback&& callback) const
    {
        for (auto const& member : m_members)
            TRY(callback(member.key, member.value));
        return {};
    }

    bool remove(StringView key);

    template<typename Builder>
    typename Builder::OutputType serialized() const;

    template<typename Builder>
    void serialize(Builder&) const;

    [[nodiscard]] DeprecatedString to_deprecated_string() const;

private:
    OrderedHashMap<DeprecatedString, JsonValue> m_members;
};

template<typename Builder>
inline void JsonObject::serialize(Builder& builder) const
{
    auto serializer = MUST(JsonObjectSerializer<>::try_create(builder));
    for_each_member([&](auto& key, auto& value) {
        MUST(serializer.add(key, value));
    });
    MUST(serializer.finish());
}

template<typename Builder>
inline typename Builder::OutputType JsonObject::serialized() const
{
    Builder builder;
    serialize(builder);
    return builder.build();
}

template<typename Builder>
inline void JsonValue::serialize(Builder& builder) const
{
    switch (m_type) {
    case Type::String: {
        builder.append('\"');
        builder.append_escaped_for_json({ m_value.as_string->characters(), m_value.as_string->length() });
        builder.append('\"');
    } break;
    case Type::Array:
        m_value.as_array->serialize(builder);
        break;
    case Type::Object:
        m_value.as_object->serialize(builder);
        break;
    case Type::Bool:
        builder.append(m_value.as_bool ? "true"sv : "false"sv);
        break;
#if !defined(KERNEL)
    case Type::Double:
        builder.appendff("{}", m_value.as_double);
        break;
#endif
    case Type::Int32:
        builder.appendff("{}", as_i32());
        break;
    case Type::Int64:
        builder.appendff("{}", as_i64());
        break;
    case Type::UnsignedInt32:
        builder.appendff("{}", as_u32());
        break;
    case Type::UnsignedInt64:
        builder.appendff("{}", as_u64());
        break;
    case Type::Null:
        builder.append("null"sv);
        break;
    default:
        VERIFY_NOT_REACHED();
    }
}

template<typename Builder>
inline typename Builder::OutputType JsonValue::serialized() const
{
    Builder builder;
    serialize(builder);
    return builder.build();
}

}

#if USING_AK_GLOBALLY
using AK::JsonObject;
#endif
