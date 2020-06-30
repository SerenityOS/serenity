/*
 * Copyright (c) 2020, The SerenityOS developers.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/Optional.h>
#include <AK/String.h>
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

    JsonPathElement(const StringView& key)
        : m_kind(Kind::Key)
        , m_key(key)
    {
    }

    Kind kind() const { return m_kind; }
    const String& key() const
    {
        ASSERT(m_kind == Kind::Key);
        return m_key;
    }

    size_t index() const
    {
        ASSERT(m_kind == Kind::Index);
        return m_index;
    }

    String to_string() const
    {
        switch (m_kind) {
        case Kind::Key:
            return key();
        case Kind::Index:
            return String::number(index());
        default:
            return "*";
        }
    }

    static JsonPathElement any_array_element;
    static JsonPathElement any_object_element;

    bool operator==(const JsonPathElement& other) const
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
    bool operator!=(const JsonPathElement& other) const
    {
        return !(*this == other);
    }

private:
    Kind m_kind;
    String m_key;
    size_t m_index { 0 };

    JsonPathElement(Kind kind)
        : m_kind(kind)
    {
    }
};

class JsonPath : public Vector<JsonPathElement> {
public:
    JsonValue resolve(const JsonValue&) const;
    String to_string() const;
};

}

using AK::JsonPath;
using AK::JsonPathElement;
