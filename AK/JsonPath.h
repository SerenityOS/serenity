/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/Types.h>
#include <AK/Vector.h>

namespace AK {

class JsonPathElement {
public:
    enum class Kind {
        Key,
        Index,
        AnyIndex,
        AnyKey,
    };

    JsonPathElement(size_t index)
        : m_kind(Kind::Index)
        , m_index(index)
    {
    }

    JsonPathElement(StringView key)
        : m_kind(Kind::Key)
        , m_key(key)
    {
    }

    Kind kind() const { return m_kind; }
    ByteString const& key() const
    {
        VERIFY(m_kind == Kind::Key);
        return m_key;
    }

    size_t index() const
    {
        VERIFY(m_kind == Kind::Index);
        return m_index;
    }

    ByteString to_byte_string() const
    {
        switch (m_kind) {
        case Kind::Key:
            return key();
        case Kind::Index:
            return ByteString::number(index());
        default:
            return "*";
        }
    }

    static JsonPathElement any_array_element;
    static JsonPathElement any_object_element;

    bool operator==(JsonPathElement const& other) const
    {
        switch (other.kind()) {
        case Kind::Key:
            return (m_kind == Kind::Key && other.key() == key()) || m_kind == Kind::AnyKey;
        case Kind::Index:
            return (m_kind == Kind::Index && other.index() == index()) || m_kind == Kind::AnyIndex;
        case Kind::AnyKey:
            return m_kind == Kind::Key;
        case Kind::AnyIndex:
            return m_kind == Kind::Index;
        }
        return false;
    }

private:
    Kind m_kind;
    ByteString m_key;
    size_t m_index { 0 };

    JsonPathElement(Kind kind)
        : m_kind(kind)
    {
    }
};

class JsonPath : public Vector<JsonPathElement> {
public:
    JsonValue resolve(JsonValue const& root) const { return MUST(try_resolve(root)); }
    ErrorOr<JsonValue> try_resolve(JsonValue const&) const;
    ByteString to_byte_string() const;
};

}

#if USING_AK_GLOBALLY
using AK::JsonPath;
using AK::JsonPathElement;
#endif
